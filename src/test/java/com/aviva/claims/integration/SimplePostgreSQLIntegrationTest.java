// Simple PostgreSQL TestContainers Integration Test (No Ryuk)
package com.aviva.claims.integration;

import com.aviva.claims.dto.ClaimRequest;
import com.aviva.claims.dto.ClaimResponse;
import com.aviva.claims.model.Claim;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SimplePostgreSQLIntegrationTest {
    
    @LocalServerPort
    private int claimsServicePort;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    // Simple PostgreSQL container (no network complexity)
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("claims_service")
            .withUsername("test")
            .withPassword("test");
    
    private static WireMockServer customerServiceMock;
    private static WireMockServer policyServiceMock;
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Configure PostgreSQL for the main claims service
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        
        // Configure JPA for PostgreSQL
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        
        // Configure external service URLs
        registry.add("customer.service.url", () -> "http://localhost:8081");
        registry.add("policy.service.url", () -> "http://localhost:8082");
    }
    
    @BeforeAll
    static void setup() {
        System.out.println("🐳 Starting PostgreSQL container (no Ryuk)...");
        
        // Start PostgreSQL container
        postgres.start();
        System.out.println("✅ PostgreSQL started: " + postgres.getJdbcUrl());
        
        // Start WireMock servers
        customerServiceMock = new WireMockServer(WireMockConfiguration.options().port(8081));
        policyServiceMock = new WireMockServer(WireMockConfiguration.options().port(8082));
        
        customerServiceMock.start();
        policyServiceMock.start();
        
        System.out.println("✅ WireMock services started");
        
        setupMocks();
    }
    
    @AfterAll
    static void teardown() {
        if (customerServiceMock != null) {
            customerServiceMock.stop();
        }
        if (policyServiceMock != null) {
            policyServiceMock.stop();
        }
        if (postgres != null) {
            postgres.stop();
        }
        System.out.println("🛑 All services stopped");
    }
    
    static void setupMocks() {
        // Customer Service Mocks
        customerServiceMock.stubFor(get(urlEqualTo("/api/customers/CUST001/exists"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));
        
        customerServiceMock.stubFor(get(urlEqualTo("/api/customers/NONEXISTENT/exists"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("false")));
        
        // Policy Service Mocks
        policyServiceMock.stubFor(get(urlMatching("/api/policies/POL001/validate\\?customerId=CUST001"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(createPolicyValidationResponse(true, "Policy is valid"))));
        
        policyServiceMock.stubFor(get(urlMatching("/api/policies/INVALID/validate\\?customerId=.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(createPolicyValidationResponse(false, "Policy not found"))));
        
        // Claim Validation Mocks
        policyServiceMock.stubFor(get(urlMatching("/api/policies/.*/claims/validate\\?.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(createClaimValidationResponse(false, "No duplicates"))));
    }
    
    private static String createPolicyValidationResponse(boolean valid, String message) {
        return String.format("""
            {
                "valid": %s,
                "policyNumber": "POL001",
                "customerId": "CUST001",
                "active": %s,
                "validationMessage": "%s"
            }
            """, valid, valid, message);
    }
    
    private static String createClaimValidationResponse(boolean isDuplicate, String message) {
        return String.format("""
            {
                "duplicate": %s,
                "hasExistingClaims": false,
                "existingClaims": [],
                "validationMessage": "%s"
            }
            """, isDuplicate, message);
    }
    
    @Test
    @Order(1)
    @DisplayName("Should successfully process a valid claim with PostgreSQL")
    void shouldProcessValidClaim() {
        System.out.println("🧪 Testing valid claim processing...");
        
        // Given
        ClaimRequest claimRequest = new ClaimRequest(
                "CUST001",
                "POL001",
                Claim.ClaimType.COLLISION,
                new BigDecimal("5000.00"),
                "Test collision claim",
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
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getClaimNumber()).startsWith("CLM-");
        assertThat(response.getBody().getCustomerId()).isEqualTo("CUST001");
        assertThat(response.getBody().getPolicyNumber()).isEqualTo("POL001");
        assertThat(response.getBody().getStatus()).isEqualTo(Claim.ClaimStatus.PENDING);
        
        System.out.println("✅ Claim processed successfully: " + response.getBody().getClaimNumber());
    }
    
    @Test
    @Order(2)
    @DisplayName("Should reject claim for non-existent customer")
    void shouldRejectInvalidCustomer() {
        System.out.println("🧪 Testing invalid customer rejection...");
        
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
        System.out.println("✅ Invalid customer correctly rejected");
    }
    
    @Test
    @Order(3)
    @DisplayName("Should reject claim for invalid policy")
    void shouldRejectInvalidPolicy() {
        System.out.println("🧪 Testing invalid policy rejection...");
        
        // Given
        ClaimRequest claimRequest = new ClaimRequest(
                "CUST001",
                "INVALID",
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
        System.out.println("✅ Invalid policy correctly rejected");
    }
    
    @Test
    @Order(4)
    @DisplayName("Should reject high-value claims")
    void shouldRejectHighValueClaims() {
        System.out.println("🧪 Testing high-value claim rejection...");
        
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
        System.out.println("✅ High-value claim correctly rejected");
    }
    
    @BeforeEach
    void resetMocks() {
        customerServiceMock.resetRequests();
        policyServiceMock.resetRequests();
    }
}