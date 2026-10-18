package com.classbridge.auth.dto;

public record SignupRequest(String orgName,String adminName, String adminEmail,String password) {
}
