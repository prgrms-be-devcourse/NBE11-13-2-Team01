package com.example.delivery_project.domain.repository;

import com.example.delivery_project.domain.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

}
