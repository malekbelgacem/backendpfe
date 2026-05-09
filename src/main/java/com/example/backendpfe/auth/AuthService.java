package com.example.backendpfe.auth;

import com.example.backendpfe.audit.AuditAction;
import com.example.backendpfe.audit.AuditLogService;
import com.example.backendpfe.auth.dto.AuthResponse;
import com.example.backendpfe.auth.dto.GoogleSigninRequest;
import com.example.backendpfe.auth.dto.RefreshRequest;
import com.example.backendpfe.auth.dto.SigninRequest;
import com.example.backendpfe.auth.dto.SignupRequest;
import com.example.backendpfe.security.JwtService;
import com.example.backendpfe.user.*;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;

    @Value("${app.google.client-id}")
    private String googleClientId;

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
        User savedUser = userRepository.save(user);

        auditLogService.record(
                AuditAction.LOGIN,
                "AUTH",
                savedUser.getIdUser(),
                savedUser,
                null,
                "User logged in successfully with username: " + savedUser.getUsername()
        );

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                savedUser.getIdUser(),
                savedUser.getUsername(),
                savedUser.getRole().getRoleName().name()
        );
    }

    public AuthResponse googleSignin(GoogleSigninRequest req) {
        GoogleIdToken.Payload payload = verifyGoogleToken(req.getIdToken());

        String email = payload.getEmail();
        String fullName = (String) payload.get("name");

        if (email == null || email.isBlank()) {
            throw new RuntimeException("Google account email not found");
        }

        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseGet(() -> createGoogleUser(email, fullName));

        user.setLastLogin(Instant.now());

        var userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().getRoleName().name())
                .build();

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        user.setRefreshToken(refreshToken);
        User savedUser = userRepository.save(user);

        auditLogService.record(
                AuditAction.LOGIN,
                "AUTH_GOOGLE",
                savedUser.getIdUser(),
                savedUser,
                null,
                "User logged in successfully with Google account: " + savedUser.getEmail()
        );

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                savedUser.getIdUser(),
                savedUser.getUsername(),
                savedUser.getRole().getRoleName().name()
        );
    }

    private GoogleIdToken.Payload verifyGoogleToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken == null) {
                throw new RuntimeException("Invalid Google ID token");
            }

            return idToken.getPayload();

        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Google token verification failed: " + e.getMessage());
        }
    }

    private User createGoogleUser(String email, String fullName) {
        RoleName roleName = RoleName.CLIENT;
        Role role = getOrCreateRole(roleName);

        String baseUsername = email.split("@")[0];
        String username = generateUniqueUsername(baseUsername);

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(role)
                .isActive(true)
                .isDeleted(false)
                .createdAt(Instant.now())
                .build();

        return userRepository.save(user);
    }

    private String generateUniqueUsername(String baseUsername) {
        String candidate = baseUsername;
        int i = 1;

        while (userRepository.existsByUsernameAndIsDeletedFalse(candidate)) {
            candidate = baseUsername + i;
            i++;
        }

        return candidate;
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
        User savedUser = userRepository.save(user);

        return new AuthResponse(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                savedUser.getIdUser(),
                savedUser.getUsername(),
                savedUser.getRole().getRoleName().name()
        );
    }
}