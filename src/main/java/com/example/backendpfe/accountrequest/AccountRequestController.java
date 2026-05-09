package com.example.backendpfe.accountrequest;

import com.example.backendpfe.accountrequest.dto.AccountRequestLatestStatusResponse;
import com.example.backendpfe.accountrequest.dto.AccountRequestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/account-requests")
@RequiredArgsConstructor
public class AccountRequestController {

    private final AccountRequestService service;

    @PostMapping("/client/{id}")
    public ResponseEntity<?> create(
            @PathVariable Long id,
            @RequestParam("accountType") String accountType,
            @RequestParam("description") String description,
            @RequestParam("document") MultipartFile document
    ) {
        try {
            System.out.println("ID = " + id);
            System.out.println("TYPE = " + accountType);
            System.out.println("DESC = " + description);
            System.out.println("FILE = " + document.getOriginalFilename());

            AccountRequestResponse response = service.createRequest(id, accountType, description, document);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Demande reçue avec succès",
                    "requestId", response.getIdRequest(),
                    "status", response.getStatus()
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Erreur backend: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<Page<AccountRequestResponse>> myRequests(
            @PathVariable Long clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(service.myRequests(clientId, page, size));
    }

    @GetMapping("/client/{clientId}/latest")
    public ResponseEntity<AccountRequestLatestStatusResponse> latestRequestStatus(
            @PathVariable Long clientId
    ) {
        return ResponseEntity.ok(service.getLatestRequestStatus(clientId));
    }
}