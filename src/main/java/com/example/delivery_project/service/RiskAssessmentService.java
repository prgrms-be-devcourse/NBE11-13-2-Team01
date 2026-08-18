package com.example.delivery_project.service;

import com.example.delivery_project.domain.entity.delivery.DeliveryStop;
import com.example.delivery_project.domain.entity.delivery.RiskAssessment;
import com.example.delivery_project.domain.entity.delivery.RiskFactor;
import com.example.delivery_project.domain.repository.DeliveryStopRepository;
import com.example.delivery_project.enums.RiskFactorType;
import com.example.delivery_project.enums.RiskLevel;
import com.example.delivery_project.domain.entity.weather.Weather;
import com.example.delivery_project.domain.repository.RiskAssessmentRepository;
import com.example.delivery_project.domain.repository.RiskFactorRepository;
import com.example.delivery_project.domain.repository.WeatherRepository;
import com.example.delivery_project.exception.DeliveryException;
import com.example.delivery_project.exception.global.BusinessException;
import com.example.delivery_project.service.component.LocationConverter;
import com.example.delivery_project.service.component.RiskFactorCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RiskAssessmentService {

    private static final List<String> RISK_CATEGORIES = List.of("T1H", "RN1", "PTY");

    private final WeatherRepository weatherRepository;
    private final RiskFactorCalculator riskFactorCalculator;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final RiskFactorRepository riskFactorRepository;
    private final DeliveryStopRepository deliveryStopRepository;

    @Transactional
    //List로 받아서 처리
    public void saveAssessments(List<DeliveryStop> deliveryStopList){
        for(DeliveryStop stop: deliveryStopList){
            try{
                saveRiskAssessment(stop);
            }catch (Exception e){
                log.error("위험도 평가 실패: stopId={}",stop.getId());
            }
        }
    }
    @Transactional
    //1. DeliveryStop을 인자로 받아 RiskAssessment 생성/저장
    public void saveRiskAssessment(DeliveryStop deliveryStop) {
        //deliveryStop에 대한 날씨 카테고리 key-value Map 생성
        Map<String, String> weatherValues = fetchWeatherValues(deliveryStop);

        List<RiskFactorType> types = determineTypes(weatherValues);

        int riskScore = calcScore(types);

        // 1. RiskAssessment 먼저 생성 후 저장 (id가 생겨야 RiskFactor의 FK로 쓸 수 있음)
        RiskAssessment assessment = deliveryStop.getRiskAssessment();
        riskAssessmentRepository.save(assessment);


        // 2. 이제 id가 생긴 assessment로 RiskFactor들 생성 후 저장
        for(RiskFactorType factorType: types){
            assessment.addFactor(factorType, factorType.getDescription());
        }
        riskFactorRepository.saveAll(assessment.getRiskFactors());

        // 3. 도메인 모델 일관성을 위해 연결 : db-메모리 같은 상태 유지를 위해
        deliveryStop.attachRiskAssessment(assessment);


    }

    @Transactional
    public void updateRiskAssessment(Long deliveryStopId){
        DeliveryStop deliveryStop = deliveryStopRepository.findById(deliveryStopId)
                .orElseThrow(()->new BusinessException(DeliveryException.DELIVERY_STOP_NOT_FOUND));

        //1. 날씨 데이터 다시 db에서 꺼내서 확인
        Map<String,String> weatherValues = fetchWeatherValues(deliveryStop);
        log.info("weatherValues isEmpty={}, size={}", weatherValues.isEmpty(), weatherValues.size());
        weatherValues.forEach((key, value) -> log.info("weatherValues[{}] = {}", key, value));
        // List<RiskFactorType> types = determineTypes(weatherValues);
        // weatherValues에 PTY 등이 없으면 NumberFormatException 발생 (RiskFactorCalculator.isRain null 처리 필요) + newTypes에서 안 씀

        RiskAssessment riskAssessment = deliveryStop.getRiskAssessment();

        List<RiskFactorType> newTypes = new ArrayList<>();
        newTypes.add(RiskFactorType.HEAT_WAVE);
        //2. RiskFactor 삭제 or 추가
        //새로운 날씨에 대한 새로운 List<RiskFactor>
        //3. RiskFactor 바탕으로 RiskAssessment 업데이트

        riskAssessment.updateFactors(newTypes); //analyzedAt이 최신화 안됨

    }
    public RiskLevel getRiskLevel(Long deliveryStopId){
        DeliveryStop deliveryStop = deliveryStopRepository.findById(deliveryStopId)
                .orElseThrow(()-> new BusinessException(DeliveryException.DELIVERY_STOP_NOT_FOUND));

        RiskAssessment assessment = deliveryStop.getRiskAssessment();
        return assessment.getLevel();
    }

    // DeliveryStop의 좌표 기준, 현재 날짜/시간에 해당하는 T1H, RN1, PTY 값을 조회
    private Map<String, String> fetchWeatherValues(DeliveryStop deliveryStop) {
        //좌표 전환
        LocationConverter.LatXLngY grid = LocationConverter.convertGridGps(
                LocationConverter.TO_GRID,
                deliveryStop.getLatitude(),
                deliveryStop.getLongitude()
        );
        int nx = (int) grid.x;
        int ny = (int) grid.y;

        LocalDate fcstDate = LocalDate.now();
        LocalTime fcstTime = LocalTime.now().truncatedTo(ChronoUnit.HOURS); //분,초는 버림

        List<Weather> weathers = weatherRepository.findByNxAndNyAndFcstDateAndFcstTimeAndCategoryIn(
                nx, ny, fcstDate, fcstTime, RISK_CATEGORIES
        );

        return weathers.stream()
                .collect(Collectors.toMap(Weather::getCategory, Weather::getFcstValue));
    }

    private int calcScore(List<RiskFactorType> types){
        return types.stream()
                .mapToInt(RiskFactorType::getRiskScore)
                .sum();
    }
    //1. 에러 처리 필요 : weatherValue에 null이 들어왔을 경우
    private List<RiskFactorType> determineTypes(Map<String,String> weatherValues){
        List<RiskFactorType> types = new ArrayList<>();
        //각 RiskFactorType에 해당하면 List에 추가만 진행. RiskFactor는 RiskAssessment 생성 후에 생성.
        if (riskFactorCalculator.isHeavyRain(weatherValues.get(RISK_CATEGORIES.get(1)), weatherValues.get(RISK_CATEGORIES.get(2)))) {
            types.add(RiskFactorType.HEAVY_RAIN);
        }
        if (riskFactorCalculator.isHeatWave(weatherValues.get(RISK_CATEGORIES.get(0)))) {
            types.add(RiskFactorType.HEAT_WAVE);
        }
        return types;
    }
}
