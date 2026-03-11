package com.example.backendpfe.user.dto;



import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {
    private Long idUser;
    private String username;
    private String email;
    private Boolean isActive;
    private String role; // ADMIN/ANALYST/AUDITOR
    private String adresse;
    private Double latitude;
    private Double longitude;
}

