package com.classbridge.security;

public record AuthenticatedUser(Long userId, Long orgId,String role) {
}
