package com.example.delivery_project.service.component;

import com.example.delivery_project.spec.GeocodedLocation;

public interface GeocodingClient {
    GeocodedLocation geocode(String address);
}
