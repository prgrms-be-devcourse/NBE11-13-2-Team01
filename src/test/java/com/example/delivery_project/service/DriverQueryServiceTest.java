package com.example.delivery_project.service;

import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.domain.repository.UserRepository;
import com.example.delivery_project.dto.response.DriverSummaryResponse;
import com.example.delivery_project.enums.Role;
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
class DriverQueryServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DriverQueryService service;

    @Test
    void 배송기사_역할의_사용자만_요약해_반환한다() {
        User driver = User.of(
                7L,
                "driver",
                "password",
                "배송기사",
                Role.ROLE_DELIVERY_DRIVER
        );
        when(userRepository.findAllByRoleOrderByNameAsc(
                Role.ROLE_DELIVERY_DRIVER
        )).thenReturn(List.of(driver));

        List<DriverSummaryResponse> responses = service.getDrivers();

        assertThat(responses).containsExactly(
                new DriverSummaryResponse(7L, "driver", "배송기사")
        );
        verify(userRepository).findAllByRoleOrderByNameAsc(
                Role.ROLE_DELIVERY_DRIVER
        );
    }
}
