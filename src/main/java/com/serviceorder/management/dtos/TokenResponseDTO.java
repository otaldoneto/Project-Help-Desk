package com.serviceorder.management.dtos;

public record TokenResponseDTO(String accessToken, String tokenType, long expiresIn, String refreshToken) {
}