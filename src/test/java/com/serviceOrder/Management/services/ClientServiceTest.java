package com.serviceOrder.Management.services;

import com.serviceOrder.Management.controllers.exceptions.BusinessRuleException;
import com.serviceOrder.Management.controllers.exceptions.ResourceNotFoundException;
import com.serviceOrder.Management.dtos.ClientDTO;
import com.serviceOrder.Management.entities.Client;
import com.serviceOrder.Management.repositories.ClientRepository;

import com.serviceOrder.Management.repositories.ServiceOrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

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

    @Mock
    private ServiceOrderRepository orderRepository;

    private ClientDTO newClient() {
        return new ClientDTO(null, "Acme Ltda", "acme@mail.com", "11222333000181");
    }

    @Test
    @DisplayName("insert should normalize email and CPF/CNPJ before saving")
    void insertShouldNormalizeAndSave() {
        ClientDTO dto = new ClientDTO(null, "Acme Ltda", "  Acme@Mail.COM ", "11.222.333/0001-81");
        when(clientRepository.existsByEmail("acme@mail.com")).thenReturn(false);
        when(clientRepository.existsByDocument("11222333000181")).thenReturn(false);
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientDTO result = clientService.insert(dto);

        assertEquals("acme@mail.com", result.email());
        assertEquals("11222333000181", result.cpfOrCnpj());
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
        when(clientRepository.existsByDocument("11222333000181")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> clientService.insert(newClient()));
        verify(clientRepository, never()).save(any());
    }

    @Test
    @DisplayName("findById should throw ResourceNotFoundException when client does not exist")
    void findByIdShouldThrowWhenNotFound() {
        when(clientRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> clientService.findById(999L));
    }

    @Test
    @DisplayName("findAll should return a page of DTOs")
    void findAllShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Client client = new Client(1L, "Acme Ltda", "acme@mail.com", "11222333000181");
        when(clientRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(client), pageable, 1));

        Page<ClientDTO> result = clientService.findAll(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Acme Ltda", result.getContent().get(0).name());
    }

    @Test
    @DisplayName("update should change the data and normalize email and CPF/CNPJ")
    void updateShouldChangeAndNormalize() {
        Client existing = new Client(1L, "Acme Ltda", "acme@mail.com", "11222333000181");
        ClientDTO dto = new ClientDTO(null, "Acme Corp", "Corp@Mail.COM", "45.723.174/0001-10");
        when(clientRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(clientRepository.existsByEmailAndIdNot("corp@mail.com", 1L)).thenReturn(false);
        when(clientRepository.existsByDocumentAndIdNot("45723174000110", 1L)).thenReturn(false);
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientDTO result = clientService.update(1L, dto);

        assertEquals("Acme Corp", result.name());
        assertEquals("corp@mail.com", result.email());
        assertEquals("45723174000110", result.cpfOrCnpj());
    }

    @Test
    @DisplayName("update should throw BusinessRuleException when the email belongs to another client")
    void updateShouldThrowWhenEmailBelongsToAnotherClient() {
        Client existing = new Client(1L, "Acme Ltda", "acme@mail.com", "11222333000181");
        when(clientRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(clientRepository.existsByEmailAndIdNot("other@mail.com", 1L)).thenReturn(true);

        ClientDTO dto = new ClientDTO(null, "Acme Ltda", "other@mail.com", "11222333000181");

        assertThrows(BusinessRuleException.class, () -> clientService.update(1L, dto));
        verify(clientRepository, never()).save(any());
    }

    @Test
    @DisplayName("update should throw ResourceNotFoundException when the client does not exist")
    void updateShouldThrowWhenNotFound() {
        when(clientRepository.findById(999L)).thenReturn(Optional.empty());

        ClientDTO dto = new ClientDTO(null, "Acme Ltda", "acme@mail.com", "11222333000181");

        assertThrows(ResourceNotFoundException.class, () -> clientService.update(999L, dto));
        verify(clientRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete should remove a client without service orders")
    void deleteShouldRemoveClient() {
        when(clientRepository.existsById(1L)).thenReturn(true);
        when(orderRepository.existsByClientId(1L)).thenReturn(false);

        clientService.delete(1L);

        verify(clientRepository).deleteById(1L);
    }

    @Test
    @DisplayName("delete should throw BusinessRuleException when the client has service orders")
    void deleteShouldThrowWhenClientHasOrders() {
        when(clientRepository.existsById(1L)).thenReturn(true);
        when(orderRepository.existsByClientId(1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> clientService.delete(1L));
        verify(clientRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("delete should throw ResourceNotFoundException when the client does not exist")
    void deleteShouldThrowWhenNotFound() {
        when(clientRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> clientService.delete(999L));
        verify(clientRepository, never()).deleteById(any());
    }
}