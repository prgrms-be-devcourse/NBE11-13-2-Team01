package com.example.delivery_project.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "기상청 초단기예보 데이터 응답")
public record WeatherResponse(Header header, Body body) {
    public record Header(String resultCode, String resultMsg){}
    public record Body(String dataType, Items items, Integer pageNo,
                       Integer numOfRows, Integer totalCount){}
    public record Items(List<Item> item){}
    public record Item(
            @Schema(description = "발표 날짜", example = "20260101")
            String baseDate,
            @Schema(description = "발표 시각", example = "1430")
            String baseTime,
            @Schema(description = "카테고리", example = "T1H")
            String category,
            @Schema(description = "예보 날짜", example = "20260101")
            String fcstDate,
            @Schema(description = "예보 시각", example = "1430")
            String fcstTime,
            @Schema(description = "카테고리 값. 각 카테고리마다 다른 종류의 값을 반환.", example = "강수없음")
            String fcstValue,
            @Schema(description = "예보 위치 nx 격자 좌표", example = "60")
            Integer nx,
            @Schema(description = "예보 위치 ny 격자 좌표", example = "127")
            Integer ny
    ){}
}
