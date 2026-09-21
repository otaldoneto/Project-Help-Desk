package com.serviceOrder.Management.dtos;

import com.serviceOrder.Management.entities.AppUser;
import com.serviceOrder.Management.enums.UserRole;

public record UserDTO(Long id, String name, String email, UserRole role) {

    public UserDTO(AppUser entity) {
        this(entity.getId(), entity.getName(), entity.getEmail(), entity.getRole());
    }
}