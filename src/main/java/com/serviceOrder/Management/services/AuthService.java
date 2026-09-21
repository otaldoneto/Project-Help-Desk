package com.serviceOrder.Management.services;

import com.serviceOrder.Management.controllers.exceptions.InvalidCredentialsException;
import com.serviceOrder.Management.dtos.LoginRequestDTO;
import com.serviceOrder.Management.dtos.TokenResponseDTO;
import com.serviceOrder.Management.entities.AppUser;
import com.serviceOrder.Management.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public TokenResponseDTO login(LoginRequestDTO dto) {
        String email = dto.email().trim().toLowerCase(Locale.ROOT);

        // Same message for "unknown email" and "wrong password", so the API does not reveal which emails exist
        AppUser user = userRepository.findByEmail(email)
                .filter(candidate -> passwordEncoder.matches(dto.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        return jwtService.generateToken(user);
    }
}