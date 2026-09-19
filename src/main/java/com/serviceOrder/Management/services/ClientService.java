package com.serviceOrder.Management.services;

import com.serviceOrder.Management.controllers.exceptions.BusinessRuleException;
import com.serviceOrder.Management.controllers.exceptions.ResourceNotFoundException;
import com.serviceOrder.Management.dtos.ClientDTO;
import com.serviceOrder.Management.entities.Client;
import com.serviceOrder.Management.repositories.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class ClientService {
    private final ClientRepository repository;

    public ClientService(ClientRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ClientDTO> findAll() {
        List<Client> list = repository.findAll();
        return list.stream().map(ClientDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public ClientDTO findById(Long id) {
        Client entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));
        return new ClientDTO(entity);
    }

    @Transactional
    public ClientDTO insert(ClientDTO dto) {
        String email = dto.email().trim().toLowerCase(Locale.ROOT);
        // Remove separators but keep letters: CNPJ can be alphanumeric.
        String cpfOrCnpj = dto.cpfOrCnpj().replaceAll("[.\\-/\\s]", "").toUpperCase(Locale.ROOT);

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
}