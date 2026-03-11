package com.example.backendpfe.security;

import com.example.backendpfe.user.User;
import com.example.backendpfe.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User u = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String role = u.getRole().getRoleName().name(); // ADMIN/ANALYST/AUDITOR

        return new org.springframework.security.core.userdetails.User(
                u.getUsername(),
                u.getPassword(),
                Boolean.TRUE.equals(u.getIsActive()), // enabled
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }
}