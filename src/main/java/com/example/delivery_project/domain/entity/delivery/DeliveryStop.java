package com.example.delivery_project.domain.entity.delivery;

import com.example.delivery_project.domain.entity.enums.DeliveryStopStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_plan_id", nullable = false)
    private DeliveryPlan deliveryPlan;

    @Column(nullable = false)
    private Integer sequence;

    @Column(nullable = false)
    private String address;

    private Double latitude;
    private Double longitude;

    private LocalDateTime scheduledArrivalAt;
    private LocalDateTime actualArrivalAt;

    @Enumerated(EnumType.STRING)
    private DeliveryStopStatus status;
}

