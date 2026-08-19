package com.example.delivery_project.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "날씨 위험 요인 목록 응답")
public record WeatherRiskResponse(
        List<WeatherRiskFactorResponse> factors
) {
}