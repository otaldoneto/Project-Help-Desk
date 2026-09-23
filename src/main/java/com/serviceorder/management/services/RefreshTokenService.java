package com.serviceorder.management.services;

import com.serviceorder.management.controllers.exceptions.InvalidCredentialsException;
import com.serviceorder.management.dtos.TokenResponseDTO;
import com.serviceorder.management.entities.AppUser;
import com.serviceorder.management.entities.RefreshToken;
import com.serviceorder.management.repositories.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

// Refresh tokens are opaque random strings (not JWTs): the server looks them up in the
// database, so they do not need to be self-describing, and — unlike access tokens — they
// can be revoked (logout, rotation) because that lookup is a real, server-side check.
@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final JwtService jwtService;
    private final long refreshTokenDays;

    public RefreshTokenService(RefreshTokenRepository repository, JwtService jwtService,
                               @Value("${app.jwt.refresh-expiration-days}") long refreshTokenDays) {
        this.repository = repository;
        this.jwtService = jwtService;
        this.refreshTokenDays = refreshTokenDays;
    }

    @Transactional
    public TokenResponseDTO issueTokenPair(AppUser user) {
        String rawRefreshToken = generateRawToken();
        RefreshToken entity = new RefreshToken(null, hash(rawRefreshToken), user,
                Instant.now().plus(Duration.ofDays(refreshTokenDays)), Instant.now());
        repository.save(entity);

        String accessToken = jwtService.generateAccessToken(user);
        return new TokenResponseDTO(accessToken, "Bearer", jwtService.getExpirationSeconds(), rawRefreshToken);
    }

    // Validates the refresh token, deletes it (rotation: a used refresh token cannot be reused,
    // which makes reuse of a stolen token detectable) and issues a brand new access + refresh pair.
    @Transactional
    public TokenResponseDTO refresh(String rawRefreshToken) {
        RefreshToken entity = repository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired refresh token"));

        repository.delete(entity);

        if (entity.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidCredentialsException("Invalid or expired refresh token");
        }

        return issueTokenPair(entity.getUser());
    }

    // Idempotent: logging out with an already-invalid token is not an error
    @Transactional
    public void revoke(String rawRefreshToken) {
        repository.deleteByTokenHash(hash(rawRefreshToken));
    }

    private static String generateRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}