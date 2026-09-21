package com.serviceOrder.Management.services;

import com.serviceOrder.Management.controllers.exceptions.BusinessRuleException;
import com.serviceOrder.Management.controllers.exceptions.ResourceNotFoundException;
import com.serviceOrder.Management.dtos.TechnicianDTO;
import com.serviceOrder.Management.entities.Technician;
import com.serviceOrder.Management.repositories.TechnicianRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class TechnicianService {
    private final TechnicianRepository repository;

    public TechnicianService(TechnicianRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<TechnicianDTO> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(TechnicianDTO::new);
    }

    @Transactional(readOnly = true)
    public TechnicianDTO findById(Long id) {
        Technician entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found with id: " + id));
        return new TechnicianDTO(entity);
    }

    @Transactional
    public TechnicianDTO insert(TechnicianDTO dto) {
        String email = dto.email().trim().toLowerCase(Locale.ROOT);

        if (repository.existsByEmail(email)) {
            throw new BusinessRuleException("A technician with this email already exists");
        }

        Technician entity = new Technician();
        entity.setName(dto.name());
        entity.setEmail(email);
        entity.setSpecialty(dto.specialty());
        entity = repository.save(entity);
        return new TechnicianDTO(entity);
    }
}