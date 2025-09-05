package com.aviva.policy.controller;

import com.aviva.claims.model.Policy;
import com.aviva.claims.model.Claim;
import com.aviva.policy.service.PolicyService;
import com.aviva.policy.dto.PolicyValidationResponse;
import com.aviva.policy.dto.ClaimValidationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {
    
    private final PolicyService policyService;
    
    @Autowired
    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }
    
    @GetMapping("/{policyNumber}")
    public ResponseEntity<Policy> getPolicy(@PathVariable String policyNumber) {
        Optional<Policy> policy = policyService.findByPolicyNumber(policyNumber);
        return policy.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Policy>> getPoliciesByCustomer(@PathVariable String customerId) {
        List<Policy> policies = policyService.findByCustomerId(customerId);
        return ResponseEntity.ok(policies);
    }
    
    @GetMapping("/{policyNumber}/validate")
    public ResponseEntity<PolicyValidationResponse> validatePolicy(
            @PathVariable String policyNumber, 
            @RequestParam String customerId) {
        
        Optional<Policy> policyOpt = policyService.findByPolicyNumber(policyNumber);
        
        if (policyOpt.isEmpty()) {
            return ResponseEntity.ok(new PolicyValidationResponse(
                false, policyNumber, customerId, false, "Policy not found"));
        }
        
        Policy policy = policyOpt.get();
        boolean isValidForClaim = policyService.isPolicyValidForClaim(policyNumber, customerId);
        boolean isActive = policyService.isPolicyActive(policyNumber);
        
        String message = isValidForClaim ? "Policy is valid for claims" : 
                        !policy.getCustomerId().equals(customerId) ? "Policy does not belong to customer" :
                        !isActive ? "Policy is not active or expired" : "Policy validation failed";
        
        return ResponseEntity.ok(new PolicyValidationResponse(
            isValidForClaim, policyNumber, customerId, isActive, message));
    }
    
    @GetMapping("/{policyNumber}/claims/validate")
    public ResponseEntity<ClaimValidationResponse> validateClaim(
            @PathVariable String policyNumber,
            @RequestParam String customerId,
            @RequestParam String incidentLocation,
            @RequestParam String incidentDate) {
        
        LocalDateTime incidentDateTime = LocalDateTime.parse(incidentDate);
        
        boolean isDuplicate = policyService.isClaimDuplicate(
            customerId, policyNumber, incidentLocation, incidentDateTime);
        
        List<Claim> existingClaims = policyService.findClaimsByPolicy(policyNumber);
        boolean hasExistingClaims = !existingClaims.isEmpty();
        
        String message = isDuplicate ? "Duplicate claim detected" :
                        hasExistingClaims ? "Policy has existing claims" :
                        "No duplicate claims found";
        
        return ResponseEntity.ok(new ClaimValidationResponse(
            isDuplicate, hasExistingClaims, existingClaims, message));
    }
    
    @PostMapping
    public ResponseEntity<Policy> createPolicy(@RequestBody Policy policy) {
        try {
            Policy savedPolicy = policyService.savePolicy(policy);
            return ResponseEntity.ok(savedPolicy);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

// Policy Service Application
