package com.example.delivery_project.controller;

import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.domain.repository.UserRepository;
import com.example.delivery_project.dto.response.TokenResponse;
import com.example.delivery_project.enums.Role;
import com.example.delivery_project.exception.ExceptionCode;
import com.example.delivery_project.exception.global.BusinessException;
import com.example.delivery_project.security.jwt.JwtProperties;
import com.example.delivery_project.security.jwt.TokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevTokenControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private DevTokenController controller;

    @Test
    void RefreshToken을_저장하지_않고_AccessToken만_발급한다() {
        User driver = User.of(
                1L,
                "driver1",
                "password",
                "배송기사1",
                Role.ROLE_DELIVERY_DRIVER
        );
        Duration validity = Duration.ofHours(2);
        when(userRepository.findByLoginId("driver1"))
                .thenReturn(Optional.of(driver));
        when(jwtProperties.getAccessTokenValidity())
                .thenReturn(validity);
        when(tokenProvider.generateToken(driver, validity))
                .thenReturn("access-token");

        TokenResponse response =
                controller.issueAccessToken("driver1");

        assertThat(response.accessToken()).isEqualTo("access-token");
        verify(tokenProvider).generateToken(driver, validity);
    }

    @Test
    void 존재하지_않는_아이디로는_개발용_토큰을_발급하지_않는다() {
        when(userRepository.findByLoginId("unknown"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> controller.issueAccessToken("unknown")
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ExceptionCode.INVALID_INPUT)
        );
    }
}
