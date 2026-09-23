package com.serviceOrder.Management.dtos;

public record TokenResponseDTO(String accessToken, String tokenType, long expiresIn, String refreshToken) {
}