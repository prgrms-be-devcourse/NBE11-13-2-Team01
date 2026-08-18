package com.example.delivery_project.dto.response;

import java.util.List;

public record WeatherResponse(Header header, Body body) {
    public record Header(String resultCode, String resultMsg){}
    public record Body(String dataType, Items items, Integer pageNo,
                       Integer numOfRows, Integer totalCount){}
    public record Items(List<Item> item){}
    public record Item(
            String baseDate,
            String baseTime,
            String category,
            String fcstDate,
            String fcstTime,
            String fcstValue,
            Integer nx,
            Integer ny
    ){}
}
