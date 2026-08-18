package com.example.nova.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MfaSetupResponse {
    private String secret;
    private String qrCodeImageBase64; // data:image/png;base64,....
    private String otpAuthUrl;
}
