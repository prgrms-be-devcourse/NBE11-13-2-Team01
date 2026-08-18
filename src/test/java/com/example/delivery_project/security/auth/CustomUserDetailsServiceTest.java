package com.example.delivery_project.security.auth;

import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.domain.repository.UserRepository;
import com.example.delivery_project.enums.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void 로그인_아이디로_사용자_정보를_조회한다() {
        User user = User.of(
                1L,
                "driver",
                "password",
                "배송기사",
                Role.ROLE_DELIVERY_DRIVER
        );
        when(userRepository.findByLoginId("driver"))
                .thenReturn(Optional.of(user));

        CustomUserDetails result =
                userDetailsService.loadUserByUsername("driver");

        assertThat(result.getUser()).isEqualTo(user);
    }

    @Test
    void 사용자가_없으면_UsernameNotFoundException이_발생한다() {
        when(userRepository.findByLoginId("missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> userDetailsService.loadUserByUsername("missing")
        ).isInstanceOf(UsernameNotFoundException.class);
    }
}
