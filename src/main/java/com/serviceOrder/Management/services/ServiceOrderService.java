package com.serviceOrder.Management.services;

import com.serviceOrder.Management.controllers.exceptions.BusinessRuleException;
import com.serviceOrder.Management.controllers.exceptions.ResourceNotFoundException;
import com.serviceOrder.Management.dtos.OrderFilterDTO;
import com.serviceOrder.Management.dtos.ServiceOrderCreateDTO;
import com.serviceOrder.Management.dtos.ServiceOrderDTO;
import com.serviceOrder.Management.dtos.ServiceOrderFinishDTO;
import com.serviceOrder.Management.entities.Client;
import com.serviceOrder.Management.entities.ServiceOrder;
import com.serviceOrder.Management.entities.Technician;
import com.serviceOrder.Management.enums.OrderStatus;
import com.serviceOrder.Management.repositories.ClientRepository;
import com.serviceOrder.Management.repositories.ServiceOrderRepository;
import com.serviceOrder.Management.repositories.ServiceOrderSpecifications;
import com.serviceOrder.Management.repositories.TechnicianRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Arrays;


@Service
public class ServiceOrderService {
    private final ServiceOrderRepository repository;
    private final ClientRepository clientRepository;
    private final TechnicianRepository technicianRepository;

    public ServiceOrderService(ServiceOrderRepository repository,
                               ClientRepository clientRepository,
                               TechnicianRepository technicianRepository) {
        this.repository = repository;
        this.clientRepository = clientRepository;
        this.technicianRepository = technicianRepository;
    }

    @Transactional(readOnly = true)
    public Page<ServiceOrderDTO> findAll(OrderFilterDTO filter, Pageable pageable) {
        return repository.findAll(ServiceOrderSpecifications.from(filter), pageable).map(ServiceOrderDTO::new);
    }

    @Transactional(readOnly = true)
    public ServiceOrderDTO findById(Long id) {
        return new ServiceOrderDTO(findOrder(id));
    }

    @Transactional
    public ServiceOrderDTO create(ServiceOrderCreateDTO dto) {
        Client client = clientRepository.findById(dto.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + dto.clientId()));
        ServiceOrder entity = new ServiceOrder(null, dto.title(), dto.description(), dto.priority(), client);
        entity = repository.save(entity);
        return new ServiceOrderDTO(entity);
    }

    @Transactional
    public ServiceOrderDTO assignTechnician(Long orderId, Long technicianId) {
        ServiceOrder entity = findOrder(orderId);
        ensureStatusIn(entity, "assign a technician to", OrderStatus.OPEN, OrderStatus.IN_PROGRESS);
        Technician technician = technicianRepository.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found with id: " + technicianId));
        entity.setTechnician(technician);
        entity.setStatus(OrderStatus.IN_PROGRESS);
        entity = repository.save(entity);
        // Forces the UPDATE now, so the @PreUpdate auditing listener sets lastModifiedBy/lastModifiedAt
        // before we read them into the response DTO (an UPDATE is otherwise deferred to the transaction's flush).
        repository.flush();
        return new ServiceOrderDTO(entity);
    }

    @Transactional
    public ServiceOrderDTO finish(Long orderId, ServiceOrderFinishDTO dto) {
        ServiceOrder entity = findOrder(orderId);
        ensureStatusIn(entity, "finish", OrderStatus.IN_PROGRESS);
        entity.setRootCauseReport(dto.rootCauseReport());
        entity.setStatus(OrderStatus.FINISHED);
        entity.setFinishedAt(Instant.now());
        entity = repository.save(entity);
        repository.flush();
        return new ServiceOrderDTO(entity);
    }

    @Transactional
    public ServiceOrderDTO cancel(Long orderId) {
        ServiceOrder entity = findOrder(orderId);
        ensureStatusIn(entity, "cancel", OrderStatus.OPEN, OrderStatus.IN_PROGRESS);
        entity.setStatus(OrderStatus.CANCELED);
        entity = repository.save(entity);
        repository.flush();
        return new ServiceOrderDTO(entity);
    }

    private ServiceOrder findOrder(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service Order not found with id: " + id));
    }

    // Allowed transitions: OPEN -> IN_PROGRESS -> FINISHED, and OPEN/IN_PROGRESS -> CANCELED.
    private void ensureStatusIn(ServiceOrder order, String action, OrderStatus... allowed) {
        if (!Arrays.asList(allowed).contains(order.getStatus())) {
            throw new BusinessRuleException(
                    "Cannot " + action + " a service order with status " + order.getStatus());
        }
    }
}