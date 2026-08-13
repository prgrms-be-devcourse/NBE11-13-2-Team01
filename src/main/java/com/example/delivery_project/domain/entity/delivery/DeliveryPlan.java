package com.example.delivery_project.domain.entity.delivery;

import com.example.delivery_project.domain.entity.delivery.spec.Location;
import com.example.delivery_project.domain.entity.enums.DeliveryPlanStatus;
import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.exception.DeliveryException;
import com.example.delivery_project.exception.ErrorCode;
import com.example.delivery_project.exception.global.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    public DeliveryStop addStop(
            String address,
            Double latitude,
            Double longitude,
            LocalDateTime analyzedAt
    ) {
        if(!status.isReady()) {
            throwException(
                    DeliveryException.DELIVERY_INVALID_PLAN_STATUS_CHANGE
            );
        }
        DeliveryStop deliveryStop = DeliveryStop.of(
                this,
                address,
                latitude,
                longitude,
                analyzedAt
        );
        this.deliveryStops.add(deliveryStop);
        return deliveryStop;
    }

    public DeliveryStop addStop(
            Location location,
            LocalDateTime analyzedAt
    ) {
        return this.addStop(
                location.address(),
                location.latitude(),
                location.longitude(),
                analyzedAt
        );
    }

    public List<DeliveryStop> getDeliveryStops() {
        return Collections.unmodifiableList(this.deliveryStops);
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
        return !deliveryStops.isEmpty()
                && deliveryStops.stream()
                .allMatch(DeliveryStop::isCompleted);
    }

    public void start() {
        if(!status.isReady()) {
            throwException(DeliveryException.DELIVERY_PLAN_NOT_READY_TO_START);
        }
        this.status = DeliveryPlanStatus.DELIVERING;
        this.actualDepartureAt = LocalDateTime.now();
        this.deliveryStops.forEach(DeliveryStop::start);
    }

    public void completeStop(long stopId) {
        DeliveryStop now = deliveryStops.stream()
                .filter(stop -> stop.getId().equals(stopId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(DeliveryException.DELIVERY_STOP_NOT_FOUND));
        now.complete();
    }

    public void updateScheduledDepartureAt(LocalDateTime departureAt) {
        if(!status.isReady()) {
            throwException(DeliveryException.DELIVERY_INVALID_PLAN_STATUS_CHANGE);
        }
        this.scheduledDepartureAt = departureAt;
    }

    public void reorderStops(List<Long> stopIds) {
        if(!status.isReady()) {
            throwException(
                    DeliveryException.DELIVERY_INVALID_PLAN_STATUS_CHANGE
            );
        }

        final Map<Long, DeliveryStop> stopMap = deliveryStops.stream()
                .collect(Collectors.toMap(
                        DeliveryStop::getId,
                        Function.identity()
                ));
        final List<DeliveryStop> reorderedStops = new ArrayList<>();

        for(Long stopId : stopIds) {
            DeliveryStop stop = stopMap.remove(stopId);
            if(stop == null) {
                throwException(
                        DeliveryException.DELIVERY_STOP_NOT_FOUND
                );
            }
            reorderedStops.add(stop);
        }

        if(!stopMap.isEmpty()) {
            throwException(
                    DeliveryException.DELIVERY_INVALID_PLAN_STATUS_CHANGE
            );
        }
        this.deliveryStops.clear();
        this.deliveryStops.addAll(reorderedStops);
    }

    public void finish() {
        if(!status.isDelivering()) {
            throwException(
                    DeliveryException.DELIVERY_INVALID_PLAN_STATUS_CHANGE
            );
        }
        if(!areAllStopsCompleted()) {
            throwException(
                    DeliveryException.DELIVERY_INCOMPLETE_STOP
            );
        }
        this.status = DeliveryPlanStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public boolean isFinished() {
        return status.isCompleted();
    }

    private void throwException(ErrorCode code) {
        throw new BusinessException(code);
    }
}