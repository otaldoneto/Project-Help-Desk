package com.serviceOrder.Management.services;

import com.serviceOrder.Management.controllers.exceptions.ResourceNotFoundException;
import com.serviceOrder.Management.dtos.OrderServiceCreateDTO;
import com.serviceOrder.Management.dtos.OrderServiceDTO;
import com.serviceOrder.Management.dtos.OrderServiceFinishDTO;
import com.serviceOrder.Management.entities.Client;
import com.serviceOrder.Management.entities.OrderService;
import com.serviceOrder.Management.entities.Technician;
import com.serviceOrder.Management.enums.OrderStatus;
import com.serviceOrder.Management.repositories.ClientRepository;
import com.serviceOrder.Management.repositories.OrderServiceRepository;
import com.serviceOrder.Management.repositories.TechnicianRepository;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

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
    public List<OrderServiceDTO> findAll() {
        return repository.findAll().stream().map(OrderServiceDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public OrderServiceDTO findById(Long id) {
        OrderService entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Service Order not found with id " + id));
        return new OrderServiceDTO(entity);
    }

    @Transactional
    public OrderServiceDTO create(OrderServiceCreateDTO dto) {
        Client client = clientRepository.findById(dto.clientId())
                .orElseThrow(() -> new RuntimeException("Service Order not found with id: " + dto.clientId()));
        OrderService entity = new OrderService(null, dto.title(), dto.description(), dto.priority(), client);
        entity = repository.save(entity);
        return new OrderServiceDTO(entity);
    }
    @Transactional
    public OrderServiceDTO assignTechnician(Long orderId, Long technicianId) {
        OrderService entity = repository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Service Order not found with id: " + orderId));
        Technician technician = technicianRepository.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found with id: " + technicianId));
        entity.setTechnician(technician);
        entity.setStatus(OrderStatus.IN_PROGRESS);
        entity = repository.save(entity);
        return new OrderServiceDTO(entity);
    }
    @Transactional
    public OrderServiceDTO finish(Long orderId, OrderServiceFinishDTO dto) {
        OrderService entity = repository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Service Order not found with id: " + orderId));
        //Business Rule: A root cause analysis report is mandatory to close the work order.
        if(dto.rootCauseReport() == null || dto.rootCauseReport().trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot finish a Service Order without a root cause report");
        }

        entity.setRootCauseReport(dto.rootCauseReport());
        entity.setStatus(OrderStatus.FINISHED);
        entity.setFinishedAt(Instant.now());

        entity = repository.save(entity);
        return new OrderServiceDTO(entity);
    }


}
