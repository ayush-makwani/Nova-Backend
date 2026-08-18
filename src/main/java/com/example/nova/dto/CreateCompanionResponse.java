package com.example.nova.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class CreateCompanionResponse {
    private String message;
    private List<CompanionResponse> companions;
    private BigDecimal totalMonthlyPrice;
}
