package com.example.delivery_project.domain.entity.delivery;

import com.example.delivery_project.enums.ProductType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_stop_id", nullable = false)
    private DeliveryStop deliveryStop;

    @Column(nullable = false)
    @NotBlank
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductType productType;

    @Column(nullable = false)
    @Positive
    private Integer quantity;

    static DeliveryItem of(
            DeliveryStop deliveryStop,
            String productName,
            ProductType productType,
            Integer quantity
    ) {
        DeliveryItem item = new DeliveryItem();
        item.deliveryStop = deliveryStop;
        item.productName = productName;
        item.productType = productType;
        item.quantity = quantity;
        return item;
    }
}
