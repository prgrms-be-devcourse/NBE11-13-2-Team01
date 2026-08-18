package com.example.delivery_project.enums;

public enum RiskLevel {
    UNKNOWN,
    SAFE,
    CAUTION,
    DANGER;

    public static RiskLevel from(int riskScore) {
        if(riskScore < 0) return UNKNOWN;
        if(riskScore >= 70) return DANGER;
        if(riskScore >= 40) return CAUTION;
        return SAFE;
    }
}
