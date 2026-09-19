package com.serviceOrder.Management.services;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.serviceOrder.Management.dtos.OrderServiceDTO;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class PdfReportService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    public byte[] generateOrderReport(OrderServiceDTO order) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
            Paragraph title = new Paragraph("SERVICE ORDER REPORT", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            document.add(new Paragraph("Service Order #" + order.id(), boldFont));
            document.add(new Paragraph("Status: " + order.status()));
            document.add(new Paragraph("Priority: " + order.priority()));
            document.add(new Paragraph("Created at: " + format(order.createdAt())));
            if (order.finishedAt() != null) {
                document.add(new Paragraph("Finished at: " + format(order.finishedAt())));
            }
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.addCell(headerCell("Client", boldFont));
            table.addCell(headerCell("Assigned Technician", boldFont));
            table.addCell(order.client() != null ? order.client().name() : "Not informed");
            table.addCell(order.technician() != null ? order.technician().name() : "Not assigned");
            document.add(table);
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Problem Description:", boldFont));
            document.add(new Paragraph(order.description()));

            if (order.rootCauseReport() != null) {
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Technical Report / Resolution:", boldFont));
                document.add(new Paragraph(order.rootCauseReport()));
            }

            document.close();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate the service order PDF", e);
        }
        return out.toByteArray();
    }

    private PdfPCell headerCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(Color.LIGHT_GRAY);
        return cell;
    }

    private String format(Instant instant) {
        return instant != null ? DATE_FORMAT.format(instant) : "-";
    }
}