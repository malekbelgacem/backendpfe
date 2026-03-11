package com.example.backendpfe.user.dto;



import com.example.backendpfe.user.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter; import lombok.Setter;

@Getter @Setter
public class UserUpdateRequest {
    @Size(min=3,max=80)
    private String username;

    @Email
    private String email;

    @Size(min=6,max=120)
    private String password; // optionnel (si tu veux changer)

    private RoleName role;
    private Boolean isActive;
    private String adresse;
    private Double latitude;
    private Double longitude;
}
