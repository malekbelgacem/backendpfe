package com.example.backendpfe.casemanagement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCaseRequest {

    @NotBlank
    private String justification;
}
