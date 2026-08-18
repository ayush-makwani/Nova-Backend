package com.example.nova.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MfaEnableRequest {

    @NotBlank(message = "MFA code is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "MFA code must be a 6 digit number")
    private String code;
}
