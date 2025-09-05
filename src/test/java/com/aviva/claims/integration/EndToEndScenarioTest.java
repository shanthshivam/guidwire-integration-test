package com.aviva.claims.integration;

import com.aviva.claims.dto.ClaimRequest;
import com.aviva.claims.dto.ClaimResponse;
import com.aviva.claims.integration.builder.TestDataBuilder;
import com.aviva.claims.model.Claim;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class EndToEndScenarioTest extends BaseIntegrationTest {
    
    @LocalServerPort
    private int claimsServicePort;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Override
    protected int getCustomerServicePort() {
        return 8081;
    }
    
    @Override
    protected int getPolicyServicePort() {
        return 8082;
    }
    
    @Test
    @DisplayName("Complete claim lifecycle - submission to status update")
    void completeClaimLifecycleTest() {
        // Step 1: Submit a new claim
        ClaimRequest claimRequest = TestDataBuilder.claimRequest()
                .customerId("CUST001")
                .policyNumber("POL001")
                .claimType(Claim.ClaimType.COLLISION)
                .claimAmount(new BigDecimal("7500.00"))
                .build();
        
        ResponseEntity<ClaimResponse> submitResponse = restTemplate.postForEntity(
                "http://localhost:" + claimsServicePort + "/api/claims",
                claimRequest,
                ClaimResponse.class
        );
        
        // Verify submission
        assertThat(submitResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(submitResponse.getBody()).isNotNull();
        assertThat(submitResponse.getBody().getStatus()).isEqualTo(Claim.ClaimStatus.PENDING);
        
        String claimNumber = submitResponse.getBody().getClaimNumber();
        
        // Step 2: Retrieve the submitted claim
        ResponseEntity<ClaimResponse> getResponse = restTemplate.getForEntity(
                "http://localhost:" + claimsServicePort + "/api/claims/" + claimNumber,
                ClaimResponse.class
        );
        
        // Verify retrieval
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().getClaimNumber()).isEqualTo(claimNumber);
        assertThat(getResponse.getBody().getClaimAmount()).isEqualTo(new BigDecimal("7500.00"));
        
        // Step 3: Update claim status to INVESTIGATING
        ResponseEntity<ClaimResponse> updateResponse = restTemplate.exchange(
                "http://localhost:" + claimsServicePort + "/api/claims/" + claimNumber + "/status?status=INVESTIGATING",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                ClaimResponse.class
        );
        
        // Verify status update
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody()).isNotNull();
        assertThat(updateResponse.getBody().getStatus()).isEqualTo(Claim.ClaimStatus.INVESTIGATING);
        
        // Step 4: Update claim status to APPROVED
        ResponseEntity<ClaimResponse> approveResponse = restTemplate.exchange(
                "http://localhost:" + claimsServicePort + "/api/claims/" + claimNumber + "/status?status=APPROVED",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                ClaimResponse.class
        );
        
        // Verify final status
        assertThat(approveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(approveResponse.getBody()).isNotNull();
        assertThat(approveResponse.getBody().getStatus()).isEqualTo(Claim.ClaimStatus.APPROVED);
        
        // Step 5: Final verification - retrieve the claim one more time
        ResponseEntity<ClaimResponse> finalGetResponse = restTemplate.getForEntity(
                "http://localhost:" + claimsServicePort + "/api/claims/" + claimNumber,
                ClaimResponse.class
        );
        
        assertThat(finalGetResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(finalGetResponse.getBody()).isNotNull();
        assertThat(finalGetResponse.getBody().getStatus()).isEqualTo(Claim.ClaimStatus.APPROVED);
    }
    
    @Test
    @DisplayName("Multiple customers with multiple policies scenario")
    void multipleCustomersMultiplePoliciesScenario() {
        // Customer 1 with 2 different types of claims
        ClaimRequest claim1 = TestDataBuilder.claimRequest()
                .customerId("CUST001")
                .policyNumber("POL001")
                .claimType(Claim.ClaimType.COLLISION)
                .claimAmount(new BigDecimal("4500.00"))
                .incidentLocation("Roundabout collision")
                .build();
        
        ClaimRequest claim2 = TestDataBuilder.claimRequest()
                .customerId("CUST001")
                .policyNumber("POL001")
                .claimType(Claim.ClaimType.THEFT)
                .claimAmount(new BigDecimal("12000.00"))
                .incidentLocation("Shopping center theft")
                .build();
        
        // Customer 2 with different policy
        ClaimRequest claim3 = TestDataBuilder.claimRequest()
                .customerId("CUST002")
                .policyNumber("POL002")
                .claimType(Claim.ClaimType.VANDALISM)
                .claimAmount(new BigDecimal("2500.00"))
                .incidentLocation("Residential area vandalism")
                .build();
        
        // Submit all claims
        ResponseEntity<ClaimResponse> response1 = restTemplate.postForEntity(
                "http://localhost:" + claimsServicePort + "/api/claims", claim1, ClaimResponse.class);
        ResponseEntity<ClaimResponse> response2 = restTemplate.postForEntity(
                "http://localhost:" + claimsServicePort + "/api/claims", claim2, ClaimResponse.class);
        ResponseEntity<ClaimResponse> response3 = restTemplate.postForEntity(
                "http://localhost:" + claimsServicePort + "/api/claims", claim3, ClaimResponse.class);
        
        // Verify all submissions
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response3.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Verify different claim numbers
        assertThat(response1.getBody().getClaimNumber()).isNotEqualTo(response2.getBody().getClaimNumber());
        assertThat(response2.getBody().getClaimNumber()).isNotEqualTo(response3.getBody().getClaimNumber());
        
        // Verify customer and policy associations
        assertThat(response1.getBody().getCustomerId()).isEqualTo("CUST001");
        assertThat(response2.getBody().getCustomerId()).isEqualTo("CUST001");
        assertThat(response3.getBody().getCustomerId()).isEqualTo("CUST002");
        
        assertThat(response1.getBody().getPolicyNumber()).isEqualTo("POL001");
        assertThat(response2.getBody().getPolicyNumber()).isEqualTo("POL001");
        assertThat(response3.getBody().getPolicyNumber()).isEqualTo("POL002");
    }
}