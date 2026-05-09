package com.example.backendpfe.accountrequest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountRequestLatestStatusResponse {
    private boolean exists;
    private String status;
    private String accountType;
    private String description;
    private Long requestId;
}