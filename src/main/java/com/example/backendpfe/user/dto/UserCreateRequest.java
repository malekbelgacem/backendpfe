package com.example.backendpfe.user.dto;



import com.example.backendpfe.user.RoleName;
import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.Setter;

@Getter @Setter
public class UserCreateRequest {
    @NotBlank @Size(min=3,max=80)
    private String username;

    @NotBlank @Email
    private String email;

    @NotBlank @Size(min=6,max=120)
    private String password;

    private RoleName role; // optionnel
    private Boolean isActive; // optionnel
    private String adresse;
    private Double latitude;
    private Double longitude;
}
