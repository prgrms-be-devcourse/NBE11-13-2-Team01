package com.example.delivery_project.service.component;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WeatherProviderTest {

    @Test
    void weather_request_contains_api_key_and_returns_response_body() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        WeatherProvider provider = new WeatherProvider(builder.build());
        ReflectionTestUtils.setField(provider, "apiKey", "test-key");
        server.expect(requestTo(containsString("serviceKey=test-key")))
                .andExpect(requestTo(containsString("dataType=JSON")))
                .andRespond(withSuccess("{\"result\":\"ok\"}", MediaType.APPLICATION_JSON));

        String response = provider.getWeather();

        assertThat(response).isEqualTo("{\"result\":\"ok\"}");
        server.verify();
    }
}
