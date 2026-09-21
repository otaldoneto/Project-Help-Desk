package com.serviceOrder.Management.controllers;

import com.serviceOrder.Management.dtos.TechnicianDTO;
import com.serviceOrder.Management.services.TechnicianService;
import com.serviceOrder.Management.dtos.PageResponseDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Tag(name = "Technicians", description = "Endpoints to register and query technicians")
@RestController
@RequestMapping(value = "/technicians")
public class TechnicianController {
    private final TechnicianService service;

    public TechnicianController(TechnicianService service) {
        this.service = service;
    }

    @Operation(summary = "Lists technicians",
            description = "Paginated list ordered by name. Use the page, size and sort query parameters")
    @GetMapping
    public ResponseEntity<PageResponseDTO<TechnicianDTO>> findAll(
            @PageableDefault(size = 20, sort = {"name", "id"}) Pageable pageable) {
        return ResponseEntity.ok(PageResponseDTO.from(service.findAll(pageable)));
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

    @Operation(summary = "Updates a technician",
            description = "Replaces all the fields. The email must stay unique across technicians")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Technician updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Technician not found"),
            @ApiResponse(responseCode = "409", description = "Email already registered")
    })
    @PutMapping(value = "/{id}")
    public ResponseEntity<TechnicianDTO> update(@PathVariable Long id, @Valid @RequestBody TechnicianDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @Operation(summary = "Deletes a technician", description = "A technician that has service orders cannot be deleted")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Technician deleted"),
            @ApiResponse(responseCode = "404", description = "Technician not found"),
            @ApiResponse(responseCode = "409", description = "Technician has service orders")
    })
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}