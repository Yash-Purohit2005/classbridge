package com.classbridge.security.auth;

import com.classbridge.auth.LoginService;
import com.classbridge.auth.SignupService;
import com.classbridge.auth.dto.LoginRequest;
import com.classbridge.auth.dto.LoginResponse;
import com.classbridge.auth.dto.SignupRequest;
import com.classbridge.common.exception.InvalidCredentialsException;
import com.classbridge.security.JwtService;
import com.classbridge.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class LoginServiceTest {

    @Autowired
   private SignupService signupService;

    @Autowired
   private LoginService loginService;

    @Autowired
   private JwtService jwtService;

    private static final String PASSWORD = "Password@!123";

    private String registerTestUser() {
        String suffix = String.valueOf(System.currentTimeMillis());
        String email = "user-" + suffix + "@gmail.edu";
        signupService.registerOrgAdmin(
                new SignupRequest("Test Org " + suffix, "Test User", email, PASSWORD));
        return email;
    }

    @Test
    void login_withCorrectCredentials_returnsTokenWithCorrectClaims(){

        String suffix = String.valueOf(System.currentTimeMillis());
        String userEmail = "admin-"+ suffix + "@gmail.edu";
        String name = "Rohan Paliwal";
        String orgName = "Rohan Paliwal- " + suffix;

        SignupRequest signupRequest = new SignupRequest(
                orgName,
                name,
                userEmail,
                PASSWORD
        );

        User saved = signupService.registerOrgAdmin(signupRequest);

        LoginRequest loginRequest = new LoginRequest(
                userEmail,
                PASSWORD
        );

        LoginResponse loginResponse = loginService.loginWithCredentials(loginRequest);

        assertNotNull(loginResponse.accessToken());

        var claims = jwtService.parseToken(loginResponse.accessToken());

        assertEquals(saved.getId(),jwtService.extractUserId(claims));
        assertEquals(saved.getOrganization().getId(),jwtService.extractOrgId(claims));
        assertEquals(saved.getRole().name(),jwtService.extractRole(claims));

    }



    @Test
    void login_withWrongPassword_throwsInvalidCredentials() {
        String email = registerTestUser();
        LoginRequest request = new LoginRequest(email, "WrongPassword999!");

        assertThrows(InvalidCredentialsException.class,
                () -> loginService.loginWithCredentials(request));
    }

    @Test
    void login_withUnknownEmail_throwsInvalidCredentials(){

        String email = "nobody-" + String.valueOf(System.currentTimeMillis()) + "@gmail.edu";

        LoginRequest loginRequest = new LoginRequest(email, "Password@!123");

        assertThrows(InvalidCredentialsException.class,()-> loginService.loginWithCredentials(loginRequest));
    }

    @Test
    void login_failureMessages_areIdentical() {
        String email = registerTestUser();

        InvalidCredentialsException wrongPassword = assertThrows(InvalidCredentialsException.class,
                () -> loginService.loginWithCredentials(new LoginRequest(email, "WrongPassword999!")));

        InvalidCredentialsException unknownEmail = assertThrows(InvalidCredentialsException.class,
                () -> loginService.loginWithCredentials(
                        new LoginRequest("nobody-" + System.currentTimeMillis() + "@gmail.edu", PASSWORD)));

        assertEquals(wrongPassword.getMessage(), unknownEmail.getMessage());
    }

}
