package com.example.backendpfe.transfer;

import com.example.backendpfe.transfer.dto.CreateTransferRequest;
import com.example.backendpfe.transfer.dto.TransferResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public TransferResponse createTransfer(
            @Valid @RequestBody CreateTransferRequest request,
            Authentication authentication
    ) {
        return transferService.createTransfer(request, authentication.getName());
    }

    @GetMapping("/my")
    public List<TransferResponse> getMyTransfers(Authentication authentication) {
        return transferService.getMyTransfers(authentication.getName());
    }
    @PreAuthorize("hasRole('ANALYST')")
    @PatchMapping("/{id}/approve-blocked")
    public TransferResponse approveBlockedTransfer(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return transferService.approveBlockedTransfer(id, authentication.getName());
    }
}