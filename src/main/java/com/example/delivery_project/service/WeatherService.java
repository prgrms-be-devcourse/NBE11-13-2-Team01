package com.example.delivery_project.service;

import com.example.delivery_project.dto.request.WeatherRequest;
import com.example.delivery_project.service.component.WeatherUpdater;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WeatherService {

    private final WeatherUpdater weatherUpdater;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean save(WeatherRequest request) {
        return weatherUpdater.update(request);
    }

    public WeatherUpdater.BaseDateTime resolveLatestBaseDateTime() {
        return weatherUpdater.resolveLatestBaseDateTime();
    }
}
