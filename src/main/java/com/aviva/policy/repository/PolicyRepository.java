// Policy Repository
package com.aviva.policy.repository;

import com.aviva.claims.model.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {
    Optional<Policy> findByPolicyNumber(String policyNumber);
    List<Policy> findByCustomerId(String customerId);
    boolean existsByPolicyNumber(String policyNumber);
}

// Claim Repository (for checking existing claims)
