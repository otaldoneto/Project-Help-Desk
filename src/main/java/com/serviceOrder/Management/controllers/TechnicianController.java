package com.serviceOrder.Management.controllers;

import com.serviceOrder.Management.dtos.TechnicianDTO;
import com.serviceOrder.Management.services.TechnicianService;
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

@Tag(name = "Technicians", description = "Endpoints to register and query technicians")
@RestController
@RequestMapping(value = "/technicians")
public class TechnicianController {
    private final TechnicianService service;

    public TechnicianController(TechnicianService service) {
        this.service = service;
    }

    @Operation(summary = "Lists all technicians")
    @GetMapping
    public ResponseEntity<List<TechnicianDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @Operation(summary = "Finds a technician by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Technician found"),
            @ApiResponse(responseCode = "400", description = "Invalid id"),
            @ApiResponse(responseCode = "404", description = "Technician not found")
    })
    @GetMapping(value = "/{id}")
    public ResponseEntity<TechnicianDTO> findById(@PathVariable Long id) {
        TechnicianDTO dto = service.findById(id);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Creates a technician",
            description = "The email must be unique and is stored in lower case")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Technician created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "409", description = "Email already registered")
    })
    @PostMapping
    public ResponseEntity<TechnicianDTO> insert(@Valid @RequestBody TechnicianDTO dto) {
        dto = service.insert(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(uri).body(dto);
    }
}