package com.serviceOrder.Management.repositories;

import com.serviceOrder.Management.entities.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {
}
