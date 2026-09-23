package com.serviceorder.management.controllers;

import com.serviceorder.management.services.PdfReportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.serviceorder.management.dtos.ServiceOrderCreateDTO;
import com.serviceorder.management.dtos.ServiceOrderDTO;
import com.serviceorder.management.dtos.ServiceOrderFinishDTO;
import com.serviceorder.management.services.ServiceOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.serviceorder.management.dtos.PageResponseDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import com.serviceorder.management.dtos.OrderFilterDTO;
import com.serviceorder.management.enums.OrderPriority;
import com.serviceorder.management.enums.OrderStatus;

import java.net.URI;

@Tag(name = "Service Orders", description = "Endpoints to manage the service order lifecycle")
@RestController
@RequestMapping(value = "/orders")
public class ServiceOrderController {
    private final ServiceOrderService service;
    private final PdfReportService pdfReportService;

    public ServiceOrderController(ServiceOrderService service, PdfReportService pdfReportService) {
        this.service = service;
        this.pdfReportService = pdfReportService;
    }

    @Operation(summary = "Lists service orders",
            description = "Paginated list, newest first. The filters are optional and combined with AND: status, priority, clientId, technicianId and title (case-insensitive, matches part of the title)")
    @GetMapping
    public ResponseEntity<PageResponseDTO<ServiceOrderDTO>> findAll(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) OrderPriority priority,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long technicianId,
            @RequestParam(required = false) String title,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        OrderFilterDTO filter = new OrderFilterDTO(status, priority, clientId, technicianId, title);
        return ResponseEntity.ok(PageResponseDTO.from(service.findAll(filter, pageable)));
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<ServiceOrderDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @Operation(summary = "Creates a service order", description = "Registers a new service order for a client")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Service order created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Client not found")
    })
    @PostMapping
    public ResponseEntity<ServiceOrderDTO> create(@Valid @RequestBody ServiceOrderCreateDTO dto) {
        ServiceOrderDTO createdDto = service.create(dto);
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
    public ResponseEntity<ServiceOrderDTO> assignTechnician(@PathVariable Long id, @PathVariable Long technicianId) {
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
    public ResponseEntity<ServiceOrderDTO> finish(@PathVariable Long id, @Valid @RequestBody ServiceOrderFinishDTO dto) {
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
    public ResponseEntity<ServiceOrderDTO> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(service.cancel(id));
    }

    @Operation(summary = "Generates the PDF report of a service order")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF generated"),
            @ApiResponse(responseCode = "404", description = "Service order not found")
    })
    @GetMapping(value = "/{id}/report")
    public ResponseEntity<byte[]> generateReport(@PathVariable Long id) {
        byte[] pdf = pdfReportService.generateOrderReport(service.findById(id));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("service-order-" + id + ".pdf").build().toString())
                .body(pdf);
    }
}