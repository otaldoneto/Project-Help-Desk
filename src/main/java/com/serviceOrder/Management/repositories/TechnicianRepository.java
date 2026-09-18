package com.serviceOrder.Management.repositories;

import com.serviceOrder.Management.entities.Technician;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TechnicianRepository extends JpaRepository<Technician,Long> {
}
