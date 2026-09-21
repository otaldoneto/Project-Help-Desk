package com.serviceOrder.Management.controllers;

import com.serviceOrder.Management.dtos.ClientDTO;
import com.serviceOrder.Management.services.ClientService;
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


@Tag(name = "Clients", description = "Endpoints to register and query clients")
@RestController
@RequestMapping(value = "/clients")
public class ClientController {
    private final ClientService service;

    public ClientController(ClientService service) {
        this.service = service;
    }

    @Operation(summary = "Lists clients",
            description = "Paginated list ordered by name. Use the page, size and sort query parameters")
    @GetMapping
    public ResponseEntity<PageResponseDTO<ClientDTO>> findAll(
            @PageableDefault(size = 20, sort = {"name", "id"}) Pageable pageable) {
        return ResponseEntity.ok(PageResponseDTO.from(service.findAll(pageable)));
    }

    @Operation(summary = "Finds a client by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Client found"),
            @ApiResponse(responseCode = "400", description = "Invalid id"),
            @ApiResponse(responseCode = "404", description = "Client not found")
    })
    @GetMapping(value = "/{id}")
    public ResponseEntity<ClientDTO> findById(@PathVariable Long id) {
        ClientDTO dto = service.findById(id);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Creates a client",
            description = "Email and CPF/CNPJ must be unique. The email is stored in lower case and the CPF/CNPJ without separators")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Client created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "409", description = "Email or CPF/CNPJ already registered")
    })
    @PostMapping
    public ResponseEntity<ClientDTO> insert(@Valid @RequestBody ClientDTO dto) {
        dto = service.insert(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(uri).body(dto);
    }

    @Operation(summary = "Updates a client",
            description = "Replaces all the fields. Email and CPF/CNPJ must stay unique across clients")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Client updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Client not found"),
            @ApiResponse(responseCode = "409", description = "Email or CPF/CNPJ already registered")
    })
    @PutMapping(value = "/{id}")
    public ResponseEntity<ClientDTO> update(@PathVariable Long id, @Valid @RequestBody ClientDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @Operation(summary = "Deletes a client", description = "A client that has service orders cannot be deleted")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Client deleted"),
            @ApiResponse(responseCode = "404", description = "Client not found"),
            @ApiResponse(responseCode = "409", description = "Client has service orders")
    })
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}