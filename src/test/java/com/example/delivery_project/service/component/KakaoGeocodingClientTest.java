package com.example.delivery_project.service.component;

import com.example.delivery_project.exception.ExceptionCode;
import com.example.delivery_project.exception.global.BusinessException;
import com.example.delivery_project.spec.GeocodedLocation;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoGeocodingClientTest {

    private static final String REST_API_KEY = "test-rest-api-key";
    private static final String SEARCH_ADDRESS = "전북 삼성동 100";

    private MockRestServiceServer server;
    private KakaoGeocodingClient geocodingClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        geocodingClient = new KakaoGeocodingClient(
                builder.build(),
                REST_API_KEY
        );
    }

    @AfterEach
    void verifyRequest() {
        server.verify();
    }

    @Test
    @DisplayName("카카오 주소 검색 응답을 주소와 위도·경도로 변환한다")
    void geocode_converts_kakao_address_response() {
        server.expect(request -> {
                    var uri = request.getURI();
                    var queryParams = UriComponentsBuilder
                            .fromUri(uri)
                            .build()
                            .getQueryParams();
                    String decodedAddress = UriUtils.decode(
                            queryParams.getFirst("query"),
                            StandardCharsets.UTF_8
                    );

                    assertThat(uri.getScheme()).isEqualTo("https");
                    assertThat(uri.getHost()).isEqualTo("dapi.kakao.com");
                    assertThat(uri.getPath())
                            .isEqualTo("/v2/local/search/address.json");
                    assertThat(decodedAddress)
                            .isEqualTo(SEARCH_ADDRESS);
                    assertThat(queryParams.getFirst("size"))
                            .isEqualTo("1");
                })
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(
                        HttpHeaders.AUTHORIZATION,
                        "KakaoAK " + REST_API_KEY
                ))
                .andRespond(withSuccess(
                        KAKAO_ADDRESS_RESPONSE,
                        MediaType.APPLICATION_JSON
                ));

        GeocodedLocation result =
                geocodingClient.geocode(SEARCH_ADDRESS);

        assertThat(result.address())
                .isEqualTo("전북 익산시 부송동 100");
        assertThat(result.latitude())
                .isEqualTo(35.97664845766847);
        assertThat(result.longitude())
                .isEqualTo(126.99597295767953);
    }

    @Test
    @DisplayName("빈 주소는 카카오 API를 호출하지 않고 거부한다")
    void geocode_rejects_blank_address() {
        assertThatThrownBy(() -> geocodingClient.geocode(" "))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ExceptionCode.INVALID_INPUT)
                );
    }

    @Test
    @DisplayName("검색 결과가 없는 주소는 입력 오류로 처리한다")
    void geocode_rejects_address_without_result() {
        server.expect(request -> assertThat(request.getURI().getHost())
                        .isEqualTo("dapi.kakao.com"))
                .andRespond(withSuccess(
                        "{\"documents\":[]}",
                        MediaType.APPLICATION_JSON
                ));

        assertThatThrownBy(() -> geocodingClient.geocode("없는 주소"))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ExceptionCode.INVALID_INPUT)
                );
    }

    private static final String KAKAO_ADDRESS_RESPONSE = """
            {
              "meta": {
                "total_count": 4,
                "pageable_count": 4,
                "is_end": true
              },
              "documents": [
                {
                  "address_name": "전북 익산시 부송동 100",
                  "y": "35.97664845766847",
                  "x": "126.99597295767953",
                  "address_type": "REGION_ADDR",
                  "address": {
                    "address_name": "전북 익산시 부송동 100",
                    "region_1depth_name": "전북",
                    "region_2depth_name": "익산시",
                    "region_3depth_name": "부송동",
                    "region_3depth_h_name": "삼성동",
                    "h_code": "4514069000",
                    "b_code": "4514013400",
                    "mountain_yn": "N",
                    "main_address_no": "100",
                    "sub_address_no": "",
                    "x": "126.99597295767953",
                    "y": "35.97664845766847"
                  },
                  "road_address": {
                    "address_name": "전북 익산시 망산길 11-17",
                    "region_1depth_name": "전북",
                    "region_2depth_name": "익산시",
                    "region_3depth_name": "부송동",
                    "road_name": "망산길",
                    "underground_yn": "N",
                    "main_building_no": "11",
                    "sub_building_no": "17",
                    "building_name": "",
                    "zone_no": "54547",
                    "y": "35.976749396987046",
                    "x": "126.99599512792346"
                  }
                }
              ]
            }
            """;
}
