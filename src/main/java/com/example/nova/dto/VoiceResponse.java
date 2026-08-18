package com.example.nova.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class VoiceResponse {
    private Long id;
    private String name;
    private List<String> traits;
    private String description;
    private String previewAudioUrl;
    private boolean selected; // true -> "ACTIVE" badge, false -> "SELECT" button
}
