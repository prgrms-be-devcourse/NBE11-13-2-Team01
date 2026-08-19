package com.example.delivery_project.service;

import com.example.delivery_project.domain.repository.UserRepository;
import com.example.delivery_project.dto.response.DriverSummaryResponse;
import com.example.delivery_project.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DriverQueryService {

    private final UserRepository userRepository;

    public List<DriverSummaryResponse> getDrivers() {
        return userRepository
                .findAllByRoleOrderByNameAsc(Role.ROLE_DELIVERY_DRIVER)
                .stream()
                .map(DriverSummaryResponse::from)
                .toList();
    }
}
