package com.serviceOrder.Management.services;

import com.serviceOrder.Management.controllers.exceptions.BusinessRuleException;
import com.serviceOrder.Management.dtos.UserCreateDTO;
import com.serviceOrder.Management.dtos.UserDTO;
import com.serviceOrder.Management.entities.AppUser;
import com.serviceOrder.Management.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserDTO create(UserCreateDTO dto) {
        String email = dto.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new BusinessRuleException("A user with this email already exists");
        }

        AppUser user = new AppUser(null, dto.name(), email, passwordEncoder.encode(dto.password()), dto.role());
        return new UserDTO(userRepository.save(user));
    }
}