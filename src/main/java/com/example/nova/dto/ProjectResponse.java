package com.example.nova.dto;

import com.example.nova.entity.CompanionPresenceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private List<String> tags;
    private Long companionId;
    private String companionName;
    private CompanionPresenceStatus companionPresenceStatus;
    private int meetingsCount;
    private int contextFilesCount; // derived: documentKeys.size()
    private List<String> documentKeys;
    private String voiceNoteKey; // null if no voice note was recorded
    private Instant lastMeetingAt; // null if the companion hasn't attended a meeting yet
    private Instant createdAt;
}
