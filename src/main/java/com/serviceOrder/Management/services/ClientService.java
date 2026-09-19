package com.serviceOrder.Management.services;


import com.serviceOrder.Management.dtos.ClientDTO;
import com.serviceOrder.Management.controllers.exceptions.ResourceNotFoundException;
import com.serviceOrder.Management.entities.Client;
import com.serviceOrder.Management.repositories.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        Client entity = new Client();
        entity.setName(dto.name());
        entity.setEmail(dto.email());
        entity.setCpfOrCnpj(dto.cpfOrCnpj());
        entity = repository.save(entity);
        return new ClientDTO(entity);
    }
}
