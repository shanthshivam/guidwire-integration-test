// Complete PostgreSQL Base Integration Test
package com.aviva.claims.integration;

import com.aviva.claims.integration.config.TestConfig;
import com.aviva.claims.model.Customer;
import com.aviva.claims.model.Policy;
import com.aviva.claims.model.Claim;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@Import(TestConfig.class)
public abstract class BaseIntegrationTest {
    
    protected static final ObjectMapper objectMapper = new ObjectMapper();
    
    // Static port constants for WireMock services
    protected static final int CUSTOMER_SERVICE_PORT = 8082;
    protected static final int POLICY_SERVICE_PORT = 8083;
    
    // PostgreSQL containers for each service
    protected static final PostgreSQLContainer<?> customerDatabase = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("customer_service")
            .withUsername("test")
            .withPassword("test");
    
    protected static final PostgreSQLContainer<?> policyDatabase = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("policy_service")
            .withUsername("test")
            .withPassword("test");
    
    protected static final PostgreSQLContainer<?> claimsDatabase = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("claims_service")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Configure datasources for the main claims service
        registry.add("spring.datasource.url", claimsDatabase::getJdbcUrl);
        registry.add("spring.datasource.username", claimsDatabase::getUsername);
        registry.add("spring.datasource.password", claimsDatabase::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        
        // Configure JPA for PostgreSQL
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        
        // Configure external service URLs (using static constants)
        registry.add("customer.service.url", () -> "http://localhost:" + CUSTOMER_SERVICE_PORT);
        registry.add("policy.service.url", () -> "http://localhost:" + POLICY_SERVICE_PORT);
    }
    
    @BeforeAll
    static void setupContainers() {
        try {
            System.out.println("🐳 Starting PostgreSQL containers...");
            
            // Start all PostgreSQL database containers
            customerDatabase.start();
            System.out.println("✅ Customer DB started: " + customerDatabase.getJdbcUrl());
            
            policyDatabase.start();  
            System.out.println("✅ Policy DB started: " + policyDatabase.getJdbcUrl());
            
            claimsDatabase.start();
            System.out.println("✅ Claims DB started: " + claimsDatabase.getJdbcUrl());
            
            // Initialize databases with schema and test data
            initializeCustomerDatabase();
            initializePolicyDatabase();
            initializeClaimsDatabase();
            
            System.out.println("🎉 All containers started successfully!");
            
        } catch (Exception e) {
            System.err.println("❌ Failed to start containers: " + e.getMessage());
            // Print container logs for debugging
            if (customerDatabase.isRunning()) {
                System.err.println("Customer DB logs: " + customerDatabase.getLogs());
            }
            if (policyDatabase.isRunning()) {
                System.err.println("Policy DB logs: " + policyDatabase.getLogs());
            }
            if (claimsDatabase.isRunning()) {
                System.err.println("Claims DB logs: " + claimsDatabase.getLogs());
            }
            throw new RuntimeException("Container startup failed", e);
        }
    }
    
    protected static void initializeCustomerDatabase() {
        try (Connection connection = DriverManager.getConnection(
                customerDatabase.getJdbcUrl(),
                customerDatabase.getUsername(),
                customerDatabase.getPassword())) {
            
            Statement statement = connection.createStatement();
            
            // Create customers table with PostgreSQL syntax
            statement.execute("""
                CREATE TABLE customers (
                    id BIGSERIAL PRIMARY KEY,
                    customer_id VARCHAR(255) UNIQUE NOT NULL,
                    first_name VARCHAR(255) NOT NULL,
                    last_name VARCHAR(255) NOT NULL,
                    email VARCHAR(255),
                    phone_number VARCHAR(255),
                    address TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            
            // Insert test data
            statement.execute("""
                INSERT INTO customers (customer_id, first_name, last_name, email, phone_number, address) VALUES
                ('CUST001', 'John', 'Smith', 'john.smith@email.com', '+44-20-1234-5678', '123 Main St, London, UK'),
                ('CUST002', 'Jane', 'Doe', 'jane.doe@email.com', '+44-20-2345-6789', '456 Oak Ave, Manchester, UK'),
                ('CUST003', 'Bob', 'Johnson', 'bob.johnson@email.com', '+44-20-3456-7890', '789 Pine Rd, Birmingham, UK'),
                ('CUST004', 'Alice', 'Wilson', 'alice.wilson@email.com', '+44-20-4567-8901', '321 Elm St, Liverpool, UK')
            """);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize customer database", e);
        }
    }
    
    protected static void initializePolicyDatabase() {
        try (Connection connection = DriverManager.getConnection(
                policyDatabase.getJdbcUrl(),
                policyDatabase.getUsername(),
                policyDatabase.getPassword())) {
            
            Statement statement = connection.createStatement();
            
            // Create policies table with PostgreSQL syntax
            statement.execute("""
                CREATE TABLE policies (
                    id BIGSERIAL PRIMARY KEY,
                    policy_number VARCHAR(255) UNIQUE NOT NULL,
                    customer_id VARCHAR(255) NOT NULL,
                    vehicle_registration VARCHAR(255) NOT NULL,
                    status VARCHAR(50) NOT NULL CHECK (status IN ('ACTIVE', 'EXPIRED', 'CANCELLED', 'SUSPENDED')),
                    start_date DATE NOT NULL,
                    end_date DATE NOT NULL,
                    coverage_amount DECIMAL(15,2) NOT NULL,
                    premium DECIMAL(10,2) NOT NULL,
                    vehicle_make VARCHAR(255),
                    vehicle_model VARCHAR(255),
                    vehicle_year INTEGER,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            
            // Create claims table for policy service with PostgreSQL syntax
            statement.execute("""
                CREATE TABLE claims (
                    id BIGSERIAL PRIMARY KEY,
                    claim_number VARCHAR(255) UNIQUE NOT NULL,
                    customer_id VARCHAR(255) NOT NULL,
                    policy_number VARCHAR(255) NOT NULL,
                    status VARCHAR(50) NOT NULL CHECK (status IN ('SUBMITTED', 'PENDING', 'INVESTIGATING', 'APPROVED', 'REJECTED', 'PAID', 'CLOSED')),
                    claim_type VARCHAR(50) NOT NULL CHECK (claim_type IN ('COLLISION', 'COMPREHENSIVE', 'LIABILITY', 'PERSONAL_INJURY', 'THEFT', 'VANDALISM')),
                    claim_amount DECIMAL(15,2),
                    description TEXT,
                    incident_date TIMESTAMP,
                    incident_location VARCHAR(500),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            
            // Insert test policies
            statement.execute("""
                INSERT INTO policies (policy_number, customer_id, vehicle_registration, status, start_date, end_date, 
                                    coverage_amount, premium, vehicle_make, vehicle_model, vehicle_year) VALUES
                ('POL001', 'CUST001', 'ABC123', 'ACTIVE', '2024-01-01', '2025-01-01', 50000.00, 1200.00, 'Ford', 'Focus', 2020),
                ('POL002', 'CUST002', 'DEF456', 'ACTIVE', '2024-02-01', '2025-02-01', 75000.00, 1500.00, 'BMW', '3 Series', 2021),
                ('POL003', 'CUST003', 'GHI789', 'EXPIRED', '2023-01-01', '2024-01-01', 30000.00, 800.00, 'Toyota', 'Corolla', 2019),
                ('POL004', 'CUST004', 'JKL012', 'ACTIVE', '2024-03-01', '2025-03-01', 60000.00, 1300.00, 'Mercedes', 'A-Class', 2022),
                ('POL005', 'CUST001', 'MNO345', 'ACTIVE', '2024-04-01', '2025-04-01', 45000.00, 1100.00, 'Audi', 'A3', 2020)
            """);
            
            // Insert some existing claims for testing duplicate detection
            statement.execute("""
                INSERT INTO claims (claim_number, customer_id, policy_number, status, claim_type, claim_amount, 
                                  description, incident_date, incident_location) VALUES
                ('CLM-EXIST01', 'CUST001', 'POL001', 'APPROVED', 'COLLISION', 5000.00, 
                 'Rear-end collision', '2024-06-15 14:30:00', 'M25 Junction 10'),
                ('CLM-EXIST02', 'CUST002', 'POL002', 'PENDING', 'THEFT', 15000.00, 
                 'Vehicle stolen from parking lot', '2024-07-20 09:00:00', 'Westfield Shopping Center'),
                ('CLM-EXIST03', 'CUST004', 'POL004', 'INVESTIGATING', 'VANDALISM', 2500.00, 
                 'Windows smashed and paint scratched', '2024-08-05 22:15:00', 'Birmingham City Center')
            """);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize policy database", e);
        }
    }
    
    protected static void initializeClaimsDatabase() {
        try (Connection connection = DriverManager.getConnection(
                claimsDatabase.getJdbcUrl(),
                claimsDatabase.getUsername(),
                claimsDatabase.getPassword())) {
            
            Statement statement = connection.createStatement();
            
            // Create claims table for main claims service with PostgreSQL syntax
            statement.execute("""
                CREATE TABLE claims (
                    id BIGSERIAL PRIMARY KEY,
                    claim_number VARCHAR(255) UNIQUE NOT NULL,
                    customer_id VARCHAR(255) NOT NULL,
                    policy_number VARCHAR(255) NOT NULL,
                    status VARCHAR(50) NOT NULL CHECK (status IN ('SUBMITTED', 'PENDING', 'INVESTIGATING', 'APPROVED', 'REJECTED', 'PAID', 'CLOSED')),
                    claim_type VARCHAR(50) NOT NULL CHECK (claim_type IN ('COLLISION', 'COMPREHENSIVE', 'LIABILITY', 'PERSONAL_INJURY', 'THEFT', 'VANDALISM')),
                    claim_amount DECIMAL(15,2),
                    description TEXT,
                    incident_date TIMESTAMP,
                    incident_location VARCHAR(500),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize claims database", e);
        }
    }
    
    // Helper methods for getting service ports (now just return the static constants)
    protected int getCustomerServicePort() {
        return CUSTOMER_SERVICE_PORT;
    }
    
    protected int getPolicyServicePort() {
        return POLICY_SERVICE_PORT;
    }
    
    // Test data creation helpers
    protected Customer createTestCustomer(String customerId, String firstName, String lastName) {
        return new Customer(customerId, firstName, lastName, 
                           firstName.toLowerCase() + "." + lastName.toLowerCase() + "@test.com",
                           "+44-20-1111-2222", "Test Address, London, UK");
    }
    
    protected Policy createTestPolicy(String policyNumber, String customerId, String vehicleReg) {
        return new Policy(policyNumber, customerId, vehicleReg,
                         LocalDate.now().minusMonths(6), LocalDate.now().plusMonths(6),
                         new BigDecimal("50000.00"), new BigDecimal("1200.00"),
                         "Ford", "Focus", 2020);
    }
    
    protected Claim createTestClaim(String claimNumber, String customerId, String policyNumber) {
        return new Claim(claimNumber, customerId, policyNumber, Claim.ClaimType.COLLISION,
                        new BigDecimal("5000.00"), "Test collision claim",
                        LocalDateTime.now().minusDays(7), "Test Location");
    }
}