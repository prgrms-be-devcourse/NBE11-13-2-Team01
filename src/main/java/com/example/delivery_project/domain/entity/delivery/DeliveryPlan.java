package com.example.delivery_project.domain.entity.delivery;

import com.example.delivery_project.domain.entity.delivery.spec.Location;
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
    private Double departureLatitude;
    private Double departureLongitude;

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
            Location location,
            LocalDateTime scheduledDepartureAt
    ) {
        DeliveryPlan plan = new DeliveryPlan();
        plan.driver = driver;
        plan.departureLocation = location.address();
        plan.departureLatitude = location.latitude();
        plan.departureLongitude = location.longitude();
        plan.scheduledDepartureAt = scheduledDepartureAt;
        plan.status = DeliveryPlanStatus.READY;
        return plan;
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

    public boolean areAllStopsCompleted() {
        return deliveryStops.stream()
                .allMatch(DeliveryStop::isCompleted);
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

    public void start() {
        if(!status.isReady()) {
            // TODO 커스텀 예외로 변경
            throw new IllegalStateException("Status is not ready");
        }
        this.status = DeliveryPlanStatus.DELIVERING;
        this.actualDepartureAt = LocalDateTime.now();
        this.deliveryStops.forEach(DeliveryStop::start);
    }

    public void completeStop(long stopId) {
        // TODO 커스텀 예외로 변경
        DeliveryStop now = deliveryStops.stream()
                .filter(stop -> stop.getId().equals(stopId))
                .findFirst()
                .orElseThrow(IllegalStateException::new);
        now.complete();
    }

    public void updateScheduledDepartureAt(LocalDateTime departureAt) {
        if(!status.isReady()) {
            //TODO 커스텀 예외로 변경
            throw new IllegalStateException();
        }
        this.scheduledDepartureAt = departureAt;
    }

    public void reorderStops(List<Long> stopIds) {
        if(!status.isReady()) {
            //TODO 커스텀 예외로 변경
            throw new IllegalStateException();
        }

    }

    public void finish() {
        if(!areAllStopsCompleted()) {
            // TODO 커스텀 예외로 변경
            throw new IllegalStateException();
        }
        this.status = DeliveryPlanStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public boolean isFinished() {
        return status.isCompleted();
    }
}