package com.serviceOrder.Management.dtos;

import com.serviceOrder.Management.entities.OrderService;
import com.serviceOrder.Management.enums.OrderPriority;
import com.serviceOrder.Management.enums.OrderStatus;

import java.time.Instant;

public record OrderServiceDTO(
        Long id,
        String title,
        String description,
        OrderStatus status,
        OrderPriority priority,
        Instant createdAt,
        Instant finishedAt,
        String createdBy,
        String lastModifiedBy,
        Instant lastModifiedAt,
        String rootCauseReport,
        ClientDTO client,
        TechnicianDTO technician
) {
    public OrderServiceDTO(OrderService entity) {
        this(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getPriority(),
                entity.getCreatedAt(),
                entity.getFinishedAt(),
                entity.getCreatedBy(),
                entity.getLastModifiedBy(),
                entity.getLastModifiedAt(),
                entity.getRootCauseReport(),
                entity.getClient() != null ? new ClientDTO(entity.getClient()) : null,
                entity.getTechnician() != null ? new TechnicianDTO(entity.getTechnician()) : null
        );
    }
}