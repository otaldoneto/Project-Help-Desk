package com.serviceOrder.Management.services;

import com.serviceOrder.Management.dtos.ServiceOrderDTO;
import com.serviceOrder.Management.enums.OrderPriority;
import com.serviceOrder.Management.enums.OrderStatus;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfReportServiceTest {

    private final PdfReportService service = new PdfReportService();

    @Test
    void shouldGenerateValidPdfBytes() {
        ServiceOrderDTO order = new ServiceOrderDTO(1L, "Printer down", "Does not print",
                OrderStatus.OPEN, OrderPriority.HIGH, Instant.now(), null, null, null, null, null, null, null);

        byte[] pdf = service.generateOrderReport(order);

        assertTrue(pdf.length > 0);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }
}