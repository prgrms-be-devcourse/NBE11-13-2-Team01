package com.example.delivery_project.service;

import com.example.delivery_project.domain.repository.GridCoordinate;
import com.example.delivery_project.domain.repository.WeatherRepository;
import com.example.delivery_project.dto.request.WeatherRequest;
import com.example.delivery_project.service.component.WeatherUpdater;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WeatherService {

    private final WeatherUpdater weatherUpdater;
    private final WeatherRepository weatherRepository;

    @Transactional
    public void save(WeatherRequest request) {
        weatherUpdater.update(request);
    }

    //격자좌표를 중복되지 않게 List로 가져온다
    public List<GridCoordinate> getGridCoordinates(){
        return weatherRepository.findDistinctGridCoordinates();
    }

}
