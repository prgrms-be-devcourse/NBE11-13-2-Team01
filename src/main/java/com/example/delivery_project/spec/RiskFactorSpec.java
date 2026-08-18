package com.example.delivery_project.spec;

import com.example.delivery_project.enums.RiskFactorType;

public record RiskFactorSpec(
        RiskFactorType type,
        String description
){
}
