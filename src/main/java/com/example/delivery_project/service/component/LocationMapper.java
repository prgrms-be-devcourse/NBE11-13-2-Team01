package com.example.delivery_project.service.component;

import com.example.delivery_project.spec.GeocodedLocation;
import com.example.delivery_project.spec.Location;
import org.springframework.stereotype.Component;

@Component
public final class LocationMapper {
    public Location toLocation(GeocodedLocation result) {
        return new Location(
                result.address(),
                result.latitude(),
                result.longitude()
        );
    }
}
