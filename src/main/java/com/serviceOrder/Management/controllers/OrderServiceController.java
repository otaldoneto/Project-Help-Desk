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

@Tag(name = "Ordens de Serviço", description = "Endpoints para gerenciamento do ciclo de vida das OS")
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

    @Operation(summary = "Cria uma nova ordem de serviço", description = "Registra uma nova OS associada a um cliente")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ordem de serviço criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Client não encontrado")
    })
    @PostMapping
    public ResponseEntity<OrderServiceDTO> create(@Valid @RequestBody OrderServiceCreateDTO dto) {
        OrderServiceDTO createdDto = service.create(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(createdDto.id()).toUri();
        return ResponseEntity.created(uri).body(createdDto);
    }

    @Operation(summary = "Atribui um técnico á ordem de serviço",
            description = "Vincula um técnico e altera o status para IN_PROGRESS")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Técnico atribuído com sucesso"),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço ou Técnico não encontrado")
    })
    @PutMapping(value = "/{id}/assign/{technicianId}")
    public ResponseEntity<OrderServiceDTO> assignTechnician(@PathVariable Long id, @PathVariable Long technicianId) {
        OrderServiceDTO dto = service.assignTechnician(id, technicianId);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Finaliza uma ordem de serviço", description = "Registra o laudo técnico e altera o status para FINISHED")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ordem de serviço finalizada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada"),
            @ApiResponse(responseCode = "422", description = "Erro de validação (laudo técnico obrigatório)"),
    })
    @PutMapping(value = "/{id}/finish")
    public ResponseEntity<OrderServiceDTO> finish(@PathVariable Long id, @Valid @RequestBody OrderServiceFinishDTO dto) {
        OrderServiceDTO finishedDto = service.finish(id, dto);
        return ResponseEntity.ok(finishedDto);
    }

}
