package com.example.delivery_project.domain.repository;

import com.example.delivery_project.domain.entity.delivery.DeliveryStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface DeliveryStopRepository extends JpaRepository<DeliveryStop, Long> {
    @Query("""
        select distinct s
        from DeliveryStop s
        left join fetch s.deliveryItems
        where s.id = :stopId
          and s.deliveryPlan.id = :planId
    """)
    Optional<DeliveryStop> findDetailByIdAndPlanId(
            Long stopId,
            Long planId
    );
}
