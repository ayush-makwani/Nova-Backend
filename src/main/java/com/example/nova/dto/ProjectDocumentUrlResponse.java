package com.example.nova.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ProjectDocumentUrlResponse {
    private String documentKey; // S3 object key, as stored on the project
    private String url; // documentKey resolved against AwsCredential's base url
}
