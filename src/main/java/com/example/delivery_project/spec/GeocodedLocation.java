package com.example.delivery_project.spec;

public record GeocodedLocation(
        String address,
        Double latitude,
        Double longitude
) {
}
