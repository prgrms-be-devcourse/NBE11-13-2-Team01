package com.example.delivery_project.domain.entity.delivery;

import com.example.delivery_project.domain.entity.enums.RiskFactorType;
import com.example.delivery_project.domain.entity.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RiskAssessment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_stop_id", nullable = false)
    private DeliveryStop deliveryStop;

    @Column(nullable = false)
    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel level;

    @Column(nullable = false)
    private LocalDateTime analyzedAt;

    boolean isDanger() {
        return level == RiskLevel.DANGER;
    }

    public static RiskAssessment of(
            DeliveryStop deliveryStop,
            Integer riskScore
    ){
        RiskAssessment riskAssessment = new RiskAssessment();
        riskAssessment.deliveryStop=deliveryStop;
        riskAssessment.riskScore=riskScore;
        riskAssessment.level=RiskLevel.from(riskScore);
        riskAssessment.analyzedAt=LocalDateTime.now();
        return riskAssessment;
    }
    //RiskFactor과 연결 : RiskAccessment는 RiskFactor를 모른다!
    public RiskFactor addRiskFactor(
            RiskFactorType type,
            String description
    ){
        RiskFactor riskFactor = RiskFactor.of(
                this,
                type,
                description
        );
        return riskFactor;
    }
    //RiskAssessment update
    private void updateScore(Integer riskScore){
        this.riskScore=riskScore;
        this.level=RiskLevel.from(riskScore);
        this.analyzedAt=LocalDateTime.now();
    }
    private void calcRiskScore(){
        //1. 이 riskAssessment를 가진 모든 RiskFactor 찾아서
        //2. FactorType에서 점수 꺼내 더한다

    }


}
