package com.example.delivery_project.service.component;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.OptionalLong;

@Slf4j
@Component
public class KakaoDirectionsClient implements DrivingDirectionsClient {

    private final RestClient restClient;
    private final String restApiKey;

    public KakaoDirectionsClient(
            RestClient restClient,
            @Value("${kakao.local.key}") String restApiKey
    ) {
        this.restClient = restClient;
        this.restApiKey = restApiKey;
    }

    @Override
    public OptionalLong findTravelDurationSeconds(
            double originLatitude,
            double originLongitude,
            double destinationLatitude,
            double destinationLongitude
    ) {
        KakaoDirectionsResponse response;

        try {
            response = restClient.get()
                    .uri(
                            "https://apis-navi.kakaomobility.com/v1/directions"
                                    + "?origin={origin}"
                                    + "&destination={destination}"
                                    + "&priority=RECOMMEND"
                                    + "&summary=true",
                            toCoordinate(originLatitude, originLongitude),
                            toCoordinate(
                                    destinationLatitude,
                                    destinationLongitude
                            )
                    )
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "KakaoAK " + restApiKey.trim()
                    )
                    .retrieve()
                    .body(KakaoDirectionsResponse.class);
        } catch (RestClientResponseException e) {
            log.warn(
                    "카카오 길찾기 API 호출 실패: status={}",
                    e.getStatusCode()
            );
            return OptionalLong.empty();
        } catch (RestClientException e) {
            log.warn(
                    "카카오 길찾기 API 호출 실패: cause={}",
                    e.getClass().getSimpleName()
            );
            return OptionalLong.empty();
        }

        if (response == null
                || response.routes() == null
                || response.routes().isEmpty()) {
            log.warn("카카오 길찾기 API가 경로를 반환하지 않았습니다.");
            return OptionalLong.empty();
        }

        KakaoRoute route = response.routes().getFirst();
        if (route.resultCode() != 0
                || route.summary() == null
                || route.summary().duration() < 0) {
            log.warn(
                    "카카오 길찾기 경로 탐색 실패: code={}, message={}",
                    route.resultCode(),
                    route.resultMessage()
            );
            return OptionalLong.empty();
        }

        return OptionalLong.of(route.summary().duration());
    }

    private String toCoordinate(double latitude, double longitude) {
        return longitude + "," + latitude;
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
            long duration
    ) {
    }
}
