// Simple Single Container Test to debug the issue
package com.aviva.claims.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
//import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
public class SingleContainerTest {
    
    @Container
    static final PostgreSQLContainer<?> mysql = new PostgreSQLContainer<>("postgres:13")  // Changed to 5.7
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withEnv("MYSQL_ROOT_PASSWORD", "root")
            .withStartupTimeout(Duration.ofMinutes(3))
            .withInitScript("init-test.sql");  // We'll create this
    
    @Test
    void testSingleMySQLContainer() throws Exception {
        System.out.println("🐳 Testing single MySQL container...");
        
        // Verify container is running
        assertThat(mysql.isRunning()).isTrue();
        System.out.println("✅ Container is running");
        
        // Get connection details
        String jdbcUrl = mysql.getJdbcUrl();
        String username = mysql.getUsername();
        String password = mysql.getPassword();
        
        System.out.println("📋 Connection Details:");
        System.out.println("   JDBC URL: " + jdbcUrl);
        System.out.println("   Username: " + username);
        System.out.println("   Password: " + password);
        
        // Test direct JDBC connection
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            System.out.println("✅ Successfully connected to MySQL");
            
            // Execute a simple query
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT 1 as test")) {
                
                if (resultSet.next()) {
                    int result = resultSet.getInt("test");
                    assertThat(result).isEqualTo(1);
                    System.out.println("✅ Query executed successfully: " + result);
                }
            }
            
            // Test database creation
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(50))");
                statement.execute("INSERT INTO test_table VALUES (1, 'Test')");
                System.out.println("✅ Table created and data inserted");
                
                try (ResultSet rs = statement.executeQuery("SELECT * FROM test_table")) {
                    if (rs.next()) {
                        System.out.println("✅ Data retrieved: " + rs.getInt("id") + ", " + rs.getString("name"));
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Database connection failed: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        
        System.out.println("🎉 Single container test completed successfully!");
    }
    
    @Test
    void testContainerLogs() {
        System.out.println("📋 MySQL Container Logs:");
        System.out.println(mysql.getLogs());
    }
}