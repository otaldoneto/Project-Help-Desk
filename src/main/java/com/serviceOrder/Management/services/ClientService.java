package com.serviceOrder.Management.services;

import com.serviceOrder.Management.controllers.exceptions.BusinessRuleException;
import com.serviceOrder.Management.controllers.exceptions.ResourceNotFoundException;
import com.serviceOrder.Management.dtos.ClientDTO;
import com.serviceOrder.Management.entities.Client;
import com.serviceOrder.Management.repositories.ClientRepository;
import com.serviceOrder.Management.repositories.ServiceOrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class ClientService {
    private final ClientRepository repository;
    private final ServiceOrderRepository orderRepository;

    public ClientService(ClientRepository repository, ServiceOrderRepository orderRepository) {
        this.repository = repository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public Page<ClientDTO> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(ClientDTO::new);
    }

    @Transactional(readOnly = true)
    public ClientDTO findById(Long id) {
        Client entity = findClient(id);
        return new ClientDTO(entity);
    }

    @Transactional
    public ClientDTO insert(ClientDTO dto) {
        String email = normalizeEmail(dto.email());
        String cpfOrCnpj = normalizeDocument(dto.cpfOrCnpj());

        if (repository.existsByEmail(email)) {
            throw new BusinessRuleException("A client with this email already exists");
        }
        if (repository.existsByDocument(cpfOrCnpj)) {
            throw new BusinessRuleException("A client with this CPF/CNPJ already exists");
        }

        Client entity = new Client();
        entity.setName(dto.name());
        entity.setEmail(email);
        entity.setCpfOrCnpj(cpfOrCnpj);
        entity = repository.save(entity);
        return new ClientDTO(entity);
    }

    @Transactional
    public ClientDTO update(Long id, ClientDTO dto) {
        Client entity = findClient(id);
        String email = normalizeEmail(dto.email());
        String cpfOrCnpj = normalizeDocument(dto.cpfOrCnpj());

        if (repository.existsByEmailAndIdNot(email, id)) {
            throw new BusinessRuleException("A client with this email already exists");
        }
        if (repository.existsByDocumentAndIdNot(cpfOrCnpj, id)) {
            throw new BusinessRuleException("A client with this CPF/CNPJ already exists");
        }

        entity.setName(dto.name());
        entity.setEmail(email);
        entity.setCpfOrCnpj(cpfOrCnpj);
        entity = repository.save(entity);
        return new ClientDTO(entity);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Client not found with id: " + id);
        }
        if (orderRepository.existsByClientId(id)) {
            throw new BusinessRuleException("Cannot delete a client that has service orders");
        }
        repository.deleteById(id);
    }

    private Client findClient(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    // Remove separators but keep letters: CNPJ can be alphanumeric.
    private static String normalizeDocument(String document) {
        return document.replaceAll("[.\\-/\\s]", "").toUpperCase(Locale.ROOT);
    }
}