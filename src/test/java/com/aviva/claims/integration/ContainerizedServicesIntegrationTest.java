// Integration Test with Real Containerized Microservices
package com.aviva.claims.integration;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ContainerizedServicesIntegrationTest {
    
    @LocalServerPort
    private int claimsServicePort;
    
    private final TestRestTemplate restTemplate = new TestRestTemplate();
    
    // Shared network for all containers
    static Network testNetwork = Network.newNetwork();
    
    // Database containers
    static PostgreSQLContainer<?> customerDB = new PostgreSQLContainer<>("postgres:13")
            .withNetwork(testNetwork)
            .withNetworkAliases("customer-db")
            .withDatabaseName("customer_service")
            .withUsername("test")
            .withPassword("test");
    
    static PostgreSQLContainer<?> policyDB = new PostgreSQLContainer<>("postgres:13")
            .withNetwork(testNetwork)
            .withNetworkAliases("policy-db")
            .withDatabaseName("policy_service")
            .withUsername("test")
            .withPassword("test");
    
    static PostgreSQLContainer<?> claimsDB = new PostgreSQLContainer<>("postgres:13")
            .withNetwork(testNetwork)
            .withNetworkAliases("claims-db")
            .withDatabaseName("claims_service")
            .withUsername("test")
            .withPassword("test");
    
    // Microservice containers (assuming you have Docker images)
    static GenericContainer<?> customerService = new GenericContainer<>("aviva/customer-service:test")
            .withNetwork(testNetwork)
            .withNetworkAliases("customer-service")
            .withExposedPorts(8080)
            .withEnv("SPRING_DATASOURCE_URL", "jdbc:postgresql://customer-db:5432/customer_service")
            .withEnv("SPRING_DATASOURCE_USERNAME", "test")
            .withEnv("SPRING_DATASOURCE_PASSWORD", "test")
            .withEnv("SPRING_PROFILES_ACTIVE", "test")
            .waitingFor(Wait.forHttp("/actuator/health").withStartupTimeout(Duration.ofMinutes(2)))
            .dependsOn(customerDB);
    
    static GenericContainer<?> policyService = new GenericContainer<>("aviva/policy-service:test")
            .withNetwork(testNetwork)
            .withNetworkAliases("policy-service")
            .withExposedPorts(8080)
            .withEnv("SPRING_DATASOURCE_URL", "jdbc:postgresql://policy-db:5432/policy_service")
            .withEnv("SPRING_DATASOURCE_USERNAME", "test")
            .withEnv("SPRING_DATASOURCE_PASSWORD", "test")
            .withEnv("SPRING_PROFILES_ACTIVE", "test")
            .waitingFor(Wait.forHttp("/actuator/health").withStartupTimeout(Duration.ofMinutes(2)))
            .dependsOn(policyDB);
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Configure main claims service database
        registry.add("spring.datasource.url", claimsDB::getJdbcUrl);
        registry.add("spring.datasource.username", claimsDB::getUsername);
        registry.add("spring.datasource.password", claimsDB::getPassword);
        
        // Configure external service URLs to point to containerized services
        registry.add("customer.service.url", () -> 
            "http://localhost:" + customerService.getFirstMappedPort());
        registry.add("policy.service.url", () -> 
            "http://localhost:" + policyService.getFirstMappedPort());
    }
    
    @BeforeAll
    static void startInfrastructure() {
        System.out.println("Starting microservices infrastructure...");
        
        // Start databases first
        customerDB.start();
        policyDB.start();
        claimsDB.start();
        
        // Start microservices (they depend on databases)
        customerService.start();
        policyService.start();
        
        System.out.println("Customer Service: http://localhost:" + customerService.getFirstMappedPort());
        System.out.println("Policy Service: http://localhost:" + policyService.getFirstMappedPort());
        System.out.println("All services started successfully!");
    }
    
    @Test
    void shouldIntegrateWithRealMicroservices() {
        // Test with real microservices - no mocking needed!
        
        // Your test logic here - same as before but now calling real services
        // The claims service will make real HTTP calls to containerized customer and policy services
        
        System.out.println("Testing integration with real containerized microservices");
        
        // Example: Submit a claim that will trigger real service calls
        String claimRequest = """
            {
                "customerId": "CUST001",
                "policyNumber": "POL001",
                "claimType": "COLLISION",
                "claimAmount": 5000.00,
                "description": "Real microservice integration test"
            }
            """;
        
        var response = restTemplate.postForEntity(
            "http://localhost:" + claimsServicePort + "/api/claims",
            claimRequest,
            String.class
        );
        
        System.out.println("Claims service response: " + response.getBody());
    }
}