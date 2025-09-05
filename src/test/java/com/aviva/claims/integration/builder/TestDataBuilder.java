// Test Data Builder
package com.aviva.claims.integration.builder;

import com.aviva.claims.dto.ClaimRequest;
import com.aviva.claims.model.Claim;
import com.aviva.claims.model.Customer;
import com.aviva.claims.model.Policy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TestDataBuilder {
    
    public static class CustomerBuilder {
        private String customerId = "TEST-CUST-001";
        private String firstName = "Test";
        private String lastName = "Customer";
        private String email = "test.customer@aviva.com";
        private String phoneNumber = "+44-20-1234-5678";
        private String address = "Test Address, London, UK";
        
        public CustomerBuilder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }
        
        public CustomerBuilder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }
        
        public CustomerBuilder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }
        
        public CustomerBuilder email(String email) {
            this.email = email;
            return this;
        }
        
        public Customer build() {
            return new Customer(customerId, firstName, lastName, email, phoneNumber, address);
        }
    }
    
    public static class PolicyBuilder {
        private String policyNumber = "TEST-POL-001";
        private String customerId = "TEST-CUST-001";
        private String vehicleRegistration = "TEST123";
        private LocalDate startDate = LocalDate.now().minusMonths(6);
        private LocalDate endDate = LocalDate.now().plusMonths(6);
        private BigDecimal coverageAmount = new BigDecimal("50000.00");
        private BigDecimal premium = new BigDecimal("1200.00");
        private String vehicleMake = "Ford";
        private String vehicleModel = "Focus";
        private Integer vehicleYear = 2020;
        
        public PolicyBuilder policyNumber(String policyNumber) {
            this.policyNumber = policyNumber;
            return this;
        }
        
        public PolicyBuilder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }
        
        public PolicyBuilder vehicleRegistration(String vehicleRegistration) {
            this.vehicleRegistration = vehicleRegistration;
            return this;
        }
        
        public PolicyBuilder expired() {
            this.startDate = LocalDate.now().minusYears(2);
            this.endDate = LocalDate.now().minusYears(1);
            return this;
        }
        
        public PolicyBuilder active() {
            this.startDate = LocalDate.now().minusMonths(6);
            this.endDate = LocalDate.now().plusMonths(6);
            return this;
        }
        
        public Policy build() {
            return new Policy(policyNumber, customerId, vehicleRegistration, 
                            startDate, endDate, coverageAmount, premium, 
                            vehicleMake, vehicleModel, vehicleYear);
        }
    }
    
    public static class ClaimRequestBuilder {
        private String customerId = "TEST-CUST-001";
        private String policyNumber = "TEST-POL-001";
        private Claim.ClaimType claimType = Claim.ClaimType.COLLISION;
        private BigDecimal claimAmount = new BigDecimal("5000.00");
        private String description = "Test claim description";
        private LocalDateTime incidentDate = LocalDateTime.now().minusDays(7);
        private String incidentLocation = "Test Location";
        
        public ClaimRequestBuilder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }
        
        public ClaimRequestBuilder policyNumber(String policyNumber) {
            this.policyNumber = policyNumber;
            return this;
        }
        
        public ClaimRequestBuilder claimType(Claim.ClaimType claimType) {
            this.claimType = claimType;
            return this;
        }
        
        public ClaimRequestBuilder claimAmount(BigDecimal claimAmount) {
            this.claimAmount = claimAmount;
            return this;
        }
        
        public ClaimRequestBuilder exceedsLimit() {
            this.claimAmount = new BigDecimal("150000.00");
            return this;
        }
        
        public ClaimRequestBuilder futureIncident() {
            this.incidentDate = LocalDateTime.now().plusDays(1);
            return this;
        }
        
        public ClaimRequestBuilder oldIncident() {
            this.incidentDate = LocalDateTime.now().minusYears(2);
            return this;
        }

        public ClaimRequestBuilder incidentLocation(String incidentLocation) {
            this.incidentLocation = incidentLocation;
            return this;
        }
        
        public ClaimRequestBuilder duplicateIncident() {
            this.incidentDate = LocalDateTime.parse("2024-06-15T14:30:00");
            this.incidentLocation = "M25 Junction 10";
            return this;
        }
        
        public ClaimRequest build() {
            return new ClaimRequest(customerId, policyNumber, claimType, 
                                  claimAmount, description, incidentDate, incidentLocation);
        }
    }
    
    public static CustomerBuilder customer() {
        return new CustomerBuilder();
    }
    
    public static PolicyBuilder policy() {
        return new PolicyBuilder();
    }
    
    public static ClaimRequestBuilder claimRequest() {
        return new ClaimRequestBuilder();
    }
}

// Performance and Load Test
