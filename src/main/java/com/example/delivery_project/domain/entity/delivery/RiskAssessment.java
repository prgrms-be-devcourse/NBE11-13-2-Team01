package com.example.delivery_project.domain.entity.delivery;

import com.example.delivery_project.domain.entity.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RiskAssessment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_segment_id", nullable = false)
    private RouteSegment routeSegment;

    @Column(nullable = false)
    private Integer score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel level;

    @Column(nullable = false)
    private LocalDateTime analyzedAt;
}
