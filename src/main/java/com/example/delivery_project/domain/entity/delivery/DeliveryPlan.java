package com.example.delivery_project.domain.entity.delivery;

import com.example.delivery_project.domain.entity.enums.DeliveryPlanStatus;
import com.example.delivery_project.domain.entity.user.User;
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
public class DeliveryPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @OneToMany(mappedBy = "deliveryPlan")
    @OrderColumn(name = "sequence")
    private List<DeliveryStop> deliveryStops = new ArrayList<>();

    @Column(nullable = false)
    private String departureLocation;

    private LocalDateTime scheduledDepartureAt;

    private LocalDateTime actualDepartureAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryPlanStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    private DeliveryPlan(
            User driver,
            String departureLocation,
            LocalDateTime scheduledDepartureAt
    ) {
        this.driver = driver;
        this.departureLocation = departureLocation;
        this.scheduledDepartureAt = scheduledDepartureAt;
        this.status = DeliveryPlanStatus.READY;
    }

    public void start() {
        this.status = DeliveryPlanStatus.DELIVERING;
        this.actualDepartureAt = LocalDateTime.now();
        this.deliveryStops.forEach(DeliveryStop::start);
    }

    public void complete() {
        boolean allCompleted =
                !deliveryStops.isEmpty()
                        && deliveryStops.stream()
                        .allMatch(DeliveryStop::isCompleted);

        if (!allCompleted) {
            // TODO 프로젝트 예외로 변경
            throw new IllegalStateException("완료되지 않은 배송이 존재합니다.");
        }

        this.status = DeliveryPlanStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}