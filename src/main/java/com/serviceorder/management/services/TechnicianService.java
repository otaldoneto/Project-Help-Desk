package com.serviceorder.management.services;

import com.serviceorder.management.controllers.exceptions.BusinessRuleException;
import com.serviceorder.management.controllers.exceptions.ResourceNotFoundException;
import com.serviceorder.management.dtos.TechnicianDTO;
import com.serviceorder.management.entities.Technician;
import com.serviceorder.management.repositories.ServiceOrderRepository;
import com.serviceorder.management.repositories.TechnicianRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class TechnicianService {
    private final TechnicianRepository repository;
    private final ServiceOrderRepository orderRepository;

    public TechnicianService(TechnicianRepository repository, ServiceOrderRepository orderRepository) {
        this.repository = repository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public Page<TechnicianDTO> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(TechnicianDTO::new);
    }

    @Transactional(readOnly = true)
    public TechnicianDTO findById(Long id) {
        Technician entity = findTechnician(id);
        return new TechnicianDTO(entity);
    }

    @Transactional
    public TechnicianDTO insert(TechnicianDTO dto) {
        String email = normalizeEmail(dto.email());

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

    @Transactional
    public TechnicianDTO update(Long id, TechnicianDTO dto) {
        Technician entity = findTechnician(id);
        String email = normalizeEmail(dto.email());

        if (repository.existsByEmailAndIdNot(email, id)) {
            throw new BusinessRuleException("A technician with this email already exists");
        }

        entity.setName(dto.name());
        entity.setEmail(email);
        entity.setSpecialty(dto.specialty());
        entity = repository.save(entity);
        return new TechnicianDTO(entity);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Technician not found with id: " + id);
        }
        if (orderRepository.existsByTechnicianId(id)) {
            throw new BusinessRuleException("Cannot delete a technician that has service orders");
        }
        repository.deleteById(id);
    }

    private Technician findTechnician(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found with id: " + id));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}