package com.example.delivery_project.domain.entity.user;

import com.example.delivery_project.enums.Role;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String loginId;

    @Column
    private String password;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public static User of(
            Long id,
            String loginId,
            String password,
            String name,
            Role role
    ) {
        User user =  new User();
        user.id = id;
        user.loginId = loginId;
        user.password = password;
        user.name = name;
        user.role = role;
        return user;
    }

    public static User of(
            String loginId,
            String password,
            String name,
            Role role
    ) {

        return User.of(
                null,
                loginId,
                password,
                name,
                role
        );
    }

    public static User of(
            String loginId,
            String password,
            String name
    ) {
        return User.of(
                null,
                loginId,
                password,
                name,
                Role.ROLE_DELIVERY_DRIVER
        );
    }
}
