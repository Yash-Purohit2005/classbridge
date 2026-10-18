package com.classbridge.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenExpiryMinutes;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiry-minutes}") long accessTokenExpiryMinutes
    ) {

        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpiryMinutes = accessTokenExpiryMinutes;
    }

    public String generateToken(Long userId,Long orgId,String role){
        Instant now = Instant.now();
        Instant expiry = now.plus(Duration.ofMinutes(accessTokenExpiryMinutes));

        return Jwts.builder()
                .subject(userId.toString())
                .claim("orgId",orgId)
                .claim("role",role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseToken(String token){
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long extractUserId(Claims claims){
        return Long.valueOf(claims.getSubject());
    }

    public Long extractOrgId(Claims claims){
        return claims.get("orgId", Long.class);
    }

    public String extractRole(Claims claims) {
        return claims.get("role", String.class);
    }

}
