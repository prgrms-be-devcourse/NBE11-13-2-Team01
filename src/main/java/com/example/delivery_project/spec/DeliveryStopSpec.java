package com.example.delivery_project.spec;

import java.util.List;

public record DeliveryStopSpec(
        Location location,
        List<DeliveryItemSpec> items,
        List<RiskFactorSpec> riskFactors
){

    public DeliveryStopSpec {
        items = items == null ? List.of() : List.copyOf(items);
        riskFactors = riskFactors == null ? List.of() : List.copyOf(riskFactors);

    }
}
