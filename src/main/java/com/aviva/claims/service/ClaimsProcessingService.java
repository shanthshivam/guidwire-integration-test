package com.aviva.claims.service;

import com.aviva.claims.dto.ClaimRequest;
import com.aviva.claims.dto.ClaimResponse;
import com.aviva.claims.model.Claim;
import com.aviva.claims.repository.ClaimsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.UUID;
import java.util.Optional;

@Service
public class ClaimsProcessingService {
    
    private final ClaimsRepository claimsRepository;
    private final ClaimsValidationService validationService;
    
    @Autowired
    public ClaimsProcessingService(ClaimsRepository claimsRepository, 
                                 ClaimsValidationService validationService) {
        this.claimsRepository = claimsRepository;
        this.validationService = validationService;
    }
    
    public Mono<ClaimResponse> processClaim(ClaimRequest claimRequest) {
        return validationService.validateClaim(claimRequest)
            .flatMap(validationResult -> {
                if (!validationResult.isValid()) {
                    return Mono.error(new ClaimValidationException(validationResult.getMessage()));
                }
                return createClaim(claimRequest);
            })
            .map(ClaimResponse::new);
    }
    
    private Mono<Claim> createClaim(ClaimRequest claimRequest) {
        String claimNumber = generateClaimNumber();
        
        Claim claim = new Claim(
            claimNumber,
            claimRequest.getCustomerId(),
            claimRequest.getPolicyNumber(),
            claimRequest.getClaimType(),
            claimRequest.getClaimAmount(),
            claimRequest.getDescription(),
            claimRequest.getIncidentDate(),
            claimRequest.getIncidentLocation()
        );
        
        claim.setStatus(Claim.ClaimStatus.PENDING);
        
        return Mono.fromCallable(() -> claimsRepository.save(claim));
    }
    
    public Optional<Claim> getClaimByNumber(String claimNumber) {
        return claimsRepository.findByClaimNumber(claimNumber);
    }
    
    public Claim updateClaimStatus(String claimNumber, Claim.ClaimStatus status) {
        Claim claim = claimsRepository.findByClaimNumber(claimNumber)
            .orElseThrow(() -> new ClaimNotFoundException("Claim not found: " + claimNumber));
        
        claim.setStatus(status);
        claim.setUpdatedAt(java.time.LocalDateTime.now());
        
        return claimsRepository.save(claim);
    }
    
    private String generateClaimNumber() {
        return "CLM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    // Custom Exceptions
    public static class ClaimValidationException extends RuntimeException {
        public ClaimValidationException(String message) {
            super(message);
        }
    }
    
    public static class ClaimNotFoundException extends RuntimeException {
        public ClaimNotFoundException(String message) {
            super(message);
        }
    }
}

// Claims Controller
