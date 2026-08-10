package com.example.delivery_project.domain.entity.delivery;

import com.example.delivery_project.domain.entity.enums.RiskFactorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RiskFactor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "risk_assessment_id", nullable = false)
    private RiskAssessment riskAssessment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskFactorType type;

    @Column(nullable = false)
    private Integer penaltyScore;

    private String description;
}