package com.example.delivery_project.controller;

import com.example.delivery_project.dto.response.DriverSummaryResponse;
import com.example.delivery_project.service.DriverQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDriverControllerTest {

    @Mock
    private DriverQueryService driverQueryService;

    @InjectMocks
    private AdminDriverController controller;

    @Test
    void 관리자는_배송기사_목록을_조회한다() {
        DriverSummaryResponse driver =
                new DriverSummaryResponse(7L, "driver", "배송기사");
        when(driverQueryService.getDrivers()).thenReturn(List.of(driver));

        assertThat(controller.getDrivers()).containsExactly(driver);
        verify(driverQueryService).getDrivers();
    }
}
