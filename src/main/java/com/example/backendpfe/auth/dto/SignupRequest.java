package com.example.backendpfe.auth.dto;



import com.example.backendpfe.user.RoleName;
import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.Setter;

@Getter @Setter
public class SignupRequest {
    @NotBlank @Size(min=3,max=80)
    private String username;

    @NotBlank @Email
    private String email;

    @NotBlank @Size(min=6,max=120)
    private String password;


}

