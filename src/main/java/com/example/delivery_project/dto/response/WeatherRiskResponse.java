package com.example.delivery_project.dto.response;

import java.util.List;

public record WeatherRiskResponse(
        List<WeatherRiskFactorResponse> factors
) {
}