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
import java.util.Collections;
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

    @OneToMany(
            mappedBy = "deliveryPlan",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderColumn(name = "sequence")
    @Getter(AccessLevel.NONE)
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

    @PrePersist
    private void prePersist() {
        if(this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public List<DeliveryStop> getDeliveryStops() {
        return Collections.unmodifiableList(this.deliveryStops);
    }

    static DeliveryPlan of(
            User driver,
            String departureLocation,
            LocalDateTime scheduledDepartureAt
    ) {
        DeliveryPlan plan = new DeliveryPlan();
        plan.driver = driver;
        plan.departureLocation = departureLocation;
        plan.scheduledDepartureAt = scheduledDepartureAt;
        plan.status = DeliveryPlanStatus.READY;
        return plan;
    }

    public DeliveryStop addStop(
            String address,
            Double latitude,
            Double longitude
    ) {
        DeliveryStop stop = DeliveryStop.of(
                this,
                address,
                latitude,
                longitude
        );

        deliveryStops.add(stop);
        return stop;
    }

    public void completeStop(long stopId) {
        //TODO 커스텀 예외로 변경
        DeliveryStop now = deliveryStops.stream()
                .filter(stop -> stop.getId().equals(stopId))
                .findFirst()
                .orElseThrow(IllegalStateException::new);
        now.complete();
    }

    public int getTotalStops() {
        return deliveryStops.size();
    }

    public long getRemainingStops() {
        return deliveryStops.stream()
                .filter(d -> !d.isCompleted())
                .count();
    }

    public long getDangerStops() {
        return deliveryStops.stream()
                .filter(d -> !d.isCompleted())
                .filter(DeliveryStop::isDangerStop)
                .count();
    }

    public boolean isCompleted() {
        return deliveryStops.stream()
                .allMatch(DeliveryStop::isCompleted);
    }

    public void finish() {
        if(!isCompleted()) {
            // TODO 커스텀 예외로 변경
            throw new IllegalStateException();
        }
        this.status = DeliveryPlanStatus.COMPLETED;
    }
}