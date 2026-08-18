package com.example.nova.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateCompanionSettingsRequest {

    @NotBlank(message = "Display name is required")
    @Size(min = 2, max = 50, message = "Display name must be between 2 and 50 characters")
    private String displayName;

    @NotBlank(message = "Companion email is required")
    @Email(message = "Companion email must be a valid email address")
    @Size(max = 100)
    private String email;

    private boolean autoJoinMeetings;
    private boolean sendMomAutomatically;
    private boolean respondInVoice;
    private boolean recordMeetingAudio;
}
