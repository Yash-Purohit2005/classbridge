package com.classbridge.auth;

import com.classbridge.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    @GetMapping("/api/me")
    public AuthenticatedUser getCurrentUser(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return user;
    }

}
