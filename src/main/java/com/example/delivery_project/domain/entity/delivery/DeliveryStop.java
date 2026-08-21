package com.example.delivery_project.domain.entity.delivery;

import com.example.delivery_project.enums.DeliveryStopStatus;
import com.example.delivery_project.enums.ProductType;
import com.example.delivery_project.enums.RiskFactorType;
import com.example.delivery_project.exception.DeliveryException;
import com.example.delivery_project.exception.global.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    @OneToMany(
            mappedBy = "deliveryStop",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Getter(AccessLevel.NONE)
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

    private LocalDateTime completedAt;

    @OneToOne(
            mappedBy = "deliveryStop",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private RiskAssessment riskAssessment;

    static DeliveryStop of(
            DeliveryPlan deliveryPlan,
            String address,
            Double latitude,
            Double longitude,
            LocalDateTime analyzedAt
    ) {
        DeliveryStop stop = new DeliveryStop();
        stop.deliveryPlan = deliveryPlan;
        stop.address = address;
        stop.latitude = latitude;
        stop.longitude = longitude;
        stop.status = DeliveryStopStatus.READY;

        stop.riskAssessment = RiskAssessment.of(
                stop,
                analyzedAt
        );
        return stop;
    }

    void addRiskFactor(
            RiskFactorType type,
            String description
    ) {
        riskAssessment.addFactor(
                type,
                description
        );
    }

    public DeliveryItem addItem(
            String productName,
            ProductType productType,
            Integer quantity
    ) {
        if(!status.isReady()) {
            throw new BusinessException(
                    DeliveryException.DELIVERY_INVALID_PLAN_STATUS_CHANGE
            );
        }
        DeliveryItem item = DeliveryItem.of(
                this,
                productName,
                productType,
                quantity
        );

        deliveryItems.add(item);
        return item;
    }

    boolean isCompleted() {
        return status.isCompleted();
    }

    boolean isDangerStop() {
        return riskAssessment.isDanger();
    }

    public List<DeliveryItem> getDeliveryItems() {
        return Collections.unmodifiableList(this.deliveryItems);
    }

    void complete() {
       if(!status.isDelivering()) {
           throw new BusinessException(DeliveryException.DELIVERY_INCOMPLETE_STOP);
       }
       this.status = DeliveryStopStatus.COMPLETED;
       this.completedAt = LocalDateTime.now();
    }

    void start() {
        if(!status.isReady()) {
            throw new BusinessException(DeliveryException.DELIVERY_PLAN_NOT_READY_TO_START);
        }
        this.status = DeliveryStopStatus.DELIVERING;
    }
    public void attachRiskAssessment(RiskAssessment riskAssessment){
        this.riskAssessment=riskAssessment;
    }

}
