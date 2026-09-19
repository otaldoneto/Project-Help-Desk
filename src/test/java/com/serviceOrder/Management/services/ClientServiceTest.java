package com.serviceOrder.Management.services;

import com.serviceOrder.Management.controllers.exceptions.BusinessRuleException;
import com.serviceOrder.Management.controllers.exceptions.ResourceNotFoundException;
import com.serviceOrder.Management.dtos.ClientDTO;
import com.serviceOrder.Management.entities.Client;
import com.serviceOrder.Management.repositories.ClientRepository;

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
class ClientServiceTest {

    @InjectMocks
    private ClientService clientService;

    @Mock
    private ClientRepository clientRepository;

    private ClientDTO newClient() {
        return new ClientDTO(null, "Acme Ltda", "acme@mail.com", "12345678000199");
    }

    @Test
    @DisplayName("insert should normalize email and CPF/CNPJ before saving")
    void insertShouldNormalizeAndSave() {
        ClientDTO dto = new ClientDTO(null, "Acme Ltda", "  Acme@Mail.COM ", "12.345.678/0001-99");
        when(clientRepository.existsByEmail("acme@mail.com")).thenReturn(false);
        when(clientRepository.existsByDocument("12345678000199")).thenReturn(false);
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientDTO result = clientService.insert(dto);

        assertEquals("acme@mail.com", result.email());
        assertEquals("12345678000199", result.cpfOrCnpj());
    }

    @Test
    @DisplayName("insert should throw BusinessRuleException when email already exists")
    void insertShouldThrowWhenEmailExists() {
        when(clientRepository.existsByEmail("acme@mail.com")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> clientService.insert(newClient()));
        verify(clientRepository, never()).save(any());
    }

    @Test
    @DisplayName("insert should throw BusinessRuleException when CPF/CNPJ already exists")
    void insertShouldThrowWhenDocumentExists() {
        when(clientRepository.existsByEmail("acme@mail.com")).thenReturn(false);
        when(clientRepository.existsByDocument("12345678000199")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> clientService.insert(newClient()));
        verify(clientRepository, never()).save(any());
    }

    @Test
    @DisplayName("findById should throw ResourceNotFoundException when client does not exist")
    void findByIdShouldThrowWhenNotFound() {
        when(clientRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> clientService.findById(999L));
    }
}