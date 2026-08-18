package com.example.delivery_project.service.component;

import com.example.delivery_project.spec.GeocodedLocation;
import com.example.delivery_project.spec.Location;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocationMapperTest {

    private final LocationMapper mapper = new LocationMapper();

    @Test
    void 지오코딩_결과를_도메인_위치로_변환한다() {
        Location result = mapper.toLocation(
                new GeocodedLocation(
                        "서울시청",
                        37.5663,
                        126.9779
                )
        );

        assertThat(result.address()).isEqualTo("서울시청");
        assertThat(result.latitude()).isEqualTo(37.5663);
        assertThat(result.longitude()).isEqualTo(126.9779);
    }
}
