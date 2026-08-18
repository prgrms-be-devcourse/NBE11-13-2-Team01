package com.example.delivery_project.service.component;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocationConverterTest {

    @Test
    void 서울시청_좌표를_기상청_격자로_변환한다() {
        LocationConverter.LatXLngY grid =
                LocationConverter.convertGridGps(
                        LocationConverter.TO_GRID,
                        37.5665,
                        126.9780
                );

        assertThat(grid.x).isEqualTo(60.0);
        assertThat(grid.y).isEqualTo(127.0);
    }

    @Test
    void 격자를_위경도로_역변환할_수_있다() {
        LocationConverter.LatXLngY gps =
                LocationConverter.convertGridGps(
                        LocationConverter.TO_GPS,
                        60,
                        127
                );

        assertThat(gps.lat).isCloseTo(37.5799, within(0.1));
        assertThat(gps.lng).isCloseTo(126.9893, within(0.1));
    }

    private org.assertj.core.data.Offset<Double> within(double value) {
        return org.assertj.core.data.Offset.offset(value);
    }
}
