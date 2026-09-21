package com.serviceOrder.Management.services;

import com.serviceOrder.Management.controllers.exceptions.BusinessRuleException;
import com.serviceOrder.Management.controllers.exceptions.ResourceNotFoundException;
import com.serviceOrder.Management.dtos.TechnicianDTO;
import com.serviceOrder.Management.entities.Technician;
import com.serviceOrder.Management.repositories.OrderServiceRepository;
import com.serviceOrder.Management.repositories.TechnicianRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TechnicianServiceTest {

    @InjectMocks
    private TechnicianService technicianService;

    @Mock
    private TechnicianRepository technicianRepository;

    @Mock
    private OrderServiceRepository orderRepository;

    @Test
    @DisplayName("insert should normalize email before saving")
    void insertShouldNormalizeAndSave() {
        TechnicianDTO dto = new TechnicianDTO(null, "Carlos Silva", "  Carlos@Mail.COM ", "Networking");
        when(technicianRepository.existsByEmail("carlos@mail.com")).thenReturn(false);
        when(technicianRepository.save(any(Technician.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TechnicianDTO result = technicianService.insert(dto);

        assertEquals("carlos@mail.com", result.email());
    }

    @Test
    @DisplayName("insert should throw BusinessRuleException when email already exists")
    void insertShouldThrowWhenEmailExists() {
        TechnicianDTO dto = new TechnicianDTO(null, "Carlos Silva", "carlos@mail.com", "Networking");
        when(technicianRepository.existsByEmail("carlos@mail.com")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> technicianService.insert(dto));
        verify(technicianRepository, never()).save(any());
    }

    @Test
    @DisplayName("findById should throw ResourceNotFoundException when technician does not exist")
    void findByIdShouldThrowWhenNotFound() {
        when(technicianRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> technicianService.findById(999L));
    }

    @Test
    @DisplayName("findAll should return a page of DTOs")
    void findAllShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Technician technician = new Technician(1L, "Carlos Silva", "carlos@mail.com", "Networking");
        when(technicianRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(technician), pageable, 1));

        Page<TechnicianDTO> result = technicianService.findAll(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Carlos Silva", result.getContent().get(0).name());
    }

    @Test
    @DisplayName("update should change the data and normalize the email")
    void updateShouldChangeAndNormalize() {
        Technician existing = new Technician(1L, "Carlos Silva", "carlos@mail.com", "Networking");
        TechnicianDTO dto = new TechnicianDTO(null, "Carlos S.", "Carlos.Silva@Mail.COM", "Hardware");
        when(technicianRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(technicianRepository.existsByEmailAndIdNot("carlos.silva@mail.com", 1L)).thenReturn(false);
        when(technicianRepository.save(any(Technician.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TechnicianDTO result = technicianService.update(1L, dto);

        assertEquals("Carlos S.", result.name());
        assertEquals("carlos.silva@mail.com", result.email());
        assertEquals("Hardware", result.specialty());
    }

    @Test
    @DisplayName("update should throw BusinessRuleException when the email belongs to another technician")
    void updateShouldThrowWhenEmailBelongsToAnotherTechnician() {
        Technician existing = new Technician(1L, "Carlos Silva", "carlos@mail.com", "Networking");
        when(technicianRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(technicianRepository.existsByEmailAndIdNot("other@mail.com", 1L)).thenReturn(true);

        TechnicianDTO dto = new TechnicianDTO(null, "Carlos Silva", "other@mail.com", "Networking");

        assertThrows(BusinessRuleException.class, () -> technicianService.update(1L, dto));
        verify(technicianRepository, never()).save(any());
    }

    @Test
    @DisplayName("update should throw ResourceNotFoundException when the technician does not exist")
    void updateShouldThrowWhenNotFound() {
        when(technicianRepository.findById(999L)).thenReturn(Optional.empty());

        TechnicianDTO dto = new TechnicianDTO(null, "Carlos Silva", "carlos@mail.com", "Networking");

        assertThrows(ResourceNotFoundException.class, () -> technicianService.update(999L, dto));
        verify(technicianRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete should remove a technician without service orders")
    void deleteShouldRemoveTechnician() {
        when(technicianRepository.existsById(1L)).thenReturn(true);
        when(orderRepository.existsByTechnicianId(1L)).thenReturn(false);

        technicianService.delete(1L);

        verify(technicianRepository).deleteById(1L);
    }

    @Test
    @DisplayName("delete should throw BusinessRuleException when the technician has service orders")
    void deleteShouldThrowWhenTechnicianHasOrders() {
        when(technicianRepository.existsById(1L)).thenReturn(true);
        when(orderRepository.existsByTechnicianId(1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> technicianService.delete(1L));
        verify(technicianRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("delete should throw ResourceNotFoundException when the technician does not exist")
    void deleteShouldThrowWhenNotFound() {
        when(technicianRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> technicianService.delete(999L));
        verify(technicianRepository, never()).deleteById(any());
    }
}