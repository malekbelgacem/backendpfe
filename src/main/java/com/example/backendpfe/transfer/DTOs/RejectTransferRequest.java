package com.example.backendpfe.transfer.DTOs;



import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectTransferRequest {

    @NotBlank
    private String rejectionReason;
}