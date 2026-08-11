package com.example.delivery_project.domain.entity.delivery;

import com.example.delivery_project.domain.entity.enums.DeliveryStopStatus;
import com.example.delivery_project.domain.entity.enums.ProductType;
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
}