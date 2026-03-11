package com.example.backendpfe.accountrequest;

import com.example.backendpfe.accountrequest.dto.AccountRequestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/account-requests")
@RequiredArgsConstructor
public class AccountRequestController {

    private final AccountRequestService service;

    // ✅ version temporaire avec clientId
    @PostMapping(value = "/client/{clientId}", consumes = "multipart/form-data")
    public ResponseEntity<AccountRequestResponse> createByClientId(
            @PathVariable Long clientId,
            @RequestParam String accountType,
            @RequestParam String description,
            @RequestParam MultipartFile document
    ) {
        return ResponseEntity.ok(
                service.createRequest(clientId, accountType, description, document)
        );
    }

    // ✅ CLIENT: my requests
    @GetMapping("/client/{clientId}")
    public ResponseEntity<Page<AccountRequestResponse>> myRequests(
            @PathVariable Long clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(service.myRequests(clientId, page, size));
    }
}