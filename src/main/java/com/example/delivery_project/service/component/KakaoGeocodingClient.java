package com.example.delivery_project.service.component;

import com.example.delivery_project.spec.GeocodedLocation;
import org.springframework.stereotype.Component;

@Component
public class KakaoGeocodingClient implements GeocodingClient {
    @Override
    public GeocodedLocation geocode(String address) {
        return null;
    }
}
