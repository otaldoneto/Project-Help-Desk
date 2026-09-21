package com.serviceOrder.Management.controllers;

import com.serviceOrder.Management.entities.OrderService;
import com.serviceOrder.Management.enums.OrderStatus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OptimisticLockingTest extends ApiTestSupport {

    @Test
    @DisplayName("saving a stale copy of a service order should fail instead of overwriting the newer change")
    void staleCopyShouldNotOverwriteNewerChange() {
        Long id = saveOrder(saveClient(), "Printer down", OrderStatus.IN_PROGRESS).getId();

        // Two requests read the same order before either of them writes.
        OrderService first = orderRepository.findById(id).orElseThrow();
        OrderService second = orderRepository.findById(id).orElseThrow();

        first.setStatus(OrderStatus.CANCELED);
        orderRepository.save(first);

        second.setStatus(OrderStatus.FINISHED);
        assertThrows(ObjectOptimisticLockingFailureException.class, () -> orderRepository.save(second));

        assertEquals(OrderStatus.CANCELED, orderRepository.findById(id).orElseThrow().getStatus());
    }
}