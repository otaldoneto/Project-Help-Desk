package com.serviceOrder.Management.repositories;

import com.serviceOrder.Management.entities.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderServiceRepository extends JpaRepository<OrderService, Long> {

    @Override
    @EntityGraph(attributePaths = {"client", "technician"})
    Page<OrderService> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"client", "technician"})
    Optional<OrderService> findById(Long id);
}