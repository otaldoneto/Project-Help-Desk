package com.serviceOrder.Management.services;

import com.serviceOrder.Management.controllers.exceptions.InvalidCredentialsException;
import com.serviceOrder.Management.dtos.TokenResponseDTO;
import com.serviceOrder.Management.entities.AppUser;
import com.serviceOrder.Management.entities.RefreshToken;
import com.serviceOrder.Management.enums.UserRole;
import com.serviceOrder.Management.repositories.RefreshTokenRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository repository;

    @Mock
    private JwtService jwtService;

    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(repository, jwtService, 7L);
    }

    private AppUser sampleUser() {
        return new AppUser(1L, "Alice", "alice@mail.com", "hash", UserRole.USER, true);
    }

    @Test
    @DisplayName("issueTokenPair should save a hashed token and return the raw one, plus a fresh access token")
    void issueTokenPairShouldSaveHashedTokenAndReturnRawOne() {
        AppUser user = sampleUser();
        when(jwtService.generateAccessToken(user)).thenReturn("access-token-value");
        when(jwtService.getExpirationSeconds()).thenReturn(900L);
        when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TokenResponseDTO result = service.issueTokenPair(user);

        assertEquals("access-token-value", result.accessToken());
        assertEquals("Bearer", result.tokenType());
        assertEquals(900L, result.expiresIn());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertNotEquals(result.refreshToken(), saved.getTokenHash(), "the raw token must never equal its stored hash");
        assertEquals(user, saved.getUser());
    }

    @Test
    @DisplayName("refresh should reject a token that is not found")
    void refreshShouldRejectUnknownToken() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> service.refresh("some-raw-token"));
    }

    @Test
    @DisplayName("refresh should reject and delete an expired token")
    void refreshShouldRejectExpiredToken() {
        AppUser user = sampleUser();
        RefreshToken expired = new RefreshToken(10L, "irrelevant-hash-value", user,
                Instant.now().minusSeconds(60), Instant.now().minusSeconds(3600));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(expired));

        assertThrows(InvalidCredentialsException.class, () -> service.refresh("some-raw-token"));

        verify(repository).delete(expired);
    }

    @Test
    @DisplayName("refresh should rotate a valid token: delete the old one and issue a new pair")
    void refreshShouldRotateValidToken() {
        AppUser user = sampleUser();
        RefreshToken valid = new RefreshToken(10L, "irrelevant-hash-value", user,
                Instant.now().plusSeconds(3600), Instant.now());
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(valid));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtService.getExpirationSeconds()).thenReturn(900L);
        when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TokenResponseDTO result = service.refresh("some-raw-token");

        verify(repository).delete(valid);
        assertEquals("new-access-token", result.accessToken());
    }

    @Test
    @DisplayName("revoke should be idempotent: calling it for an unknown token is not an error")
    void revokeShouldBeIdempotent() {
        service.revoke("never-issued-token");

        verify(repository).deleteByTokenHash(any());
    }
}