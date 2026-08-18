package com.example.delivery_project.service.component;

import com.example.delivery_project.dto.request.WeatherRequest;
import com.example.delivery_project.dto.response.WeatherResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WeatherProviderTest {

    private MockRestServiceServer server;
    private WeatherProvider weatherProvider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        weatherProvider = new WeatherProvider(builder.build());
        ReflectionTestUtils.setField(
                weatherProvider,
                "apiKey",
                " test-weather-key "
        );
    }

    @AfterEach
    void verifyRequest() {
        server.verify();
    }

    @Test
    void 초단기예보_API_응답을_WeatherResponse로_변환한다() {
        server.expect(request -> {
                    var uri = request.getURI();
                    var query = UriComponentsBuilder.fromUri(uri)
                            .build()
                            .getQueryParams();

                    assertThat(uri.getHost()).isEqualTo("apis.data.go.kr");
                    assertThat(uri.getPath()).contains("getUltraSrtFcst");
                    assertThat(query.getFirst("serviceKey"))
                            .isEqualTo("test-weather-key");
                    assertThat(query.getFirst("base_date"))
                            .isEqualTo("20260818");
                    assertThat(query.getFirst("base_time"))
                            .isEqualTo("1030");
                    assertThat(query.getFirst("nx")).isEqualTo("60");
                    assertThat(query.getFirst("ny")).isEqualTo("127");
                })
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        SUCCESS_RESPONSE,
                        MediaType.APPLICATION_JSON
                ));

        WeatherResponse response = weatherProvider.getWeather(request());

        assertThat(response.header().resultCode()).isEqualTo("00");
        assertThat(response.body().items().item()).hasSize(1);
        assertThat(response.body().items().item().getFirst().category())
                .isEqualTo("T1H");
    }

    @Test
    void 기상_API의_HTTP_오류를_호출자에게_전파한다() {
        server.expect(request -> assertThat(request.getURI().getHost())
                        .isEqualTo("apis.data.go.kr"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> weatherProvider.getWeather(request()))
                .isInstanceOf(RestClientResponseException.class);
    }

    private WeatherRequest request() {
        return WeatherRequest.builder()
                .baseDate("20260818")
                .baseTime("1030")
                .nx(60)
                .ny(127)
                .build();
    }

    private static final String SUCCESS_RESPONSE = """
            {
              "response": {
                "header": {
                  "resultCode": "00",
                  "resultMsg": "NORMAL_SERVICE"
                },
                "body": {
                  "dataType": "JSON",
                  "items": {
                    "item": [
                      {
                        "baseDate": "20260818",
                        "baseTime": "1030",
                        "category": "T1H",
                        "fcstDate": "20260818",
                        "fcstTime": "1100",
                        "fcstValue": "33",
                        "nx": 60,
                        "ny": 127
                      }
                    ]
                  },
                  "pageNo": 1,
                  "numOfRows": 10,
                  "totalCount": 1
                }
              }
            }
            """;
}
