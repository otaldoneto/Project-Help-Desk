package com.serviceOrder.Management.services;

import com.serviceOrder.Management.controllers.exceptions.BusinessRuleException;
import com.serviceOrder.Management.controllers.exceptions.ResourceNotFoundException;
import com.serviceOrder.Management.dtos.OrderFilterDTO;
import com.serviceOrder.Management.dtos.OrderServiceCreateDTO;
import com.serviceOrder.Management.dtos.OrderServiceDTO;
import com.serviceOrder.Management.dtos.OrderServiceFinishDTO;
import com.serviceOrder.Management.entities.Client;
import com.serviceOrder.Management.entities.OrderService;
import com.serviceOrder.Management.entities.Technician;
import com.serviceOrder.Management.enums.OrderStatus;
import com.serviceOrder.Management.repositories.ClientRepository;
import com.serviceOrder.Management.repositories.OrderServiceRepository;
import com.serviceOrder.Management.repositories.OrderServiceSpecifications;
import com.serviceOrder.Management.repositories.TechnicianRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Arrays;


@Service
public class OrderServiceService {
    private final OrderServiceRepository repository;
    private final ClientRepository clientRepository;
    private final TechnicianRepository technicianRepository;

    public OrderServiceService(OrderServiceRepository repository,
                               ClientRepository clientRepository,
                               TechnicianRepository technicianRepository) {
        this.repository = repository;
        this.clientRepository = clientRepository;
        this.technicianRepository = technicianRepository;
    }

    @Transactional(readOnly = true)
    public Page<OrderServiceDTO> findAll(OrderFilterDTO filter, Pageable pageable) {
        return repository.findAll(OrderServiceSpecifications.from(filter), pageable).map(OrderServiceDTO::new);
    }

    @Transactional(readOnly = true)
    public OrderServiceDTO findById(Long id) {
        return new OrderServiceDTO(findOrder(id));
    }

    @Transactional
    public OrderServiceDTO create(OrderServiceCreateDTO dto) {
        Client client = clientRepository.findById(dto.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + dto.clientId()));
        OrderService entity = new OrderService(null, dto.title(), dto.description(), dto.priority(), client);
        entity = repository.save(entity);
        return new OrderServiceDTO(entity);
    }

    @Transactional
    public OrderServiceDTO assignTechnician(Long orderId, Long technicianId) {
        OrderService entity = findOrder(orderId);
        ensureStatusIn(entity, "assign a technician to", OrderStatus.OPEN, OrderStatus.IN_PROGRESS);
        Technician technician = technicianRepository.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found with id: " + technicianId));
        entity.setTechnician(technician);
        entity.setStatus(OrderStatus.IN_PROGRESS);
        entity = repository.save(entity);
        // Forces the UPDATE now, so the @PreUpdate auditing listener sets lastModifiedBy/lastModifiedAt
        // before we read them into the response DTO (an UPDATE is otherwise deferred to the transaction's flush).
        repository.flush();
        return new OrderServiceDTO(entity);
    }

    @Transactional
    public OrderServiceDTO finish(Long orderId, OrderServiceFinishDTO dto) {
        OrderService entity = findOrder(orderId);
        ensureStatusIn(entity, "finish", OrderStatus.IN_PROGRESS);
        entity.setRootCauseReport(dto.rootCauseReport());
        entity.setStatus(OrderStatus.FINISHED);
        entity.setFinishedAt(Instant.now());
        entity = repository.save(entity);
        repository.flush();
        return new OrderServiceDTO(entity);
    }

    @Transactional
    public OrderServiceDTO cancel(Long orderId) {
        OrderService entity = findOrder(orderId);
        ensureStatusIn(entity, "cancel", OrderStatus.OPEN, OrderStatus.IN_PROGRESS);
        entity.setStatus(OrderStatus.CANCELED);
        entity = repository.save(entity);
        repository.flush();
        return new OrderServiceDTO(entity);
    }

    private OrderService findOrder(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service Order not found with id: " + id));
    }

    // Allowed transitions: OPEN -> IN_PROGRESS -> FINISHED, and OPEN/IN_PROGRESS -> CANCELED.
    private void ensureStatusIn(OrderService order, String action, OrderStatus... allowed) {
        if (!Arrays.asList(allowed).contains(order.getStatus())) {
            throw new BusinessRuleException(
                    "Cannot " + action + " a service order with status " + order.getStatus());
        }
    }
}