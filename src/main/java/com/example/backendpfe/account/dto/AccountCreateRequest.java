package com.example.backendpfe.account.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AccountCreateRequest {

    @NotBlank
    @Size(min = 5, max = 40)
    private String accountNumber;

    @NotNull
    private Long clientId;

    @NotNull
    private Long analystId;

    @NotNull
    private Long auditorId;
}