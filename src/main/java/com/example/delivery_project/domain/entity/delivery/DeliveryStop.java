package com.example.delivery_project.domain.entity.delivery;

import com.example.delivery_project.domain.entity.enums.DeliveryStopStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_plan_id", nullable = false)
    private DeliveryPlan deliveryPlan;

    @OneToMany(mappedBy = "deliveryStop")
    private List<DeliveryItem> deliveryItems = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStopStatus status;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    private LocalDateTime scheduledArrivalAt;

    private LocalDateTime completedAt;

    public boolean isCompleted() {
        return status == DeliveryStopStatus.COMPLETED;
    }

    public void start() {
        this.status = DeliveryStopStatus.DELIVERING;
    }

    public void complete() {
        this.status = DeliveryStopStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
}