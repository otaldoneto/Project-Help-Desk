package com.serviceOrder.Management.controllers;

import com.serviceOrder.Management.dtos.OrderServiceCreateDTO;
import com.serviceOrder.Management.dtos.OrderServiceDTO;
import com.serviceOrder.Management.dtos.OrderServiceFinishDTO;
import com.serviceOrder.Management.services.OrderServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Tag(name = "Service Orders", description = "Endpoints to manage the service order lifecycle")
@RestController
@RequestMapping(value = "/orders")
public class OrderServiceController {
    private final OrderServiceService service;

    public OrderServiceController(OrderServiceService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<OrderServiceDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<OrderServiceDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @Operation(summary = "Creates a service order", description = "Registers a new service order for a client")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Service order created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Client not found")
    })
    @PostMapping
    public ResponseEntity<OrderServiceDTO> create(@Valid @RequestBody OrderServiceCreateDTO dto) {
        OrderServiceDTO createdDto = service.create(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(createdDto.id()).toUri();
        return ResponseEntity.created(uri).body(createdDto);
    }

    @Operation(summary = "Assigns a technician to a service order",
            description = "Links a technician and sets the status to IN_PROGRESS (allowed from OPEN or IN_PROGRESS)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Technician assigned"),
            @ApiResponse(responseCode = "404", description = "Service order or technician not found"),
            @ApiResponse(responseCode = "409", description = "Order status does not allow assignment")
    })
    @PutMapping(value = "/{id}/assign/{technicianId}")
    public ResponseEntity<OrderServiceDTO> assignTechnician(@PathVariable Long id, @PathVariable Long technicianId) {
        return ResponseEntity.ok(service.assignTechnician(id, technicianId));
    }

    @Operation(summary = "Finishes a service order",
            description = "Stores the root cause report and sets the status to FINISHED (allowed only from IN_PROGRESS)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service order finished"),
            @ApiResponse(responseCode = "400", description = "Root cause report is required"),
            @ApiResponse(responseCode = "404", description = "Service order not found"),
            @ApiResponse(responseCode = "409", description = "Order status does not allow finishing")
    })
    @PutMapping(value = "/{id}/finish")
    public ResponseEntity<OrderServiceDTO> finish(@PathVariable Long id, @Valid @RequestBody OrderServiceFinishDTO dto) {
        return ResponseEntity.ok(service.finish(id, dto));
    }

    @Operation(summary = "Cancels a service order",
            description = "Sets the status to CANCELED (allowed from OPEN or IN_PROGRESS)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service order canceled"),
            @ApiResponse(responseCode = "404", description = "Service order not found"),
            @ApiResponse(responseCode = "409", description = "Order status does not allow cancellation")
    })
    @PutMapping(value = "/{id}/cancel")
    public ResponseEntity<OrderServiceDTO> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(service.cancel(id));
    }
}