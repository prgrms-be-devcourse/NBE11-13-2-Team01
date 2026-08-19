package com.example.delivery_project.domain.repository;

import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByLoginId(String loginId);

    Optional<User> findByLoginId(String loginId);

    List<User> findAllByRoleOrderByNameAsc(Role role);
}
