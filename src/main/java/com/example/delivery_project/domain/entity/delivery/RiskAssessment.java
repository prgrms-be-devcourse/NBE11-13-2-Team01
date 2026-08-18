package com.example.delivery_project.domain.entity.delivery;

import com.example.delivery_project.enums.RiskFactorType;
import com.example.delivery_project.enums.RiskLevel;
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
public class RiskAssessment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "delivery_stop_id",
            nullable = false,
            unique = true
    )
    private DeliveryStop deliveryStop;

    @OneToMany(
            mappedBy = "riskAssessment",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Getter(AccessLevel.NONE)
    private List<RiskFactor> riskFactors = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel level;

    @Column(nullable = false)
    private LocalDateTime analyzedAt;

    public static RiskAssessment of(
            DeliveryStop stop,
            LocalDateTime analyzedAt
    ) {
        RiskAssessment assessment = new RiskAssessment();
        assessment.deliveryStop = stop;
        assessment.analyzedAt = analyzedAt;
        assessment.level = RiskLevel.UNKNOWN;
        return assessment;
    }

    boolean isDanger() {
        return level == RiskLevel.DANGER;
    }

    public List<RiskFactor> getRiskFactors() {
        return Collections.unmodifiableList(riskFactors);
    }

    public int getScore() {
        if (level == RiskLevel.UNKNOWN) {
            return -1;
        }

        return riskFactors.stream()
                .mapToInt(RiskFactor::getRiskScore)
                .sum();
    }

    public void markUnknown(LocalDateTime analyzedAt) {
        riskFactors.clear();
        this.level = RiskLevel.UNKNOWN;
        this.analyzedAt = analyzedAt;
    }


    private void addFactor(RiskFactor factor) {
        riskFactors.add(factor);
        recalculateRiskLevel();
    }

    public void addFactor(
            RiskFactorType type,
            String description
    ) {
        this.addFactor(RiskFactor.of(
                this,
                type,
                description
        ));
    }

    private void recalculateRiskLevel() {
        this.level = RiskLevel.from(getFactorScore());
    }

    private int getFactorScore() {
        return riskFactors.stream()
                .mapToInt(RiskFactor::getRiskScore)
                .sum();
    }

    public void replaceFactors(
            List<RiskFactorType> types,
            LocalDateTime analyzedAt
    ) {
        riskFactors.clear();

        for (RiskFactorType type : types) {
            riskFactors.add(RiskFactor.of(
                    this,
                    type,
                    type.getDescription()
            ));
        }

        this.analyzedAt = analyzedAt;
        recalculateRiskLevel();
    }
}
