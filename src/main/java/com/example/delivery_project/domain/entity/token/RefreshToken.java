package com.example.delivery_project.domain.entity.token;

import com.example.delivery_project.domain.entity.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true
    )
    private User user;

    @Column(nullable = false, unique = true, length = 1000)
    private String token;

    public static RefreshToken of(
            User user,
            String token
    ) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.user = user;
        refreshToken.token = token;
        return refreshToken;
    }

    public void updateToken(String token) {
        this.token = token;
    }
}
