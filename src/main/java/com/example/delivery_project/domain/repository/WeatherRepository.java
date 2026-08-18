package com.example.delivery_project.domain.repository;

import com.example.delivery_project.domain.entity.weather.Weather;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public interface WeatherRepository extends JpaRepository<Weather, Long> {

    // 특정 좌표/시각에서 지정한 category들만 조회 (예: T1H, RN1, PTY)
    List<Weather> findByNxAndNyAndFcstDateAndFcstTimeAndCategoryIn(
            Integer nx,
            Integer ny,
            LocalDate fcstDate,
            LocalTime fcstTime,
            List<String> categories
    );

    List<GridCoordinate> findDistinctBy();

    // db에 존재하는 nx, ny 좌표를 중복 없이 조회
    @Query("SELECT DISTINCT new com.example.delivery_project.domain.repository.GridCoordinate(w.nx, w.ny) FROM Weather w")
    List<GridCoordinate> findDistinctGridCoordinates();

    @Modifying
    @Query("""
        UPDATE Weather w
        SET w.fcstValue = :fcstValue, w.baseDate = :baseDate, w.baseTime = :baseTime, w.fetchedAt = :fetchedAt
        WHERE w.nx = :nx AND w.ny = :ny
          AND w.fcstDate = :fcstDate AND w.fcstTime = :fcstTime
          AND w.category = :category
        """)
    int updateFcstValue(
            @Param("nx") Integer nx,
            @Param("ny") Integer ny,
            @Param("fcstDate") LocalDate fcstDate,
            @Param("fcstTime") LocalTime fcstTime,
            @Param("baseDate") LocalDate baseDate,
            @Param("baseTime") LocalTime baseTime,
            @Param("category") String category,
            @Param("fcstValue") String fcstValue,
            @Param("fetchedAt") LocalDateTime fetchedAt
    );
}
