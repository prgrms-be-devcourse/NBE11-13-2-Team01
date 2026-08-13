package com.example.delivery_project.enums;

public enum RiskLevel {
    SAFE,
    CAUTION,
    DANGER;

    public static RiskLevel from(int riskScore) {
        if(riskScore >= 70) return DANGER;
        if(riskScore >= 40) return CAUTION;
        return SAFE;
    }
}
