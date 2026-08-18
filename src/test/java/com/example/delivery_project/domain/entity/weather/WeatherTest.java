package com.example.delivery_project.domain.entity.weather;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WeatherTest {

    @Test
    void 날씨를_생성하고_예보값을_갱신한다() {
        Weather weather = Weather.of(
                60,
                127,
                LocalDate.of(2026, 8, 18),
                LocalTime.of(11, 0),
                LocalDate.of(2026, 8, 18),
                LocalTime.of(10, 30),
                "T1H",
                "32"
        );
        LocalDateTime fetchedAt = LocalDateTime.now().plusMinutes(1);

        weather.updateFcstValue("33", fetchedAt);

        assertThat(weather.getFcstValue()).isEqualTo("33");
        assertThat(weather.getFetchedAt()).isEqualTo(fetchedAt);
    }

    @Test
    void 좌표와_카테고리는_필수다() {
        assertThatThrownBy(() -> Weather.of(
                null,
                127,
                LocalDate.now(),
                LocalTime.now(),
                LocalDate.now(),
                LocalTime.now(),
                "T1H",
                "30"
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Weather.of(
                60,
                127,
                LocalDate.now(),
                LocalTime.now(),
                LocalDate.now(),
                LocalTime.now(),
                " ",
                "30"
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
