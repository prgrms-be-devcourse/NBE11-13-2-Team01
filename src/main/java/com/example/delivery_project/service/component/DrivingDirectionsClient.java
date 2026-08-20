package com.example.delivery_project.service.component;

import java.util.OptionalLong;

public interface DrivingDirectionsClient {

    OptionalLong findTravelDurationSeconds(
            double originLatitude,
            double originLongitude,
            double destinationLatitude,
            double destinationLongitude
    );
}
