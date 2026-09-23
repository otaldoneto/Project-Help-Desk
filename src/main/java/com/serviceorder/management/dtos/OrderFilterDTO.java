package com.serviceorder.management.dtos;

import com.serviceorder.management.enums.OrderPriority;
import com.serviceorder.management.enums.OrderStatus;

// Every field is optional: a null means "do not filter by this"
public record OrderFilterDTO(
        OrderStatus status,
        OrderPriority priority,
        Long clientId,
        Long technicianId,
        String title
) {
}