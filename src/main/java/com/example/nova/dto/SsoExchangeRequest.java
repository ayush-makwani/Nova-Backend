package com.example.nova.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SsoExchangeRequest {

    @NotBlank(message = "SSO exchange code is required")
    private String code;
}
