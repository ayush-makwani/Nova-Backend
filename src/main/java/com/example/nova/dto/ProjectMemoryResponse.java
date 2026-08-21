package com.example.nova.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class ProjectMemoryResponse {
    private Long projectId;
    private String compactMemory;
    private List<String> decisions;
    private List<String> actions;
    private List<String> notes;
    private int meetingsSummarised; // 0 = never run
    private Instant updatedAt; // null until the first write
}
