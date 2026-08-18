package com.example.nova.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    /** Required only if the account has MFA enabled and the client already has a code. */
    private String mfaCode;
}
