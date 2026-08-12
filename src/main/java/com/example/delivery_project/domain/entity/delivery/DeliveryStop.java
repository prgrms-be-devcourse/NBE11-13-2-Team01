package com.example.delivery_project.domain.entity.delivery;

import com.example.delivery_project.domain.entity.enums.DeliveryStopStatus;
import com.example.delivery_project.domain.entity.enums.ProductType;
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
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private RiskAssessment riskAssessment;

    static DeliveryStop of(
            DeliveryPlan deliveryPlan,
            String address,
            Double latitude,
            Double longitude
    ) {
        DeliveryStop stop = new DeliveryStop();
        stop.deliveryPlan = deliveryPlan;
        stop.address = address;
        stop.latitude = latitude;
        stop.longitude = longitude;
        stop.status = DeliveryStopStatus.READY;
        return stop;
    }

    public List<DeliveryItem> getDeliveryItems() {
        return Collections.unmodifiableList(this.deliveryItems);
    }

    public DeliveryItem addItem(
            String productName,
            ProductType productType,
            Integer quantity
    ) {
        DeliveryItem item = DeliveryItem.of(
                this,
                productName,
                productType,
                quantity
        );

        deliveryItems.add(item);
        return item;
    }

    public void attachRiskAssessment(
            RiskAssessment riskAssessment
    ) {
        this.riskAssessment = riskAssessment;
    }

    void complete() {
       if(!status.isDelivering()) {
           //TODO 커스텀 예외로 변경
           throw new IllegalStateException("배송중이 아니라면 완료할 수 없습니다.");
       }
       this.status = DeliveryStopStatus.COMPLETED;
       this.completedAt = LocalDateTime.now();
    }

    boolean isCompleted() {
        return status.isCompleted();
    }

    boolean isDangerStop() {
        return riskAssessment.isDanger();
    }
}