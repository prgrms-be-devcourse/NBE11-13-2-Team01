package com.example.delivery_project.domain.entity.weather;

import com.example.delivery_project.exception.RiskException;
import com.example.delivery_project.exception.global.BusinessException;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
        name = "weather",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_weather_slot",
                columnNames = {"nx", "ny", "fcstDate", "fcstTime", "category"}
        )
)
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Weather {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer nx;

    @Column(nullable = false)
    private Integer ny;

    @Column(nullable = false)
    private LocalDate fcstDate;

    @Column(nullable = false)
    private LocalTime fcstTime;

    @Column(nullable = false)
    private LocalDate baseDate;

    @Column(nullable = false)
    private LocalTime baseTime;

    @Column(nullable = false, length = 10)
    private String category;

    @Column(nullable = false, length = 50)
    private String fcstValue;

    @Column(nullable = false)
    private LocalDateTime fetchedAt;

    public static Weather of(
            Integer nx,
            Integer ny,
            LocalDate fcstDate,
            LocalTime fcstTime,
            LocalDate baseDate,
            LocalTime baseTime,
            String category,
            String fcstValue
    ) {
        validateLocation(nx, ny);
        validateCategory(category);

        Weather weather = new Weather();
        weather.nx = nx;
        weather.ny = ny;
        weather.fcstDate = fcstDate;
        weather.fcstTime = fcstTime;
        weather.baseDate = baseDate;
        weather.baseTime = baseTime;
        weather.category = category;
        weather.fcstValue = fcstValue;
        weather.fetchedAt = LocalDateTime.now();
        return weather;
    }

    //update시, fcstValue,fetchedAt만 update
    public void updateFcstValue(String fcstValue, LocalDateTime fetchedAt) {
        this.fcstValue = fcstValue;
        this.fetchedAt = fetchedAt;
    }

    //격좌 좌표 검증
    private static void validateLocation(Integer nx, Integer ny) {
        if (nx == null || ny == null) {
            throw new BusinessException(RiskException.RISK_ARGUMENT_NOT_IMPLEMENTED,"격자 좌표(nx, ny)는 필수입니다.");
        }
    }

    //카테고리 검증
    private static void validateCategory(String category) {
        if (category == null || category.isBlank()) {
            throw new BusinessException(RiskException.RISK_ARGUMENT_NOT_IMPLEMENTED,"카테고리는 필수입니다.");
        }
    }
}
