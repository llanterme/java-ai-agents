package za.co.digitalcowboy.agents.domain.auth;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    long expiresIn
) {}