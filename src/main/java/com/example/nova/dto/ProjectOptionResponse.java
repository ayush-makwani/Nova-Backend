package com.example.nova.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/** Minimal shape for populating a "Select Project" dropdown. */
@Data
@Builder
@AllArgsConstructor
public class ProjectOptionResponse {
    private Long id;
    private String name;
}
