package com.aviva.claims.service;

import com.aviva.claims.dto.ClaimRequest;
import com.aviva.claims.client.ExternalServiceClient;
import com.aviva.claims.model.Customer;
import com.aviva.policy.dto.PolicyValidationResponse;
import com.aviva.policy.dto.ClaimValidationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;

@Service
public class ClaimsValidationService {
    
    private final ExternalServiceClient externalServiceClient;
    
    @Autowired
    public ClaimsValidationService(ExternalServiceClient externalServiceClient) {
        this.externalServiceClient = externalServiceClient;
    }
    
    public Mono<ValidationResult> validateClaim(ClaimRequest claimRequest) {
        return validateCustomer(claimRequest.getCustomerId())
            .flatMap(customerValid -> {
                if (!customerValid.isValid()) {
                    return Mono.just(customerValid);
                }
                return validatePolicy(claimRequest.getPolicyNumber(), claimRequest.getCustomerId());
            })
            .flatMap(policyValid -> {
                if (!policyValid.isValid()) {
                    return Mono.just(policyValid);
                }
                return validateClaimDetails(claimRequest);
            })
            .flatMap(claimValid -> {
                if (!claimValid.isValid()) {
                    return Mono.just(claimValid);
                }
                return validateBusinessRules(claimRequest);
            });
    }
    
    private Mono<ValidationResult> validateCustomer(String customerId) {
        return externalServiceClient.customerExists(customerId)
            .map(exists -> exists ? 
                ValidationResult.valid("Customer validation passed") :
                ValidationResult.invalid("Customer not found: " + customerId));
    }
    
    private Mono<ValidationResult> validatePolicy(String policyNumber, String customerId) {
        return externalServiceClient.validatePolicy(policyNumber, customerId)
            .map(response -> response.isValid() ?
                ValidationResult.valid("Policy validation passed") :
                ValidationResult.invalid("Policy validation failed: " + response.getValidationMessage()));
    }
    
    private Mono<ValidationResult> validateClaimDetails(ClaimRequest claimRequest) {
        return externalServiceClient.validateClaim(
                claimRequest.getPolicyNumber(),
                claimRequest.getCustomerId(),
                claimRequest.getIncidentLocation(),
                claimRequest.getIncidentDate())
            .map(response -> response.isDuplicate() ?
                ValidationResult.invalid("Duplicate claim detected") :
                ValidationResult.valid("Claim details validation passed"));
    }
    
    private Mono<ValidationResult> validateBusinessRules(ClaimRequest claimRequest) {
        // Business rule validations
        if (claimRequest.getClaimAmount().compareTo(BigDecimal.valueOf(100000)) > 0) {
            return Mono.just(ValidationResult.invalid("Claim amount exceeds maximum limit of £100,000"));
        }
        
        if (claimRequest.getIncidentDate().isAfter(java.time.LocalDateTime.now())) {
            return Mono.just(ValidationResult.invalid("Incident date cannot be in the future"));
        }
        
        if (claimRequest.getIncidentDate().isBefore(java.time.LocalDateTime.now().minusYears(1))) {
            return Mono.just(ValidationResult.invalid("Incident date cannot be more than 1 year old"));
        }
        
        return Mono.just(ValidationResult.valid("Business rules validation passed"));
    }
    
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        
        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public static ValidationResult valid(String message) {
            return new ValidationResult(true, message);
        }
        
        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }
        
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }
}

// Claims Processing Service
