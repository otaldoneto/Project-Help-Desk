package com.serviceOrder.Management.controllers.exceptions;

import com.serviceOrder.Management.entities.OrderService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourceExceptionHandlerTest {

    @Test
    @DisplayName("an optimistic locking failure should become a 409 in the standard error format")
    void optimisticLockShouldReturn409() {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/orders/1/finish");

        ResponseEntity<StandardError> response = new ResourceExceptionHandler()
                .optimisticLock(new ObjectOptimisticLockingFailureException(OrderService.class, 1L), request);

        assertEquals(409, response.getStatusCode().value());
        assertEquals("Concurrent modification", response.getBody().error());
        assertEquals("/orders/1/finish", response.getBody().path());
    }
}