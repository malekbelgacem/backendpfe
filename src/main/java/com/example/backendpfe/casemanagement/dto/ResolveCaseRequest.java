package com.example.backendpfe.casemanagement.dto;

import com.example.backendpfe.casemanagement.CaseDecision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResolveCaseRequest {

    @NotNull
    private CaseDecision finalDecision;

    @NotBlank
    private String justification;
}