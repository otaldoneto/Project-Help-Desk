package com.serviceorder.management.services;

import com.serviceorder.management.controllers.exceptions.BusinessRuleException;
import com.serviceorder.management.controllers.exceptions.InvalidCredentialsException;
import com.serviceorder.management.controllers.exceptions.ResourceNotFoundException;
import com.serviceorder.management.dtos.ChangePasswordDTO;
import com.serviceorder.management.dtos.UserCreateDTO;
import com.serviceorder.management.dtos.UserDTO;
import com.serviceorder.management.entities.AppUser;
import com.serviceorder.management.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Transactional(readOnly = true)
    public Page<UserDTO> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserDTO::new);
    }

    @Transactional
    public UserDTO create(UserCreateDTO dto) {
        String email = dto.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new BusinessRuleException("A user with this email already exists");
        }

        AppUser user = new AppUser(null, dto.name(), email, passwordEncoder.encode(dto.password()), dto.role(), true);
        return new UserDTO(userRepository.save(user));
    }

    @Transactional
    public UserDTO setEnabled(Long id, boolean enabled) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setEnabled(enabled);
        return new UserDTO(userRepository.save(user));
    }

    @Transactional
    public void changePassword(String email, ChangePasswordDTO dto) {
        AppUser user = userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (!passwordEncoder.matches(dto.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(dto.newPassword()));
        userRepository.save(user);
    }
}