package com.example.delivery_project.domain.entity.delivery;

import com.example.delivery_project.domain.entity.enums.ProductType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_stop_id", nullable = false)
    private DeliveryStop deliveryStop;

    @Column(nullable = false)
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductType productType;

    @Column(nullable = false)
    private Integer quantity;

    static DeliveryItem of(
            DeliveryStop deliveryStop,
            String productName,
            ProductType productType,
            Integer quantity
    ) {
        if(quantity == null || quantity <= 0) {
            // TODO Delivery 커스텀 예외로 교체
            throw new IllegalArgumentException("quantity must be greater than or equal to 0");
        }
        DeliveryItem deliveryItem = new DeliveryItem();
        deliveryItem.deliveryStop = deliveryStop;
        deliveryItem.productName = productName;
        deliveryItem.productType = productType;
        deliveryItem.quantity = quantity;
        return deliveryItem;

    }
}
