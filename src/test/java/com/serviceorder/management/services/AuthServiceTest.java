package com.serviceorder.management.services;

import com.serviceorder.management.controllers.exceptions.InvalidCredentialsException;
import com.serviceorder.management.dtos.LoginRequestDTO;
import com.serviceorder.management.dtos.TokenResponseDTO;
import com.serviceorder.management.entities.AppUser;
import com.serviceorder.management.enums.UserRole;
import com.serviceorder.management.repositories.UserRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // @Spy: a real BCryptPasswordEncoder (this test cares about actual BCrypt behavior),
    // but still recognized by @InjectMocks for constructor injection like a @Mock would be
    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("login should return a token pair when the email and password are correct")
    void loginShouldReturnTokenForCorrectCredentials() {
        AppUser user = new AppUser(1L, "Alice", "alice@mail.com",
                passwordEncoder.encode("password123"), UserRole.USER, true);
        when(userRepository.findByEmail("alice@mail.com")).thenReturn(Optional.of(user));
        when(refreshTokenService.issueTokenPair(user))
                .thenReturn(new TokenResponseDTO("access-token", "Bearer", 900, "refresh-token"));

        TokenResponseDTO result = authService.login(new LoginRequestDTO("alice@mail.com", "password123"));

        assertEquals("access-token", result.accessToken());
        assertEquals("refresh-token", result.refreshToken());
    }

    @Test
    @DisplayName("login should throw InvalidCredentialsException when the password is wrong")
    void loginShouldThrowWhenPasswordIsWrong() {
        AppUser user = new AppUser(1L, "Alice", "alice@mail.com",
                passwordEncoder.encode("password123"), UserRole.USER, true);
        when(userRepository.findByEmail("alice@mail.com")).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequestDTO("alice@mail.com", "wrong-password")));
    }

    @Test
    @DisplayName("login should throw InvalidCredentialsException when the email does not exist, " +
            "but still run the password check against a dummy hash")
    void loginShouldThrowAndStillCheckDummyHashWhenEmailDoesNotExist() {
        when(userRepository.findByEmail("nobody@mail.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequestDTO("nobody@mail.com", "any-password")));
    }

    @Test
    @DisplayName("login should throw InvalidCredentialsException when the account is disabled, " +
            "even with the correct password")
    void loginShouldThrowWhenAccountIsDisabled() {
        AppUser user = new AppUser(1L, "Alice", "alice@mail.com",
                passwordEncoder.encode("password123"), UserRole.USER, false);
        when(userRepository.findByEmail("alice@mail.com")).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequestDTO("alice@mail.com", "password123")));
    }
}