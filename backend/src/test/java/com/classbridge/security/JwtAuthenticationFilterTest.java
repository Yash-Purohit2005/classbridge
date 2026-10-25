package com.classbridge.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
public class JwtAuthenticationFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void withoutAutorizationHeadersReturns401() throws Exception{
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithGarbageTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithValidTokenReturns200AndCorrectClaims() throws Exception {
        String token = jwtService.generateToken(42L, 7L, "ADMIN");

        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(42))
                .andExpect(jsonPath("$.orgId").value(7))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void meWithTamperedTokenReturns401() throws Exception {
        String token = jwtService.generateToken(42L, 7L, "ADMIN");

        int signatureStart = token.lastIndexOf('.') + 1;

        char first = token.charAt(signatureStart);
        char replacement = first == 'a' ? 'b' : 'a';

        String tamperedToken =
                token.substring(0, signatureStart)
                        + replacement
                        + token.substring(signatureStart + 1);

        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void healthEndpointWithoutTokenReturns200() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void healthEndpointWithGarbageTokenStillReturns200() throws Exception {
        mockMvc.perform(get("/api/health")
                        .header("Authorization", "Bearer garbage"))
                .andExpect(status().isOk());
    }
}
