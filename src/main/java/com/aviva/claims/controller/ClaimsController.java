package com.aviva.claims.controller;

import com.aviva.claims.dto.ClaimRequest;
import com.aviva.claims.dto.ClaimResponse;
import com.aviva.claims.model.Claim;
import com.aviva.claims.service.ClaimsProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/claims")
public class ClaimsController {
    
    private final ClaimsProcessingService claimsProcessingService;
    
    @Autowired
    public ClaimsController(ClaimsProcessingService claimsProcessingService) {
        this.claimsProcessingService = claimsProcessingService;
    }
    
    @PostMapping
    public Mono<ResponseEntity<ClaimResponse>> submitClaim(@Valid @RequestBody ClaimRequest claimRequest) {
        return claimsProcessingService.processClaim(claimRequest)
            .map(ResponseEntity::ok)
            .onErrorReturn(ResponseEntity.badRequest().build());
    }
    
    @GetMapping("/{claimNumber}")
    public ResponseEntity<ClaimResponse> getClaim(@PathVariable String claimNumber) {
        return claimsProcessingService.getClaimByNumber(claimNumber)
            .map(ClaimResponse::new)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{claimNumber}/status")
    public ResponseEntity<ClaimResponse> updateClaimStatus(
            @PathVariable String claimNumber,
            @RequestParam Claim.ClaimStatus status) {
        try {
            Claim updatedClaim = claimsProcessingService.updateClaimStatus(claimNumber, status);
            return ResponseEntity.ok(new ClaimResponse(updatedClaim));
        } catch (ClaimsProcessingService.ClaimNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

// WebClient Configuration
