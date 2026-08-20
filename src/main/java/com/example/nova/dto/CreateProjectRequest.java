package com.example.nova.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateProjectRequest {

    @NotBlank(message = "Project name is required")
    @Size(min = 2, max = 100, message = "Project name must be between 2 and 100 characters")
    private String name;

    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;

    @Size(max = 10, message = "No more than 10 tags are allowed")
    private List<@NotBlank @Size(max = 30) String> tags;

    // "Paste text context" from step 2 of the wizard.
    @Size(max = 5000, message = "Context must be at most 5000 characters")
    private String textContext;

    // S3 object keys for documents already uploaded elsewhere (not full URLs) - "Upload Documents" in step 2.
    @Size(max = 20, message = "No more than 20 documents are allowed")
    private List<@NotBlank @Size(max = 500) String> documentKeys;

    // S3 object key for an already-uploaded voice-note recording (not a full URL) - "Voice Note" in step 2.
    @Size(max = 500)
    private String voiceNoteKey;
}
