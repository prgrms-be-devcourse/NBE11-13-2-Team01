package com.example.delivery_project.domain.repository;

import com.example.delivery_project.domain.entity.delivery.DeliveryPlan;
import com.example.delivery_project.domain.entity.delivery.DeliveryStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DeliveryPlanRepository extends JpaRepository<DeliveryPlan,Long> {
    @Query("""
    select distinct p
    from DeliveryPlan p
    join fetch p.deliveryStops s
    join fetch s.deliveryItems
    where p.id = :id
""")
    Optional<DeliveryPlan> findDetailById(Long id);

    List<DeliveryPlan> findAllByDriverId(Long driverId);
}
