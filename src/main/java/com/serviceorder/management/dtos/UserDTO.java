package com.serviceorder.management.dtos;

import com.serviceorder.management.entities.AppUser;
import com.serviceorder.management.enums.UserRole;

public record UserDTO(Long id, String name, String email, UserRole role, boolean enabled) {

    public UserDTO(AppUser entity) {
        this(entity.getId(), entity.getName(), entity.getEmail(), entity.getRole(), entity.isEnabled());
    }
}