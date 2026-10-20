package com.classbridge.auth;

import com.classbridge.common.exception.InvalidCredentialsException;
import com.classbridge.user.User;
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


@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES=32;

    private final SecureRandom secureRandom = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;

    private final long refreshTokenExpiryDays;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${app.jwt.refresh-token-expiry-days}") long refreshTokenExpiryDays
    ){
        this.refreshTokenRepository=refreshTokenRepository;
        this.refreshTokenExpiryDays=refreshTokenExpiryDays;
    }

    public record RotatedToken(String rawToken, User user){}

    @Transactional
    public String issue(User user){
        String rawToken= generateRawToken();

        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(hash(rawToken));
        entity.setExpiresAt(Instant.now().plus(Duration.ofDays(refreshTokenExpiryDays)));
        entity.setRevoked(false);
        refreshTokenRepository.save(entity);

        return rawToken;
    }

    @Transactional(readOnly = true)
    public RefreshToken validate(String rawToken){

        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(InvalidCredentialsException::new);

        if(token.isRevoked() || token.getExpiresAt().isBefore(Instant.now())){
            throw new InvalidCredentialsException();
        }

        return token;

    }

    @Transactional
    public void revoke(String rawToken){

        refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public RotatedToken rotate(String rawToken){

        RefreshToken old = validate(rawToken);
        old.setRevoked(true);
        refreshTokenRepository.save(old);

        User user = old.getUser();
        String newRawToken = issue(user);

        return new RotatedToken(newRawToken,user);

    }

    private String generateRawToken(){
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String rawToken){

        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch( NoSuchAlgorithmException e){
            throw new IllegalStateException("SHA-256 is not available",e);
        }
    }

}
