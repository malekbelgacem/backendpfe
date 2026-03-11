package com.example.backendpfe.config;

import com.example.backendpfe.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        // vérifier si un super admin existe
        boolean exists = userRepository.existsByRole_RoleName(RoleName.SUPER_ADMIN);

        if (!exists) {

            Role superAdminRole = roleRepository.findByRoleName(RoleName.SUPER_ADMIN)
                    .orElseGet(() ->
                            roleRepository.save(
                                    Role.builder()
                                            .roleName(RoleName.SUPER_ADMIN)
                                            .build()
                            )
                    );

            User superAdmin = User.builder()
                    .username("superadmin")
                    .email("admin@bank.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role(superAdminRole)
                    .isActive(true)
                    .isDeleted(false)
                    .build();

            userRepository.save(superAdmin);

            System.out.println("SUPER ADMIN CREATED");
        }
    }
}