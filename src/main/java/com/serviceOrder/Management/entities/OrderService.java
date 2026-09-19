package com.serviceOrder.Management.entities;

import com.serviceOrder.Management.enums.OrderPriority;
import com.serviceOrder.Management.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "tb_order_service")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OrderService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    private OrderPriority priority;

    private Instant createdAt;
    private Instant finishedAt;

    @Column(columnDefinition = "TEXT")
    private String rootCauseReport;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne
    @JoinColumn(name = "technician_id")
    private Technician technician;

    public OrderService(Long id, String title, String description, OrderPriority priority, Client client) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.client = client;
        this.status = OrderStatus.OPEN;
        this.createdAt = Instant.now();
    }
}