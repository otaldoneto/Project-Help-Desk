package com.serviceOrder.Management.repositories;

import com.serviceOrder.Management.entities.ServiceOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ServiceOrderRepository
        extends JpaRepository<ServiceOrder, Long>, JpaSpecificationExecutor<ServiceOrder> {

    @Override
    @EntityGraph(attributePaths = {"client", "technician"})
    Page<ServiceOrder> findAll(Specification<ServiceOrder> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"client", "technician"})
    Optional<ServiceOrder> findById(Long id);

    boolean existsByClientId(Long clientId);

    boolean existsByTechnicianId(Long technicianId);
}