package com.example.delivery_project.domain.entity.delivery;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RouteSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_plan_id", nullable = false)
    private DeliveryPlan deliveryPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_stop_id")
    private DeliveryStop fromStop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_stop_id", nullable = false)
    private DeliveryStop toStop;

    @Column(nullable = false)
    private Integer sequence;

    private Integer distanceMeters;

    private Integer estimatedDurationMinutes;
}
