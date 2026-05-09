package com.example.backendpfe.alert.dto;

import com.example.backendpfe.alert.AlertStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAlertStatusRequest {

    @NotNull
    private AlertStatus status;
}