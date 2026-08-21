package com.example.nova.dto;

import com.example.nova.entity.CompanionPresenceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/** The Project Details screen: header, assigned companion, stats, context documents, and meeting history. */
@Data
@Builder
@AllArgsConstructor
public class ProjectDetailResponse {
    private Long id;
    private String name;
    private String description;
    private List<String> tags;

    // Assigned companion - all null if the project has none.
    private Long companionId;
    private String companionName;
    private String companionEmail;
    private String companionVoice;
    private CompanionPresenceStatus companionPresenceStatus;

    // Project stats
    private int meetingsCount;
    private int contextFilesCount;
    private Instant createdAt;

    private List<ProjectDocumentUrlResponse> documents;

    // Title of the nearest upcoming SCHEDULED meeting, if any - drives the
    // "Companion will load all files before '<title>'" note. Null if none.
    private String nextMeetingTitle;

    private List<MeetingResponse> meetings;
}
