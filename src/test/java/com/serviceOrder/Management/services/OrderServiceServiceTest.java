package com.serviceOrder.Management.services;

import com.serviceOrder.Management.controllers.exceptions.ResourceNotFoundException;
import com.serviceOrder.Management.dtos.OrderServiceDTO;
import com.serviceOrder.Management.entities.OrderService;
import com.serviceOrder.Management.entities.Technician;
import com.serviceOrder.Management.enums.OrderStatus;
import com.serviceOrder.Management.repositories.OrderServiceRepository;
import com.serviceOrder.Management.repositories.TechnicianRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceServiceTest {

    @InjectMocks
    private OrderServiceService orderService;

    @Mock
    private OrderServiceRepository orderRepository;

    @Mock
    private TechnicianRepository technicianRepository;

    private Long existingOrderId;
    private Long nonExistingOrderId;
    private Long existingTechId;
    private OrderService order;
    private Technician technician;

    @BeforeEach
    void setUp() {
        existingOrderId = 1L;
        nonExistingOrderId = 999L;
        existingTechId = 1L;

        order = new OrderService();
        order.setId(existingOrderId);
        order.setStatus(OrderStatus.OPEN);

        technician = new Technician();
        technician.setId(existingTechId);
        technician.setName("Carlos Silva");
    }

    @Test
    @DisplayName("Should assign technician and change status to IN_PROGRESS when IDs exist")
    void assignTechnicianShouldUpdateStatusWhenIdsExist() {
        when(orderRepository.findById(existingOrderId)).thenReturn(Optional.of(order));
        when(technicianRepository.findById(existingTechId)).thenReturn(Optional.of(technician));
        when(orderRepository.save(any(OrderService.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderServiceDTO result = orderService.assignTechnician(existingOrderId, existingTechId);

        assertNotNull(result);
        assertEquals(OrderStatus.IN_PROGRESS, order.getStatus());
        assertEquals(technician, order.getTechnician());
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when order ID does not exist")
    void assignTechnicianShouldThrowExceptionWhenIdDoesNotExist() {
        when(orderRepository.findById(nonExistingOrderId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.assignTechnician(nonExistingOrderId, existingTechId);
        });

        verify(orderRepository, never()).save(any());
    }
}