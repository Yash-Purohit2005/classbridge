package com.classbridge.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "test-only-secret-key-not-for-real-use-must-be-long-enough-1234567890",
            15L);

    @Test
    void generateThenParse_returnsTheSameClaims(){
        String token = jwtService.generateToken(42L,7L,"ADMIN");

        Claims claims = jwtService.parseToken(token);

        assertEquals(42L,jwtService.extractUserId(claims));
        assertEquals(7L,jwtService.extractOrgId(claims));
        assertEquals("ADMIN",jwtService.extractRole(claims));



    }
}
