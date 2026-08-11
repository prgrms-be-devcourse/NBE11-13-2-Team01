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

    static DeliveryStop of(
            DeliveryPlan deliveryPlan,
            String address,
            Double latitude,
            Double longitude
    ) {
        validateAddress(address);
        validateCoordinate(latitude, longitude);
        DeliveryStop deliveryStop = new DeliveryStop();
        deliveryStop.deliveryPlan = deliveryPlan;
        deliveryStop.status = DeliveryStopStatus.READY;
        deliveryStop.address = address;
        deliveryStop.latitude = latitude;
        deliveryStop.longitude = longitude;
        return deliveryStop;
    }

    public DeliveryItem addItem (
        String productName,
        ProductType productType,
        Integer quantity
    ) {
        DeliveryItem deliveryItem = DeliveryItem.of(
                this,
                productName,
                productType,
                quantity
        );
        deliveryItems.add(deliveryItem);
        return deliveryItem;
    }

    private static void validateAddress(String address) {
        if (address == null || address.isBlank()) {
            // TODO Delivery 커스텀 예외로 교체
            throw new IllegalArgumentException("배송지 주소는 필수입니다.");
        }
    }

    private static void validateCoordinate(
            Double latitude,
            Double longitude
    ) {
        if (latitude == null || latitude < -90 || latitude > 90) {
            // TODO Delivery 커스텀 예외로 교체
            throw new IllegalArgumentException("위도가 올바르지 않습니다.");
        }

        if (longitude == null || longitude < -180 || longitude > 180) {
            // TODO Delivery 커스텀 예외로 교체
            throw new IllegalArgumentException("경도가 올바르지 않습니다.");
        }
    }

    public void complete() {
        if(!status.isDelivering()) {
            // TODO Delivery 커스텀 예외로 교체
            throw new IllegalStateException("배송중이 아니면 완료할 수 없습니다");
        }
    }
}