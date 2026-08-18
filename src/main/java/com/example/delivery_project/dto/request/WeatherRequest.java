package com.example.delivery_project.dto.request;

import lombok.Builder;

@Builder
public record WeatherRequest(String baseTime,String baseDate,int nx, int ny) {
}
