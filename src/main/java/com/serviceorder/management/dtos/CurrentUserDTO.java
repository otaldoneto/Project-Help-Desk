package com.serviceorder.management.dtos;

import java.util.List;

public record CurrentUserDTO(String email, List<String> roles) {
}