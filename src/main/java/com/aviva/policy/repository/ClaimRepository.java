package com.aviva.policy.repository;

import com.aviva.claims.model.Claim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {
    Optional<Claim> findByClaimNumber(String claimNumber);
    List<Claim> findByCustomerId(String customerId);
    List<Claim> findByPolicyNumber(String policyNumber);
    boolean existsByClaimNumber(String claimNumber);
    
    // Check for existing claims with same incident details (potential duplicates)
    List<Claim> findByCustomerIdAndPolicyNumberAndIncidentLocationAndIncidentDate(
        String customerId, String policyNumber, String incidentLocation, 
        java.time.LocalDateTime incidentDate);
}

// Policy Service
