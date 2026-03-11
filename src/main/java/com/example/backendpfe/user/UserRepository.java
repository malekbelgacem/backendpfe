package com.example.backendpfe.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // ✅ For auth + uniqueness (ignore deleted users)
    Optional<User> findByUsernameAndIsDeletedFalse(String username);
    Optional<User> findByEmailAndIsDeletedFalse(String email);

    boolean existsByUsernameAndIsDeletedFalse(String username);
    boolean existsByEmailAndIsDeletedFalse(String email);

    // ✅ For API reads
    Optional<User> findByIdUserAndIsDeletedFalse(Long idUser);

    // ✅ Pagination (only not deleted)
    Page<User> findAllByIsDeletedFalse(Pageable pageable);
    boolean existsByRole_RoleName(RoleName roleName);
}