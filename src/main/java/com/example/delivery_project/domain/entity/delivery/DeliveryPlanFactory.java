package com.example.delivery_project.domain.entity.delivery;

import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.spec.DeliveryItemSpec;
import com.example.delivery_project.spec.DeliveryStopSpec;
import com.example.delivery_project.spec.Location;
import com.example.delivery_project.spec.RiskFactorSpec;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DeliveryPlanFactory {

    public static DeliveryPlan create(
            User driver,
            Location departureLocation,
            LocalDateTime scheduledDepartureAt,
            List<DeliveryStopSpec> stopSpecs
    ) {
        DeliveryPlan plan = DeliveryPlan.of(
                driver,
                departureLocation,
                scheduledDepartureAt
        );
        LocalDateTime analyzedAt = LocalDateTime.now();

        for(DeliveryStopSpec stopSpec : stopSpecs){
            DeliveryStop stop = plan.addStop(
                    departureLocation,
                    analyzedAt
            );

            addItems(stop, stopSpec.items());
            addRiskFactors(stop, stopSpec.riskFactors());
        }

        return plan;
    }

    public static DeliveryPlan create(
            User driver,
            Location departureLocation,
            LocalDateTime scheduledDepartureAt
    ) {
        return create(
                driver,
                departureLocation,
                scheduledDepartureAt,
                List.of()
        );
    }

    private static void addItems(
            DeliveryStop stop,
            List<DeliveryItemSpec> items
    ) {
        for(DeliveryItemSpec itemSpec : items){
            stop.addItem(
                    itemSpec.productName(),
                    itemSpec.productType(),
                    itemSpec.quantity()
            );
        }
    }

    private static void addRiskFactors(
            DeliveryStop stop,
            List<RiskFactorSpec> riskFactors
    ) {
        for(RiskFactorSpec riskFactorSpec : riskFactors){
            stop.addRiskFactor(
                    riskFactorSpec.type(),
                    riskFactorSpec.description()
            );
        }
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
