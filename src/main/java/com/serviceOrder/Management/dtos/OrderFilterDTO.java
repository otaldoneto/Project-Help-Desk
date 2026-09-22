package com.serviceOrder.Management.dtos;

import com.serviceOrder.Management.enums.OrderPriority;
import com.serviceOrder.Management.enums.OrderStatus;

// Every field is optional: a null means "do not filter by this"
public record OrderFilterDTO(
        OrderStatus status,
        OrderPriority priority,
        Long clientId,
        Long technicianId,
        String title
) {
}