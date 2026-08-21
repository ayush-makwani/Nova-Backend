package com.example.nova.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** Full replacement, not a merge - the caller always sends the complete picture. */
@Data
public class UpdateProjectMemoryRequest {

    // Required but may be an empty string.
    @NotNull(message = "compactMemory is required")
    @Size(max = 20000, message = "compactMemory must be at most 20000 characters")
    private String compactMemory;

    // Optional - absent means empty, not "leave unchanged".
    @Size(max = 200, message = "No more than 200 decisions are allowed")
    private List<String> decisions;

    @Size(max = 200, message = "No more than 200 actions are allowed")
    private List<String> actions;

    @Size(max = 200, message = "No more than 200 notes are allowed")
    private List<String> notes;
}
