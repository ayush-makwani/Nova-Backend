package com.example.nova.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/** Minimal shape for populating the "Companion" assignment dropdown with currently-unassigned companions. */
@Data
@Builder
@AllArgsConstructor
public class CompanionOptionResponse {
    private Long id;
    private String name;
    private String email;
}
