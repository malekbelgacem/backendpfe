package com.example.backendpfe.auth.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter; import lombok.Setter;

@Getter @Setter
public class SigninRequest {
    @NotBlank private String username;
    @NotBlank private String password;
}

