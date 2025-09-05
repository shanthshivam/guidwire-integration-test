// WireMock Service Simulator
package com.aviva.claims.integration;

import com.aviva.claims.dto.ClaimRequest;
import com.aviva.claims.dto.ClaimResponse;
import com.aviva.claims.model.Claim;
import com.aviva.claims.model.Customer;
import com.aviva.claims.model.Policy;
import com.aviva.policy.dto.ClaimValidationResponse;
import com.aviva.policy.dto.PolicyValidationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AutoClaimsIntegrationTest extends BaseIntegrationTest {
    
    @LocalServerPort
    private int claimsServicePort;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    private static WireMockServer customerServiceMock;
    private static WireMockServer policyServiceMock;
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    
    @BeforeAll
    static void setupWireMock() {
        // Start WireMock servers for external services
        customerServiceMock = new WireMockServer(WireMockConfiguration.options().port(8082));
        policyServiceMock = new WireMockServer(WireMockConfiguration.options().port(8083));
        
        customerServiceMock.start();
        policyServiceMock.start();
        
        setupCustomerServiceMocks();
        setupPolicyServiceMocks();
    }
    
    @AfterAll
    static void tearDownWireMock() {
        if (customerServiceMock != null) {
            customerServiceMock.stop();
        }
        if (policyServiceMock != null) {
            policyServiceMock.stop();
        }
    }
    
    @Override
    protected int getCustomerServicePort() {
        return 8082;
    }
    
    @Override
    protected int getPolicyServicePort() {
        return 8083;
    }
    
    private static void setupCustomerServiceMocks() {
        // Mock successful customer lookup
        customerServiceMock.stubFor(get(urlEqualTo("/api/customers/CUST001"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(createCustomerJson("CUST001", "John", "Smith"))));
        
        customerServiceMock.stubFor(get(urlEqualTo("/api/customers/CUST002"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(createCustomerJson("CUST002", "Jane", "Doe"))));
        
        // Mock customer not found
        customerServiceMock.stubFor(get(urlEqualTo("/api/customers/NONEXISTENT"))
                .willReturn(aResponse().withStatus(404)));
        
        // Mock customer existence checks
        customerServiceMock.stubFor(get(urlEqualTo("/api/customers/CUST001/exists"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));
        
        customerServiceMock.stubFor(get(urlEqualTo("/api/customers/CUST002/exists"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));
        
        customerServiceMock.stubFor(get(urlEqualTo("/api/customers/NONEXISTENT/exists"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("false")));
    }
    
    private static void setupPolicyServiceMocks() {
        // Mock successful policy validation
        policyServiceMock.stubFor(get(urlMatching("/api/policies/POL001/validate\\?customerId=CUST001"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(createPolicyValidationJson(true, "POL001", "CUST001", true, "Policy is valid for claims"))));
        
        policyServiceMock.stubFor(get(urlMatching("/api/policies/POL002/validate\\?customerId=CUST002"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(createPolicyValidationJson(true, "POL002", "CUST002", true, "Policy is valid for claims"))));
        
        // Mock policy not found
        policyServiceMock.stubFor(get(urlMatching("/api/policies/NONEXISTENT/validate\\?customerId=.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(createPolicyValidationJson(false, "NONEXISTENT", "CUST001", false, "Policy not found"))));
        
        // Mock expired policy
        policyServiceMock.stubFor(get(urlMatching("/api/policies/POL003/validate\\?customerId=CUST003"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(createPolicyValidationJson(false, "POL003", "CUST003", false, "Policy is not active or expired"))));
        
        // Mock claim validation - no duplicates
        policyServiceMock.stubFor(get(urlMatching("/api/policies/POL001/claims/validate\\?customerId=CUST001&incidentLocation=New Location&incidentDate=.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(createClaimValidationJson(false, false, "No duplicate claims found"))));
        
        policyServiceMock.stubFor(get(urlMatching("/api/policies/POL002/claims/validate\\?customerId=CUST002&incidentLocation=New Location&incidentDate=.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(createClaimValidationJson(false, false, "No duplicate claims found"))));
        
        // Mock duplicate claim detection
        policyServiceMock.stubFor(get(urlMatching("/api/policies/POL001/claims/validate\\?customerId=CUST001&incidentLocation=M25 Junction 10&incidentDate=2024-06-15T14:30:00"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(createClaimValidationJson(true, true, "Duplicate claim detected"))));
    }
    
    private static String createCustomerJson(String customerId, String firstName, String lastName) {
        try {
            Customer customer = new Customer(customerId, firstName, lastName, 
                firstName.toLowerCase() + "." + lastName.toLowerCase() + "@test.com",
                "+44-20-1234-5678", "Test Address, London, UK");
            return objectMapper.writeValueAsString(customer);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create customer JSON", e);
        }
    }
    
    private static String createPolicyValidationJson(boolean valid, String policyNumber, 
                                                   String customerId, boolean active, String message) {
        try {
            PolicyValidationResponse response = new PolicyValidationResponse(valid, policyNumber, customerId, active, message);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create policy validation JSON", e);
        }
    }
    
    private static String createClaimValidationJson(boolean isDuplicate, boolean hasExistingClaims, String message) {
        try {
            ClaimValidationResponse response = new ClaimValidationResponse(isDuplicate, hasExistingClaims, Collections.emptyList(), message);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create claim validation JSON", e);
        }
    }
    
    @Test
    @Order(1)
    @DisplayName("Should successfully process a valid claim")
    void shouldProcessValidClaim() {
        // Given
        ClaimRequest claimRequest = new ClaimRequest(
                "CUST001",
                "POL001",
                Claim.ClaimType.COLLISION,
                new BigDecimal("5000.00"),
                "Minor collision at roundabout",
                LocalDateTime.now().minusDays(1),
                "New Location"
        );
        
        // When
        ResponseEntity<ClaimResponse> response = restTemplate.postForEntity(
                "http://localhost:" + claimsServicePort + "/api/claims",
                claimRequest,
                ClaimResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getClaimNumber()).startsWith("CLM-");
        assertThat(response.getBody().getCustomerId()).isEqualTo("CUST001");
        assertThat(response.getBody().getPolicyNumber()).isEqualTo("POL001");
        assertThat(response.getBody().getStatus()).isEqualTo(Claim.ClaimStatus.PENDING);
        assertThat(response.getBody().getClaimAmount()).isEqualTo(new BigDecimal("5000.00"));
        
        // Verify external service calls were made
        customerServiceMock.verify(getRequestedFor(urlEqualTo("/api/customers/CUST001/exists")));
        policyServiceMock.verify(getRequestedFor(urlMatching("/api/policies/POL001/validate\\?customerId=CUST001")));
    }
    
    @Test
    @Order(2)
    @DisplayName("Should reject claim for non-existent customer")
    void shouldRejectClaimForNonExistentCustomer() {
        // Given
        ClaimRequest claimRequest = new ClaimRequest(
                "NONEXISTENT",
                "POL001",
                Claim.ClaimType.COLLISION,
                new BigDecimal("3000.00"),
                "Test claim",
                LocalDateTime.now().minusDays(1),
                "Test Location"
        );
        
        // When
        ResponseEntity<ClaimResponse> response = restTemplate.postForEntity(
                "http://localhost:" + claimsServicePort + "/api/claims",
                claimRequest,
                ClaimResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        
        // Verify customer service was called
        customerServiceMock.verify(getRequestedFor(urlEqualTo("/api/customers/NONEXISTENT/exists")));
    }
    
    @Test
    @Order(3)
    @DisplayName("Should reject claim for invalid policy")
    void shouldRejectClaimForInvalidPolicy() {
        // Given
        ClaimRequest claimRequest = new ClaimRequest(
                "CUST001",
                "NONEXISTENT",
                Claim.ClaimType.COLLISION,
                new BigDecimal("4000.00"),
                "Test claim",
                LocalDateTime.now().minusDays(1),
                "Test Location"
        );
        
        // When
        ResponseEntity<ClaimResponse> response = restTemplate.postForEntity(
                "http://localhost:" + claimsServicePort + "/api/claims",
                claimRequest,
                ClaimResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        
        // Verify services were called in correct order
        customerServiceMock.verify(getRequestedFor(urlEqualTo("/api/customers/CUST001/exists")));
        policyServiceMock.verify(getRequestedFor(urlMatching("/api/policies/NONEXISTENT/validate\\?customerId=CUST001")));
    }
    
    @Test
    @Order(4)
    @DisplayName("Should reject claim for expired policy")
    void shouldRejectClaimForExpiredPolicy() {
        // Given
        ClaimRequest claimRequest = new ClaimRequest(
                "CUST003",
                "POL003",
                Claim.ClaimType.COLLISION,
                new BigDecimal("2000.00"),
                "Test claim",
                LocalDateTime.now().minusDays(1),
                "Test Location"
        );
        
        // When
        ResponseEntity<ClaimResponse> response = restTemplate.postForEntity(
                "http://localhost:" + claimsServicePort + "/api/claims",
                claimRequest,
                ClaimResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
    
    @Test
    @Order(5)
    @DisplayName("Should reject duplicate claim")
    void shouldRejectDuplicateClaim() {
        // Given - using exact same incident details as existing claim
        ClaimRequest claimRequest = new ClaimRequest(
                "CUST001",
                "POL001",
                Claim.ClaimType.COLLISION,
                new BigDecimal("5000.00"),
                "Duplicate claim test",
                LocalDateTime.parse("2024-06-15T14:30:00"),
                "M25 Junction 10"
        );
        
        // When
        ResponseEntity<ClaimResponse> response = restTemplate.postForEntity(
                "http://localhost:" + claimsServicePort + "/api/claims",
                claimRequest,
                ClaimResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        
        // Verify duplicate detection was called
        policyServiceMock.verify(getRequestedFor(urlMatching("/api/policies/POL001/claims/validate.*incidentLocation=M25%20Junction%2010.*")));
    }
    
    @Test
    @Order(6)
    @DisplayName("Should reject claim with amount exceeding limit")
    void shouldRejectClaimExceedingLimit() {
        // Given
        ClaimRequest claimRequest = new ClaimRequest(
                "CUST001",
                "POL001",
                Claim.ClaimType.COLLISION,
                new BigDecimal("150000.00"), // Exceeds £100,000 limit
                "High value claim",
                LocalDateTime.now().minusDays(1),
                "Test Location"
        );
        
        // When
        ResponseEntity<ClaimResponse> response = restTemplate.postForEntity(
                "http://localhost:" + claimsServicePort + "/api/claims",
                claimRequest,
                ClaimResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
    
    @Test
    @Order(7)
    @DisplayName("Should reject claim with future incident date")
    void shouldRejectClaimWithFutureIncidentDate() {
        // Given
        ClaimRequest claimRequest = new ClaimRequest(
                "CUST001",
                "POL001",
                Claim.ClaimType.COLLISION,
                new BigDecimal("3000.00"),
                "Future incident claim",
                LocalDateTime.now().plusDays(1), // Future date
                "Test Location"
        );
        
        // When
        ResponseEntity<ClaimResponse> response = restTemplate.postForEntity(
                "http://localhost:" + claimsServicePort + "/api/claims",
                claimRequest,
                ClaimResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
    
    @Test
    @Order(8)
    @DisplayName("Should reject claim with very old incident date")
    void shouldRejectClaimWithVeryOldIncidentDate() {
        // Given
        ClaimRequest claimRequest = new ClaimRequest(
                "CUST001",
                "POL001",
                Claim.ClaimType.COLLISION,
                new BigDecimal("3000.00"),
                "Very old incident claim",
                LocalDateTime.now().minusYears(2), // More than 1 year old
                "Test Location"
        );
        
        // When
        ResponseEntity<ClaimResponse> response = restTemplate.postForEntity(
                "http://localhost:" + claimsServicePort + "/api/claims",
                claimRequest,
                ClaimResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
    
    @Test
    @Order(9)
    @DisplayName("Should process multiple valid claims from different customers")
    void shouldProcessMultipleValidClaims() {
        // Given
        ClaimRequest claim1 = new ClaimRequest(
                "CUST001",
                "POL001",
                Claim.ClaimType.THEFT,
                new BigDecimal("8000.00"),
                "Vehicle theft claim",
                LocalDateTime.now().minusDays(2),
                "Shopping Center Parking"
        );
        
        ClaimRequest claim2 = new ClaimRequest(
                "CUST002",
                "POL002",
                Claim.ClaimType.VANDALISM,
                new BigDecimal("1500.00"),
                "Vehicle vandalism claim",
                LocalDateTime.now().minusDays(3),
                "Residential Street"
        );
        
        // When
        ResponseEntity<ClaimResponse> response1 = restTemplate.postForEntity(
                "http://localhost:" + claimsServicePort + "/api/claims",
                claim1,
                ClaimResponse.class
        );
        
        ResponseEntity<ClaimResponse> response2 = restTemplate.postForEntity(
                "http://localhost:" + claimsServicePort + "/api/claims",
                claim2,
                ClaimResponse.class
        );
        
        // Then
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        assertThat(response1.getBody().getCustomerId()).isEqualTo("CUST001");
        assertThat(response1.getBody().getClaimType()).isEqualTo(Claim.ClaimType.THEFT);
        
        assertThat(response2.getBody().getCustomerId()).isEqualTo("CUST002");
        assertThat(response2.getBody().getClaimType()).isEqualTo(Claim.ClaimType.VANDALISM);
        
        // Verify different claim numbers were generated
        assertThat(response1.getBody().getClaimNumber()).isNotEqualTo(response2.getBody().getClaimNumber());
    }
    
    @Test
    @Order(10)
    @DisplayName("Should handle service unavailability gracefully")
    void shouldHandleServiceUnavailabilityGracefully() {
        // Given - stop the customer service mock temporarily
        customerServiceMock.stop();
        
        ClaimRequest claimRequest = new ClaimRequest(
                "CUST001",
                "POL001",
                Claim.ClaimType.COLLISION,
                new BigDecimal("3000.00"),
                "Service unavailable test",
                LocalDateTime.now().minusDays(1),
                "Test Location"
        );
        
        // When
        ResponseEntity<ClaimResponse> response = restTemplate.postForEntity(
                "http://localhost:" + claimsServicePort + "/api/claims",
                claimRequest,
                ClaimResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        
        // Restart the service for other tests
        customerServiceMock.start();
        setupCustomerServiceMocks();
    }
    
    @BeforeEach
    void setup() {
        // Reset WireMock request logs before each test
        customerServiceMock.resetRequests();
        policyServiceMock.resetRequests();
    }
}