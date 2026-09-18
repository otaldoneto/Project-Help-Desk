package com.serviceOrder.Management.repositories;

import com.serviceOrder.Management.entities.OrderService;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderServiceRepository extends JpaRepository<OrderService,Long> {
}
