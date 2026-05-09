package com.example.backendpfe.user;

import com.example.backendpfe.user.dto.UpdateUserRoleRequest;
import com.example.backendpfe.user.dto.UserCreateRequest;
import com.example.backendpfe.user.dto.UserResponse;
import com.example.backendpfe.user.dto.UserUpdateRequest;
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
                        Role.builder()
                                .roleName(roleName)
                                .build()
                ));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .idUser(user.getIdUser())
                .username(user.getUsername())
                .email(user.getEmail())
                .isActive(user.getIsActive())
                .role(user.getRole() != null ? user.getRole().getRoleName().name() : null)
                .adresse(user.getAdresse())
                .latitude(user.getLatitude())
                .longitude(user.getLongitude())
                .build();
    }

    public Page<UserResponse> getAll(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("idUser").ascending());
        return userRepository.findAllByIsDeletedFalse(pageable).map(this::toResponse);
    }

    public UserResponse getById(Long id) {
        User user = userRepository.findByIdUserAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return toResponse(user);
    }

    public UserResponse create(UserCreateRequest req) {
        if (userRepository.existsByUsernameAndIsDeletedFalse(req.getUsername())) {
            throw new RuntimeException("Username already used");
        }

        if (userRepository.existsByEmailAndIsDeletedFalse(req.getEmail())) {
            throw new RuntimeException("Email already used");
        }

        RoleName roleName = req.getRole() != null ? req.getRole() : RoleName.CLIENT;

        User user = User.builder()
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

        userRepository.save(user);
        return toResponse(user);
    }

    public UserResponse update(Long id, UserUpdateRequest req) {
        User user = userRepository.findByIdUserAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (req.getUsername() != null && !req.getUsername().isBlank()
                && !req.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsernameAndIsDeletedFalse(req.getUsername())) {
                throw new RuntimeException("Username already used");
            }
            user.setUsername(req.getUsername());
        }

        if (req.getEmail() != null && !req.getEmail().isBlank()
                && !req.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmailAndIsDeletedFalse(req.getEmail())) {
                throw new RuntimeException("Email already used");
            }
            user.setEmail(req.getEmail());
        }

        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        }

        if (req.getIsActive() != null) {
            user.setIsActive(req.getIsActive());
        }

        if (req.getRole() != null) {
            user.setRole(getOrCreateRole(req.getRole()));
        }

        if (req.getAdresse() != null) {
            user.setAdresse(req.getAdresse());
        }

        if (req.getLatitude() != null) {
            user.setLatitude(req.getLatitude());
        }

        if (req.getLongitude() != null) {
            user.setLongitude(req.getLongitude());
        }

        userRepository.save(user);
        return toResponse(user);
    }

    public void delete(Long id) {
        User user = userRepository.findByIdUserAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setIsDeleted(true);
        user.setIsActive(false);
        userRepository.save(user);
    }

    public UserResponse restore(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (Boolean.FALSE.equals(user.getIsDeleted())) {
            return toResponse(user);
        }

        user.setIsDeleted(false);
        user.setIsActive(true);
        userRepository.save(user);
        return toResponse(user);
    }

    public UserResponse updateUserRole(Long id, UpdateUserRoleRequest request) {
        User user = userRepository.findByIdUserAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getRoleName() == null) {
            throw new RuntimeException("Role is required");
        }

        Role role = roleRepository.findByRoleName(request.getRoleName())
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .roleName(request.getRoleName())
                                .build()
                ));

        user.setRole(role);
        userRepository.save(user);
        return toResponse(user);
    }
}