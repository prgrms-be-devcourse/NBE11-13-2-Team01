package com.example.delivery_project.controller;

import com.example.delivery_project.config.OpenApiConfig;
import com.example.delivery_project.dto.response.DriverSummaryResponse;
import com.example.delivery_project.service.DriverQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/drivers")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "관리자 배송 기사", description = "배송 계획 할당을 위한 배송 기사 조회 API")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AdminDriverController {

    private final DriverQueryService driverQueryService;

    @Operation(summary = "배송 기사 목록 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "배송 기사 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요")
    })
    @GetMapping
    public List<DriverSummaryResponse> getDrivers() {
        return driverQueryService.getDrivers();
    }
}
