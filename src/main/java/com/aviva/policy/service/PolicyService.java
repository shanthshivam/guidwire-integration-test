package com.aviva.policy.service;

import com.aviva.claims.model.Policy;
import com.aviva.claims.model.Claim;
import com.aviva.policy.repository.PolicyRepository;
import com.aviva.policy.repository.ClaimRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PolicyService {
    
    private final PolicyRepository policyRepository;
    private final ClaimRepository claimRepository;
    
    @Autowired
    public PolicyService(PolicyRepository policyRepository, ClaimRepository claimRepository) {
        this.policyRepository = policyRepository;
        this.claimRepository = claimRepository;
    }
    
    public Optional<Policy> findByPolicyNumber(String policyNumber) {
        return policyRepository.findByPolicyNumber(policyNumber);
    }
    
    public List<Policy> findByCustomerId(String customerId) {
        return policyRepository.findByCustomerId(customerId);
    }
    
    public boolean isPolicyActive(String policyNumber) {
        return policyRepository.findByPolicyNumber(policyNumber)
            .map(policy -> policy.getStatus() == Policy.PolicyStatus.ACTIVE && 
                          policy.getEndDate().isAfter(LocalDate.now()))
            .orElse(false);
    }
    
    public boolean isPolicyValidForClaim(String policyNumber, String customerId) {
        return policyRepository.findByPolicyNumber(policyNumber)
            .map(policy -> policy.getCustomerId().equals(customerId) && 
                          policy.getStatus() == Policy.PolicyStatus.ACTIVE &&
                          policy.getEndDate().isAfter(LocalDate.now()))
            .orElse(false);
    }
    
    public Policy savePolicy(Policy policy) {
        return policyRepository.save(policy);
    }
    
    // Claim-related methods
    public boolean isClaimDuplicate(String customerId, String policyNumber, 
                                   String incidentLocation, java.time.LocalDateTime incidentDate) {
        List<Claim> existingClaims = claimRepository
            .findByCustomerIdAndPolicyNumberAndIncidentLocationAndIncidentDate(
                customerId, policyNumber, incidentLocation, incidentDate);
        return !existingClaims.isEmpty();
    }
    
    public List<Claim> findClaimsByPolicy(String policyNumber) {
        return claimRepository.findByPolicyNumber(policyNumber);
    }
    
    public List<Claim> findClaimsByCustomer(String customerId) {
        return claimRepository.findByCustomerId(customerId);
    }
    
    public Optional<Claim> findClaimByNumber(String claimNumber) {
        return claimRepository.findByClaimNumber(claimNumber);
    }
    
    public boolean claimExists(String claimNumber) {
        return claimRepository.existsByClaimNumber(claimNumber);
    }
}

// Policy Validation Response DTO
