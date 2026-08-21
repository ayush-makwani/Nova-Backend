package com.example.nova.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ProjectDocumentRequest {

    // S3 object keys for already-uploaded documents (not full URLs) - same
    // convention as CreateProjectRequest.documentKeys; the upload itself
    // happens elsewhere, this just links the keys to the project.
    @NotEmpty(message = "At least one document key is required")
    @Size(max = 20, message = "No more than 20 documents can be added at once")
    private List<@NotBlank @Size(max = 500) String> documentKeys;
}
