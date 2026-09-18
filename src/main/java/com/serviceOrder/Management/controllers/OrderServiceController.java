package com.serviceOrder.Management.controllers;

import com.serviceOrder.Management.dtos.OrderServiceDTO;
import com.serviceOrder.Management.dtos.OrderServiceFinishDTO;
import com.serviceOrder.Management.services.OrderServiceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

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

    @PostMapping
    public ResponseEntity<OrderServiceDTO> create(@Valid @RequestBody OrderServiceDTO dto) {
        OrderServiceDTO createdDTO = service.create(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(createdDTO.id()).toUri();
        return ResponseEntity.created(uri).body(createdDTO);
    }

    @PutMapping(value = "/{id}/assign/{technicianId}")
    public ResponseEntity<OrderServiceDTO> assignTechnician(@PathVariable Long id, @PathVariable Long technicianId) {
        OrderServiceDTO dto = service.assignTechnician(id, technicianId);
        return ResponseEntity.ok(dto);
    }

    @PutMapping(value = "{id}/finish")
    public ResponseEntity<OrderServiceDTO> finish(@PathVariable Long id, @Valid @RequestBody OrderServiceFinishDTO dto) {
        OrderServiceDTO finishedDto = service.finish(id, dto);
        return ResponseEntity.ok(finishedDto);
    }

}
