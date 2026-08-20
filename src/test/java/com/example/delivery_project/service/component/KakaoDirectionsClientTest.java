package com.example.delivery_project.service.component;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.OptionalLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoDirectionsClientTest {

    private static final String REST_API_KEY = "test-rest-api-key";

    private MockRestServiceServer server;
    private KakaoDirectionsClient directionsClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        directionsClient = new KakaoDirectionsClient(
                builder.build(),
                REST_API_KEY
        );
    }

    @AfterEach
    void verifyRequest() {
        server.verify();
    }

    @Test
    @DisplayName("카카오 자동차 길찾기 응답에서 예상 소요시간을 반환한다")
    void findTravelDurationSeconds_returns_kakao_duration() {
        server.expect(request -> {
                    var uri = request.getURI();
                    var query = UriComponentsBuilder.fromUri(uri)
                            .build()
                            .getQueryParams();

                    assertThat(uri.getScheme()).isEqualTo("https");
                    assertThat(uri.getHost())
                            .isEqualTo("apis-navi.kakaomobility.com");
                    assertThat(uri.getPath()).isEqualTo("/v1/directions");
                    assertThat(UriUtils.decode(
                            query.getFirst("origin"),
                            StandardCharsets.UTF_8
                    ))
                            .isEqualTo("126.901,37.501");
                    assertThat(UriUtils.decode(
                            query.getFirst("destination"),
                            StandardCharsets.UTF_8
                    ))
                            .isEqualTo("126.904,37.504");
                    assertThat(query.getFirst("priority"))
                            .isEqualTo("RECOMMEND");
                    assertThat(query.getFirst("summary")).isEqualTo("true");
                })
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(
                        HttpHeaders.AUTHORIZATION,
                        "KakaoAK " + REST_API_KEY
                ))
                .andRespond(withSuccess(
                        SUCCESS_RESPONSE,
                        MediaType.APPLICATION_JSON
                ));

        OptionalLong duration =
                directionsClient.findTravelDurationSeconds(
                        37.501,
                        126.901,
                        37.504,
                        126.904
                );

        assertThat(duration).hasValue(1_125);
    }

    @Test
    @DisplayName("카카오가 유효한 경로를 찾지 못하면 빈 결과를 반환한다")
    void findTravelDurationSeconds_returns_empty_for_failed_route() {
        server.expect(request -> assertThat(request.getURI().getHost())
                        .isEqualTo("apis-navi.kakaomobility.com"))
                .andRespond(withSuccess(
                        FAILED_ROUTE_RESPONSE,
                        MediaType.APPLICATION_JSON
                ));

        OptionalLong duration =
                directionsClient.findTravelDurationSeconds(
                        37.501,
                        126.901,
                        37.504,
                        126.904
                );

        assertThat(duration).isEmpty();
    }

    private static final String SUCCESS_RESPONSE = """
            {
              "routes": [
                {
                  "result_code": 0,
                  "result_msg": "길찾기 성공",
                  "summary": {
                    "distance": 10234,
                    "duration": 1125
                  }
                }
              ]
            }
            """;

    private static final String FAILED_ROUTE_RESPONSE = """
            {
              "routes": [
                {
                  "result_code": 104,
                  "result_msg": "출발지와 도착지가 동일합니다"
                }
              ]
            }
            """;
}
