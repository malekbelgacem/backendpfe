package com.example.backendpfe.account;

import com.example.backendpfe.account.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    // ✅ SUPER_ADMIN: list all accounts
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<AccountResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(accountService.getAll(page, size));
    }

    // ✅ SUPER_ADMIN: accounts by client
    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<AccountResponse>> getByClient(
            @PathVariable Long clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(accountService.getByClient(clientId, page, size));
    }

    // ✅ SUPER_ADMIN: accounts by analyst
    @GetMapping("/analyst/{analystId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<AccountResponse>> getByAnalyst(
            @PathVariable Long analystId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(accountService.getByAnalyst(analystId, page, size));
    }

    // ✅ SUPER_ADMIN: accounts by auditor
    @GetMapping("/auditor/{auditorId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<AccountResponse>> getByAuditor(
            @PathVariable Long auditorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(accountService.getByAuditor(auditorId, page, size));
    }

    // ✅ SUPER_ADMIN: get by id
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<AccountResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getById(id));
    }

    // ✅ SUPER_ADMIN: create account مباشرة (optional)
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody AccountCreateRequest req) {
        AccountResponse created = accountService.create(req);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getIdAccount())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    // ✅ SUPER_ADMIN: update
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<AccountResponse> update(@PathVariable Long id, @RequestBody AccountUpdateRequest req) {
        return ResponseEntity.ok(accountService.update(id, req));
    }

    // ✅ SUPER_ADMIN: soft delete
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        accountService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ✅ SUPER_ADMIN: restore
    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<AccountResponse> restore(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.restore(id));
    }
    @GetMapping("/my-accounts/{clientId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Page<AccountResponse>> getMyAccounts(
            @PathVariable Long clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(accountService.getByClient(clientId, page, size));
    }
}