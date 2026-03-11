package com.example.backendpfe.auth;

import com.example.backendpfe.auth.dto.AuthResponse;
import com.example.backendpfe.auth.dto.RefreshRequest;
import com.example.backendpfe.auth.dto.SigninRequest;
import com.example.backendpfe.auth.dto.SignupRequest;
import com.example.backendpfe.security.JwtService;
import com.example.backendpfe.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    private Role getOrCreateRole(RoleName roleName) {
        return roleRepository.findByRoleName(roleName)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(roleName).build()));
    }

    public AuthResponse signup(SignupRequest req) {
        if (userRepository.existsByUsernameAndIsDeletedFalse(req.getUsername())) {
            throw new RuntimeException("Username already used");
        }
        if (userRepository.existsByEmailAndIsDeletedFalse(req.getEmail())) {
            throw new RuntimeException("Email already used");
        }

        RoleName roleName = RoleName.CLIENT;
        Role role = getOrCreateRole(roleName);

        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(role)
                .isActive(true)
                .isDeleted(false)
                .createdAt(Instant.now())
                .build();

        userRepository.save(user);

        var userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(roleName.name())
                .build();

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                user.getIdUser(),
                user.getUsername(),
                roleName.name()
        );
    }

    public AuthResponse signin(SigninRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
        );

        var principal = (org.springframework.security.core.userdetails.User) auth.getPrincipal();

        User user = userRepository.findByUsernameAndIsDeletedFalse(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setLastLogin(Instant.now());

        String accessToken = jwtService.generateToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);

        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                user.getIdUser(),
                user.getUsername(),
                user.getRole().getRoleName().name()
        );
    }

    public AuthResponse refreshToken(RefreshRequest req) {
        String refreshToken = req.getRefreshToken();
        String username = jwtService.extractUsername(refreshToken);

        User user = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRefreshToken() == null || !user.getRefreshToken().equals(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        if (jwtService.isTokenExpired(refreshToken)) {
            throw new RuntimeException("Refresh token expired");
        }

        var userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().getRoleName().name())
                .build();

        String newAccessToken = jwtService.generateToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        user.setRefreshToken(newRefreshToken);
        userRepository.save(user);

        return new AuthResponse(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                user.getIdUser(),
                user.getUsername(),
                user.getRole().getRoleName().name()
        );
    }
}