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
import com.serviceOrder.Management.enums.OrderPriority;
import com.serviceOrder.Management.enums.OrderStatus;
import com.serviceOrder.Management.repositories.ClientRepository;
import com.serviceOrder.Management.repositories.ServiceOrderRepository;
import com.serviceOrder.Management.repositories.TechnicianRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceOrderServiceTest {

    private static final Long ORDER_ID = 1L;
    private static final Long MISSING_ID = 999L;
    private static final Long TECH_ID = 1L;
    private static final Long CLIENT_ID = 1L;

    @InjectMocks
    private ServiceOrderService serviceOrderService;

    @Mock
    private ServiceOrderRepository orderRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private TechnicianRepository technicianRepository;

    private Technician technician;

    @BeforeEach
    void setUp() {
        technician = new Technician();
        technician.setId(TECH_ID);
        technician.setName("Carlos Silva");
    }

    private ServiceOrder orderWithStatus(OrderStatus status) {
        ServiceOrder order = new ServiceOrder();
        order.setId(ORDER_ID);
        order.setStatus(status);
        return order;
    }

    // ---------- findById ----------

    @Test
    @DisplayName("findById should throw ResourceNotFoundException when order does not exist")
    void findByIdShouldThrowWhenNotFound() {
        when(orderRepository.findById(MISSING_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> serviceOrderService.findById(MISSING_ID));
    }

    // ---------- findAll ----------

    @Test
    @DisplayName("findAll should return a page of DTOs")
    void findAllShouldReturnPageOfDtos() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ServiceOrder> page = new PageImpl<>(List.of(orderWithStatus(OrderStatus.OPEN)), pageable, 1);
        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<ServiceOrderDTO> result = serviceOrderService.findAll(new OrderFilterDTO(null, null, null, null, null), pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(ORDER_ID, result.getContent().get(0).id());
    }

    // ---------- create ----------

    @Test
    @DisplayName("create should open a new order for an existing client")
    void createShouldOpenOrderWhenClientExists() {
        Client client = new Client(CLIENT_ID, "Acme", "acme@mail.com", "12345678900");
        ServiceOrderCreateDTO dto = new ServiceOrderCreateDTO("Printer down", "Does not print", OrderPriority.HIGH, CLIENT_ID);
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(client));
        when(orderRepository.save(any(ServiceOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceOrderDTO result = serviceOrderService.create(dto);

        assertEquals(OrderStatus.OPEN, result.status());
        assertEquals(OrderPriority.HIGH, result.priority());
        assertEquals(CLIENT_ID, result.client().id());
        assertNotNull(result.createdAt());
    }

    @Test
    @DisplayName("create should throw ResourceNotFoundException when client does not exist")
    void createShouldThrowWhenClientNotFound() {
        ServiceOrderCreateDTO dto = new ServiceOrderCreateDTO("Printer down", "Does not print", OrderPriority.LOW, MISSING_ID);
        when(clientRepository.findById(MISSING_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> serviceOrderService.create(dto));
        verify(orderRepository, never()).save(any());
    }

    // ---------- assignTechnician ----------

    @Test
    @DisplayName("assignTechnician should set technician and status IN_PROGRESS when IDs exist")
    void assignTechnicianShouldUpdateStatusWhenIdsExist() {
        ServiceOrder order = orderWithStatus(OrderStatus.OPEN);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(technicianRepository.findById(TECH_ID)).thenReturn(Optional.of(technician));
        when(orderRepository.save(any(ServiceOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceOrderDTO result = serviceOrderService.assignTechnician(ORDER_ID, TECH_ID);

        assertNotNull(result);
        assertEquals(OrderStatus.IN_PROGRESS, order.getStatus());
        assertEquals(technician, order.getTechnician());
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    @DisplayName("assignTechnician should throw ResourceNotFoundException when order does not exist")
    void assignTechnicianShouldThrowWhenOrderNotFound() {
        when(orderRepository.findById(MISSING_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> serviceOrderService.assignTechnician(MISSING_ID, TECH_ID));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("assignTechnician should throw ResourceNotFoundException when technician does not exist")
    void assignTechnicianShouldThrowWhenTechnicianNotFound() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(orderWithStatus(OrderStatus.OPEN)));
        when(technicianRepository.findById(MISSING_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> serviceOrderService.assignTechnician(ORDER_ID, MISSING_ID));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("assignTechnician should throw BusinessRuleException when order is already FINISHED")
    void assignTechnicianShouldThrowWhenOrderFinished() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(orderWithStatus(OrderStatus.FINISHED)));

        assertThrows(BusinessRuleException.class, () -> serviceOrderService.assignTechnician(ORDER_ID, TECH_ID));
        verify(orderRepository, never()).save(any());
    }

    // ---------- finish ----------

    @Test
    @DisplayName("finish should store report and finishedAt when order is IN_PROGRESS")
    void finishShouldCloseOrderWhenInProgress() {
        ServiceOrder order = orderWithStatus(OrderStatus.IN_PROGRESS);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(ServiceOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceOrderDTO result = serviceOrderService.finish(ORDER_ID, new ServiceOrderFinishDTO("Power supply replaced"));

        assertEquals(OrderStatus.FINISHED, result.status());
        assertEquals("Power supply replaced", result.rootCauseReport());
        assertNotNull(result.finishedAt());
    }

    @Test
    @DisplayName("finish should throw BusinessRuleException when order is still OPEN")
    void finishShouldThrowWhenOrderOpen() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(orderWithStatus(OrderStatus.OPEN)));

        assertThrows(BusinessRuleException.class,
                () -> serviceOrderService.finish(ORDER_ID, new ServiceOrderFinishDTO("report")));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("finish should throw BusinessRuleException when order is already FINISHED")
    void finishShouldThrowWhenAlreadyFinished() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(orderWithStatus(OrderStatus.FINISHED)));

        assertThrows(BusinessRuleException.class,
                () -> serviceOrderService.finish(ORDER_ID, new ServiceOrderFinishDTO("report")));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("finish should throw ResourceNotFoundException when order does not exist")
    void finishShouldThrowWhenOrderNotFound() {
        when(orderRepository.findById(MISSING_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> serviceOrderService.finish(MISSING_ID, new ServiceOrderFinishDTO("report")));
    }

    // ---------- cancel ----------

    @Test
    @DisplayName("cancel should set status CANCELED when order is OPEN")
    void cancelShouldCancelOpenOrder() {
        ServiceOrder order = orderWithStatus(OrderStatus.OPEN);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(ServiceOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceOrderDTO result = serviceOrderService.cancel(ORDER_ID);

        assertEquals(OrderStatus.CANCELED, result.status());
    }

    @Test
    @DisplayName("cancel should throw BusinessRuleException when order is FINISHED")
    void cancelShouldThrowWhenOrderFinished() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(orderWithStatus(OrderStatus.FINISHED)));

        assertThrows(BusinessRuleException.class, () -> serviceOrderService.cancel(ORDER_ID));
        verify(orderRepository, never()).save(any());
    }
}