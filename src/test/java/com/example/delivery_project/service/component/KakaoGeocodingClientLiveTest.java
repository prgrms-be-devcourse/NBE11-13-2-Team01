package com.example.delivery_project.service.component;

import com.example.delivery_project.exception.global.BusinessException;
import com.example.delivery_project.spec.GeocodedLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class KakaoGeocodingClientLiveTest {

    private static final String ADDRESS =
            "한강대로 405";

    @Test
    @DisplayName("실제 카카오 API로 주소를 위도와 경도로 변환한다")
    @EnabledIfEnvironmentVariable(
            named = "KAKAO_LOCAL_API_KEY",
            matches = ".+"
    )
    void geocode_calls_actual_kakao_api() {
        KakaoGeocodingClient client =
                new KakaoGeocodingClient(
                        RestClient.builder().build(),
                        System.getenv("KAKAO_LOCAL_API_KEY")
                );

        GeocodedLocation result;

        try {
            result = client.geocode(ADDRESS);
        } catch (BusinessException e) {
            throw new AssertionError(
                    "실제 카카오 API 호출 실패: " + e.getReason(),
                    e
            );
        }

        System.out.println("===== 카카오 주소 변환 결과 =====");
        System.out.println("입력 주소: " + ADDRESS);
        System.out.println("변환 주소: " + result.address());
        System.out.println("latitude: " + result.latitude());
        System.out.println("longitude: " + result.longitude());

        assertThat(result.address()).isNotBlank();
        assertThat(result.latitude())
                .isBetween(33.0, 39.0);
        assertThat(result.longitude())
                .isBetween(124.0, 132.0);
    }
}
