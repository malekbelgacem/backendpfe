package com.example.backendpfe.accountrequest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountRequestCreateRequest {

    @NotBlank
    private String accountType; // DEBIT / CREDIT

    @NotBlank
    private String description;
}