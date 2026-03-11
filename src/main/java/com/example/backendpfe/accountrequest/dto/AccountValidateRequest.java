package com.example.backendpfe.accountrequest.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountValidateRequest {

    @NotNull
    private Long requestId;

    @NotNull
    private Boolean approved; // true=approve false=reject

    private String decisionNote;

    // obligatoires seulement si approved = true
    private String accountNumber;
    private Long analystId;
    private Long auditorId;
}