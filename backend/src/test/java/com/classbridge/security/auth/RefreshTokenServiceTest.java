package com.classbridge.security.auth;

import com.classbridge.auth.RefreshToken;
import com.classbridge.auth.RefreshTokenRepository;
import com.classbridge.auth.RefreshTokenService;
import com.classbridge.auth.SignupService;
import com.classbridge.auth.dto.SignupRequest;
import com.classbridge.common.exception.InvalidCredentialsException;
import com.classbridge.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class RefreshTokenServiceTest {

    @Autowired
    private SignupService signupService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;


    private User createTestUser(){
        String suffix = String.valueOf(System.currentTimeMillis());
        return signupService.registerOrgAdmin(new SignupRequest(
                "Token Org " + suffix,
                "Token User",
                "token-" + suffix + "@gmail.edu",
                "Password@!123"
        ));
    }

    @Test
    void issueThenValidate_returnsSameUser_andStoresOnlyTheHash(){

        User user = createTestUser();

        String rawToken = refreshTokenService.issue(user);
        RefreshToken validated = refreshTokenService.validate(rawToken);

        assertEquals(user.getId(),validated.getUser().getId());

        RefreshToken stored = refreshTokenRepository
                .findByTokenHash(RefreshTokenService.hash(rawToken))
                .orElseThrow();
        assertNotEquals(rawToken,stored.getTokenHash());

    }

    @Test
    void validate_withExpiredToken_throws(){

        User user = createTestUser();
        String rawToken = refreshTokenService.issue(user);

        RefreshToken stored = refreshTokenRepository
                .findByTokenHash(RefreshTokenService.hash(rawToken))
                .orElseThrow();
        stored.setExpiresAt(Instant.now().minusSeconds(60));
        refreshTokenRepository.save(stored);

        assertThrows(InvalidCredentialsException.class,
                () -> refreshTokenService.validate(rawToken));

    }

    @Test
    void revoke_thenValidate_throws(){

        User user = createTestUser();
        String rawToken = refreshTokenService.issue(user);

        refreshTokenService.revoke(rawToken);

        assertThrows(InvalidCredentialsException.class,
                ()-> refreshTokenService.validate(rawToken));
    }

    @Test
    void validate_withUnknownToken_throws(){

        assertThrows(InvalidCredentialsException.class,
                ()-> refreshTokenService.validate("never-used"));
    }

    @Test
    void rotate_revokesOldToken_andIssuesNewOne(){

        User user = createTestUser();
        String oldToken = refreshTokenService.issue(user);

        RefreshTokenService.RotatedToken rotated = refreshTokenService.rotate(oldToken);

        assertThrows(
                InvalidCredentialsException.class,
                () -> refreshTokenService.validate(oldToken)
        );

        assertNotNull(
                refreshTokenService.validate(rotated.rawToken())
        );

        assertNotEquals(oldToken, rotated.rawToken());
        assertEquals(user.getId(), rotated.user().getId());
    }
}
