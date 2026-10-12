package com.byfood.api.repository;

import com.byfood.api.model.Order;
import com.byfood.api.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = "restaurant")
    Optional<Order> findByTrackingCode(UUID trackingCode);

    @EntityGraph(attributePaths = "restaurant")
    Optional<Order> findByIdAndRestaurantId(Long id, Long restaurantId);

    @EntityGraph(attributePaths = "restaurant")
    Page<Order> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId, Pageable pageable);

    @EntityGraph(attributePaths = "restaurant")
    Page<Order> findByRestaurantIdAndStatusOrderByCreatedAtDesc(Long restaurantId, OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "restaurant")
    Page<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);
}
