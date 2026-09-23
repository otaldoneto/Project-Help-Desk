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
import java.util.Optional;

@Service
public class AuthService {

    // A real BCrypt hash of an unused password. It exists only so that matches() always runs
    // and takes roughly the same time, whether the email is registered or not (a timing side-channel).
    private static final String DUMMY_HASH = "$2a$10$xi84FVlhFeisSJdc6v9Gfef761wBiewNd.JzTPeMWtPxfPyKZy.Ia";

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
        Optional<AppUser> user = userRepository.findByEmail(email);

        // Always call matches(), even for an email that does not exist, comparing against a fixed
        // dummy hash in that case. This keeps the response time similar in both cases, so an
        // attacker cannot tell which emails are registered just by timing the response.
        String hashToCheck = user.map(AppUser::getPasswordHash).orElse(DUMMY_HASH);
        boolean passwordMatches = passwordEncoder.matches(dto.password(), hashToCheck);

        // A disabled account gets the same generic message as a wrong password: revealing
        // "this account exists but is disabled" would be the same kind of information leak
        // the dummy-hash check above avoids.
        boolean isUsable = user.isPresent() && passwordMatches && user.get().isEnabled();
        if (!isUsable) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return jwtService.generateToken(user.get());
    }
}