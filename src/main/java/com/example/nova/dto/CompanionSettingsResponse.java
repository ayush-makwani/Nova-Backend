package com.example.nova.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CompanionSettingsResponse {
    private Long id;
    private String displayName;
    private String email;
    private boolean autoJoinMeetings;
    private boolean sendMomAutomatically;
    private boolean respondInVoice;
    private boolean recordMeetingAudio;
}
