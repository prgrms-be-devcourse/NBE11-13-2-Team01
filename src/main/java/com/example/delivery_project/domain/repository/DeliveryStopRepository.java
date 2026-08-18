package com.example.delivery_project.domain.repository;

import com.example.delivery_project.domain.entity.delivery.DeliveryStop;
import com.example.delivery_project.enums.DeliveryStopStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeliveryStopRepository extends JpaRepository<DeliveryStop, Long> {

    List<DeliveryStop> findAllByStatusIn(
            Collection<DeliveryStopStatus> statuses
    );

    List<DeliveryStop> findAllByDeliveryPlanIdAndStatusIn(
            Long planId,
            Collection<DeliveryStopStatus> statuses
    );

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
