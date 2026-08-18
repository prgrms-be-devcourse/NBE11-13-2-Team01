package com.example.delivery_project.service.component;

import com.example.delivery_project.exception.global.BusinessException;
import com.example.delivery_project.spec.GeocodedLocation;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KakaoDirectionsLiveTest {

    private static final String ORIGIN_ADDRESS =
            "서울특별시 용산구 한강대로 405";
    private static final String DESTINATION_ADDRESS =
            "부산광역시 동구 중앙대로 206";

    @Test
    @DisplayName("실제 카카오 API로 주소를 좌표로 변환하고 자동차 경로를 탐색한다")
    @EnabledIfEnvironmentVariable(
            named = "KAKAO_LOCAL_API_KEY",
            matches = ".+"
    )
    void geocode_and_find_actual_driving_route() {
        String restApiKey = System.getenv("KAKAO_LOCAL_API_KEY");
        RestClient restClient = RestClient.builder().build();
        KakaoGeocodingClient geocodingClient =
                new KakaoGeocodingClient(restClient, restApiKey);

        GeocodedLocation origin = geocode(
                geocodingClient,
                ORIGIN_ADDRESS
        );
        GeocodedLocation destination = geocode(
                geocodingClient,
                DESTINATION_ADDRESS
        );

        KakaoDirectionsResponse response;

        try {
            response = restClient.get()
                    .uri(
                            "https://apis-navi.kakaomobility.com/v1/directions"
                                    + "?origin={origin}"
                                    + "&destination={destination}"
                                    + "&priority=RECOMMEND"
                                    + "&summary=true",
                            toCoordinate(origin),
                            toCoordinate(destination)
                    )
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "KakaoAK " + restApiKey.trim()
                    )
                    .retrieve()
                    .body(KakaoDirectionsResponse.class);
        } catch (RestClientResponseException e) {
            throw new AssertionError(
                    "실제 카카오 길찾기 API 호출 실패: status="
                            + e.getStatusCode()
                            + ", response="
                            + e.getResponseBodyAsString(),
                    e
            );
        }

        assertThat(response).isNotNull();
        assertThat(response.routes()).isNotEmpty();

        KakaoRoute route = response.routes().getFirst();

        assertThat(route.resultCode())
                .as(route.resultMessage())
                .isZero();
        assertThat(route.summary()).isNotNull();
        assertThat(route.summary().distance()).isPositive();
        assertThat(route.summary().duration()).isPositive();

        System.out.println("===== 카카오 자동차 길찾기 결과 =====");
        System.out.printf(
                "출발지: %s (latitude=%f, longitude=%f)%n",
                origin.address(),
                origin.latitude(),
                origin.longitude()
        );
        System.out.printf(
                "도착지: %s (latitude=%f, longitude=%f)%n",
                destination.address(),
                destination.latitude(),
                destination.longitude()
        );
        System.out.printf(
                "실제 도로 거리: %,d m (%.2f km)%n",
                route.summary().distance(),
                route.summary().distance() / 1_000.0
        );
        System.out.printf(
                "예상 소요 시간: %d분 %d초%n",
                route.summary().duration() / 60,
                route.summary().duration() % 60
        );
    }

    private GeocodedLocation geocode(
            KakaoGeocodingClient client,
            String address
    ) {
        try {
            return client.geocode(address);
        } catch (BusinessException e) {
            throw new AssertionError(
                    "실제 카카오 주소 API 호출 실패: address="
                            + address
                            + ", reason="
                            + e.getReason(),
                    e
            );
        }
    }

    private String toCoordinate(GeocodedLocation location) {
        return location.longitude() + "," + location.latitude();
    }

    private record KakaoDirectionsResponse(
            List<KakaoRoute> routes
    ) {
    }

    private record KakaoRoute(
            @JsonProperty("result_code")
            int resultCode,
            @JsonProperty("result_msg")
            String resultMessage,
            KakaoRouteSummary summary
    ) {
    }

    private record KakaoRouteSummary(
            int distance,
            int duration
    ) {
    }
}
