package com.btvn.serviceprojectfinal.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    // ← Thay TokenBlacklistRepository bằng TokenBlacklistService
    private final TokenBlacklistService tokenBlacklistService;

    // ===== GENERATE =====
    public String generateAccessToken(String email, String role) {
        return buildToken(email, role, "ACCESS", accessTokenExpiration);
    }

    public String generateRefreshToken(String email, String role) {
        return buildToken(email, role, "REFRESH", refreshTokenExpiration);
    }

    private String buildToken(String email, String role,
                              String tokenType, long expiration) {
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .claim("tokenType", tokenType)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    // ===== EXTRACT =====
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public String extractTokenType(String token) {
        return parseClaims(token).get("tokenType", String.class);
    }

    public Date extractExpiration(String token) {
        return parseClaims(token).getExpiration();
    }

    // ===== VALIDATE =====
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            // ← Dùng Redis thay DB
            return !tokenBlacklistService.isBlacklisted(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // ===== PRIVATE =====
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64URL.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}