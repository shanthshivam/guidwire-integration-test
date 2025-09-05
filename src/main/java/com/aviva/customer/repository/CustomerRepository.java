// Customer Repository
package com.aviva.customer.repository;

import com.aviva.claims.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByCustomerId(String customerId);
    boolean existsByCustomerId(String customerId);
}

// Customer Service
