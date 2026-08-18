package com.example.delivery_project.service.component;

import com.example.delivery_project.exception.ExceptionCode;
import com.example.delivery_project.exception.global.BusinessException;
import com.example.delivery_project.spec.GeocodedLocation;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Component
public class KakaoGeocodingClient implements GeocodingClient {

    private final RestClient restClient;
    private final String restApiKey;

    public KakaoGeocodingClient(
            RestClient restClient,
            @Value("${kakao.local.key}") String restApiKey
    ) {
        this.restClient = restClient;
        this.restApiKey = restApiKey;
    }

    @Override
    public GeocodedLocation geocode(String address) {
        if (address == null || address.isBlank()) {
            throw new BusinessException(
                    ExceptionCode.INVALID_INPUT,
                    "주소가 비어 있습니다."
            );
        }

        KakaoAddressResponse response;

        try {
            response = restClient.get()
                    .uri(
                            "https://dapi.kakao.com/v2/local/search/address.json"
                                    + "?query={query}&size=1",
                            address
                    )
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "KakaoAK " + restApiKey.trim()
                    )
                    .retrieve()
                    .body(KakaoAddressResponse.class);

        } catch (RestClientResponseException e) {
            throw new BusinessException(
                    ExceptionCode.UNEXPECTED_ERROR,
                    "카카오 주소 API 호출에 실패했습니다. status="
                            + e.getStatusCode()
                            + ", response="
                            + e.getResponseBodyAsString()
            );
        } catch (RestClientException e) {
            throw new BusinessException(
                    ExceptionCode.UNEXPECTED_ERROR,
                    "카카오 주소 API 호출에 실패했습니다. cause="
                            + e.getClass().getSimpleName()
                            + ": "
                            + e.getMessage()
            );
        }

        if (response == null
                || response.documents() == null
                || response.documents().isEmpty()) {
            throw new BusinessException(
                    ExceptionCode.INVALID_INPUT,
                    "검색되지 않는 주소입니다: " + address
            );
        }

        KakaoAddressDocument document =
                response.documents().getFirst();

        return new GeocodedLocation(
                document.addressName(),
                Double.parseDouble(document.y()),
                Double.parseDouble(document.x())
        );
    }

    private record KakaoAddressResponse(
            List<KakaoAddressDocument> documents
    ) {
    }

    private record KakaoAddressDocument(
            @JsonProperty("address_name")
            String addressName,
            String x,
            String y
    ) {
    }
}
