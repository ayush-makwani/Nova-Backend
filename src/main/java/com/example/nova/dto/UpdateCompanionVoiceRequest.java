package com.example.nova.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateCompanionVoiceRequest {

    @NotNull(message = "voiceId is required")
    private Long voiceId;
}
