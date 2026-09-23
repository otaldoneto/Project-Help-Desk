package com.serviceorder.management.repositories;

import com.serviceorder.management.dtos.OrderFilterDTO;
import com.serviceorder.management.entities.ServiceOrder;
import com.serviceorder.management.enums.OrderPriority;
import com.serviceorder.management.enums.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Locale;

public final class ServiceOrderSpecifications {

    private ServiceOrderSpecifications() {
    }

    // Combines the filters that were sent with AND; the ones that were not sent do not restrict anything
    public static Specification<ServiceOrder> from(OrderFilterDTO filter) {
        return Specification.allOf(List.of(
                hasStatus(filter.status()),
                hasPriority(filter.priority()),
                belongsToClient(filter.clientId()),
                assignedTo(filter.technicianId()),
                titleContains(filter.title())
        ));
    }

    private static Specification<ServiceOrder> hasStatus(OrderStatus status) {
        if (status == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private static Specification<ServiceOrder> hasPriority(OrderPriority priority) {
        if (priority == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    private static Specification<ServiceOrder> belongsToClient(Long clientId) {
        if (clientId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("client").get("id"), clientId);
    }

    private static Specification<ServiceOrder> assignedTo(Long technicianId) {
        if (technicianId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("technician").get("id"), technicianId);
    }

    private static Specification<ServiceOrder> titleContains(String title) {
        if (title == null || title.isBlank()) {
            return Specification.unrestricted();
        }
        String pattern = "%" + escapeLike(title.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.<String>get("title")), pattern, '\\');
    }

    // In a LIKE, % and _ are wildcards. Escaping them makes the user's text be searched literally.
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}