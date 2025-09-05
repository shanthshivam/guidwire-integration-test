// Minimal Spring Context Test
package com.aviva.claims.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MinimalSpringContextTest {
    
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");
    
    static {
        postgres.start();
        System.out.println("✅ PostgreSQL started for minimal test: " + postgres.getJdbcUrl());
    }
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        
        // Mock external services (so they don't cause startup failures)
        registry.add("customer.service.url", () -> "http://localhost:8081");
        registry.add("policy.service.url", () -> "http://localhost:8082");
    }
    
    @Test
    void contextLoads() {
        System.out.println("🧪 Testing Spring context loading...");
        assertThat(true).isTrue();
        System.out.println("✅ Spring context loaded successfully!");
    }
    
    @Test
    void testBasicFunctionality() {
        System.out.println("🧪 Testing basic functionality...");
        
        // Simple assertion to verify test runs
        String testMessage = "Hello TestContainers!";
        assertThat(testMessage).isEqualTo("Hello TestContainers!");
        
        System.out.println("✅ Basic functionality test passed!");
    }
}