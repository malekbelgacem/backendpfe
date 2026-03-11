package com.example.backendpfe.user;

import com.example.backendpfe.user.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private Role getOrCreateRole(RoleName roleName) {
        return roleRepository.findByRoleName(roleName)
                .orElseGet(() -> roleRepository.save(
                        Role.builder().roleName(roleName).build()
                ));
    }

    private UserResponse toResponse(User u) {
        return UserResponse.builder()
                .idUser(u.getIdUser())
                .username(u.getUsername())
                .email(u.getEmail())
                .isActive(u.getIsActive())
                .role(u.getRole().getRoleName().name())
                .adresse(u.getAdresse())
                .latitude(u.getLatitude())
                .longitude(u.getLongitude())
                .build();
    }

    // ✅ Pagination: only not deleted
    public Page<UserResponse> getAll(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("idUser").ascending());
        return userRepository.findAllByIsDeletedFalse(pageable).map(this::toResponse);
    }

    public UserResponse getById(Long id) {
        User u = userRepository.findByIdUserAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return toResponse(u);
    }

    public UserResponse create(UserCreateRequest req) {
        if (userRepository.existsByUsernameAndIsDeletedFalse(req.getUsername())) {
            throw new RuntimeException("Username already used");
        }

        if (userRepository.existsByEmailAndIsDeletedFalse(req.getEmail())) {
            throw new RuntimeException("Email already used");
        }

        RoleName roleName = (req.getRole() != null) ? req.getRole() : RoleName.CLIENT;

        User u = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(getOrCreateRole(roleName))
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .isDeleted(false)
                .adresse(req.getAdresse())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .build();

        userRepository.save(u);
        return toResponse(u);
    }

    public UserResponse update(Long id, UserUpdateRequest req) {
        User u = userRepository.findByIdUserAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (req.getUsername() != null && !req.getUsername().equals(u.getUsername())) {
            if (userRepository.existsByUsernameAndIsDeletedFalse(req.getUsername())) {
                throw new RuntimeException("Username already used");
            }
            u.setUsername(req.getUsername());
        }

        if (req.getEmail() != null && !req.getEmail().equals(u.getEmail())) {
            if (userRepository.existsByEmailAndIsDeletedFalse(req.getEmail())) {
                throw new RuntimeException("Email already used");
            }
            u.setEmail(req.getEmail());
        }

        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            u.setPassword(passwordEncoder.encode(req.getPassword()));
        }

        if (req.getIsActive() != null) {
            u.setIsActive(req.getIsActive());
        }

        if (req.getRole() != null) {
            u.setRole(getOrCreateRole(req.getRole()));
        }

        if (req.getAdresse() != null) {
            u.setAdresse(req.getAdresse());
        }

        if (req.getLatitude() != null) {
            u.setLatitude(req.getLatitude());
        }

        if (req.getLongitude() != null) {
            u.setLongitude(req.getLongitude());
        }

        userRepository.save(u);
        return toResponse(u);
    }

    // ✅ Soft delete (+ disable)
    public void delete(Long id) {
        User u = userRepository.findByIdUserAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        u.setIsDeleted(true);
        u.setIsActive(false);
        userRepository.save(u);
    }

    // ✅ Restore deleted user
    public UserResponse restore(Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (Boolean.FALSE.equals(u.getIsDeleted())) {
            return toResponse(u);
        }

        u.setIsDeleted(false);
        u.setIsActive(true);
        userRepository.save(u);
        return toResponse(u);
    }

    // ✅ SUPER_ADMIN: update only role
    public UserResponse updateUserRole(Long id, UpdateUserRoleRequest request) {
        User user = userRepository.findByIdUserAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getRoleName() == null) {
            throw new RuntimeException("Role is required");
        }

        Role role = roleRepository.findByRoleName(request.getRoleName())
                .orElseGet(() -> roleRepository.save(
                        Role.builder().roleName(request.getRoleName()).build()
                ));

        user.setRole(role);

        userRepository.save(user);
        return toResponse(user);
    }
}