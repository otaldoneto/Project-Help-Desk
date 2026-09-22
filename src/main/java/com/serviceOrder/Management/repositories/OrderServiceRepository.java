package com.serviceOrder.Management.repositories;

import com.serviceOrder.Management.entities.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface OrderServiceRepository
        extends JpaRepository<OrderService, Long>, JpaSpecificationExecutor<OrderService> {

    @Override
    @EntityGraph(attributePaths = {"client", "technician"})
    Page<OrderService> findAll(Specification<OrderService> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"client", "technician"})
    Optional<OrderService> findById(Long id);

    boolean existsByClientId(Long clientId);

    boolean existsByTechnicianId(Long technicianId);
}