// Simple Container-Only Test (No Spring Context)
package com.aviva.claims.integration;

import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SimpleContainerOnlyTest {
    
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");
    
    @BeforeAll
    static void setup() {
        System.out.println("🐳 Starting PostgreSQL container for container-only test...");
        postgres.start();
        System.out.println("✅ PostgreSQL started: " + postgres.getJdbcUrl());
        
        // Create test table
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE test_claims (id SERIAL PRIMARY KEY, claim_number VARCHAR(50), amount DECIMAL(10,2))");
            stmt.execute("INSERT INTO test_claims (claim_number, amount) VALUES ('CLM-001', 5000.00)");
            System.out.println("✅ Test table created and data inserted");
            
        } catch (Exception e) {
            throw new RuntimeException("Database setup failed", e);
        }
    }
    
    @Test
    @Order(1)
    @DisplayName("Should connect to PostgreSQL and query data")
    void shouldConnectAndQueryData() {
        System.out.println("🧪 Testing PostgreSQL connection and query...");
        
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM test_claims WHERE claim_number = 'CLM-001'");
            
            if (rs.next()) {
                String claimNumber = rs.getString("claim_number");
                double amount = rs.getDouble("amount");
                
                System.out.println("✅ Successfully retrieved: " + claimNumber + " - £" + amount);
                
                // Simple assertions
                if (!"CLM-001".equals(claimNumber)) {
                    throw new AssertionError("Expected CLM-001, got " + claimNumber);
                }
                if (amount != 5000.00) {
                    throw new AssertionError("Expected 5000.00, got " + amount);
                }
            } else {
                throw new AssertionError("No data found");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
        
        System.out.println("✅ Container-only test passed!");
    }
    
    @Test
    @Order(2)
    @DisplayName("Should insert new data")
    void shouldInsertNewData() {
        System.out.println("🧪 Testing data insertion...");
        
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            
            Statement stmt = conn.createStatement();
            stmt.execute("INSERT INTO test_claims (claim_number, amount) VALUES ('CLM-002', 7500.00)");
            
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM test_claims");
            rs.next();
            int count = rs.getInt("count");
            
            if (count != 2) {
                throw new AssertionError("Expected 2 records, got " + count);
            }
            
            System.out.println("✅ Data insertion test passed! Total records: " + count);
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    @AfterAll
    static void cleanup() {
        if (postgres != null) {
            postgres.stop();
            System.out.println("🛑 PostgreSQL container stopped");
        }
    }
}