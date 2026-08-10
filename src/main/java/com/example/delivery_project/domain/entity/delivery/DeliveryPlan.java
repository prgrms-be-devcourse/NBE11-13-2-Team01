package com.example.delivery_project.domain.entity.delivery;

import com.example.delivery_project.domain.entity.enums.DeliveryStatus;
import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.domain.entity.vehicle.Vehicle;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(nullable = false)
    private String departureLocation;

    private LocalDateTime scheduledDepartureAt;

    private LocalDateTime actualDepartureAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    private Integer overallRiskScore;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

}
