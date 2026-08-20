package com.example.nova.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignCompanionRequest {

    @NotNull(message = "companionId is required")
    private Long companionId;
}
