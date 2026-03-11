package com.example.backendpfe.user.dto;

import com.example.backendpfe.user.RoleName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRoleRequest {

    private RoleName roleName;

}