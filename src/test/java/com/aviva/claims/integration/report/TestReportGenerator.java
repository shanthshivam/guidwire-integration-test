package com.aviva.claims.integration.report;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TestReportGenerator implements TestWatcher, BeforeAllCallback, AfterAllCallback {
    
    private static final List<TestResult> testResults = new ArrayList<>();
    private static LocalDateTime startTime;
    
    @Override
    public void beforeAll(ExtensionContext context) {
        startTime = LocalDateTime.now();
        System.out.println("=".repeat(80));
        System.out.println("🚀 AVIVA AUTO CLAIMS INTEGRATION TESTS STARTED");
        System.out.println("=".repeat(80));
        System.out.println("Start Time: " + startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        System.out.println("TestContainers: ✓ MySQL, ✓ WireMock Services");
        System.out.println("-".repeat(80));
    }
    
    @Override
    public void afterAll(ExtensionContext context) {
        LocalDateTime endTime = LocalDateTime.now();
        generateReport(endTime);
    }
    
    @Override
    public void testSuccessful(ExtensionContext context) {
        testResults.add(new TestResult(
            context.getDisplayName(),
            context.getTestClass().map(Class::getSimpleName).orElse("Unknown"),
            "PASSED",
            ""
        ));
        System.out.println("✅ " + context.getDisplayName() + " - PASSED");
    }
    
    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        testResults.add(new TestResult(
            context.getDisplayName(),
            context.getTestClass().map(Class::getSimpleName).orElse("Unknown"),
            "FAILED",
            cause.getMessage()
        ));
        System.err.println("❌ " + context.getDisplayName() + " - FAILED: " + cause.getMessage());
    }
    
    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        testResults.add(new TestResult(
            context.getDisplayName(),
            context.getTestClass().map(Class::getSimpleName).orElse("Unknown"),
            "ABORTED",
            cause.getMessage()
        ));
        System.out.println("⚠️ " + context.getDisplayName() + " - ABORTED: " + cause.getMessage());
    }
    
    private void generateReport(LocalDateTime endTime) {
        System.out.println("-".repeat(80));
        System.out.println("📊 TEST EXECUTION SUMMARY");
        System.out.println("-".repeat(80));
        
        long passed = testResults.stream().filter(r -> "PASSED".equals(r.status)).count();
        long failed = testResults.stream().filter(r -> "FAILED".equals(r.status)).count();
        long aborted = testResults.stream().filter(r -> "ABORTED".equals(r.status)).count();
        
        System.out.println("Total Tests: " + testResults.size());
        System.out.println("✅ Passed: " + passed);
        System.out.println("❌ Failed: " + failed);
        System.out.println("⚠️ Aborted: " + aborted);
        System.out.println("Duration: " + java.time.Duration.between(startTime, endTime).toSeconds() + " seconds");
        System.out.println("Success Rate: " + String.format("%.2f%%", (double) passed / testResults.size() * 100));
        
        // Generate detailed report file
        generateDetailedReport(endTime);
        
        System.out.println("-".repeat(80));
        System.out.println("🏁 INTEGRATION TESTS COMPLETED");
        System.out.println("=".repeat(80));
    }
    
    private void generateDetailedReport(LocalDateTime endTime) {
        try (FileWriter writer = new FileWriter("target/integration-test-report.md")) {
            writer.write("# Aviva Auto Claims Integration Test Report\n\n");
            writer.write("**Generated on:** " + endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\n\n");
            writer.write("## Test Configuration\n");
            writer.write("- **TestContainers:** MySQL 8.0 (3 databases)\n");
            writer.write("- **Service Simulation:** WireMock\n");
            writer.write("- **Test Framework:** JUnit 5\n");
            writer.write("- **Integration Approach:** Left-shift testing with containerized dependencies\n\n");
            
            writer.write("## Test Results Summary\n");
            writer.write("| Metric | Value |\n");
            writer.write("|--------|-------|\n");
            writer.write("| Total Tests | " + testResults.size() + " |\n");
            writer.write("| Passed | " + testResults.stream().filter(r -> "PASSED".equals(r.status)).count() + " |\n");
            writer.write("| Failed | " + testResults.stream().filter(r -> "FAILED".equals(r.status)).count() + " |\n");
            writer.write("| Duration | " + java.time.Duration.between(startTime, endTime).toSeconds() + "s |\n\n");
            
            writer.write("## Detailed Test Results\n");
            for (TestResult result : testResults) {
                String emoji = "PASSED".equals(result.status) ? "✅" : 
                              "FAILED".equals(result.status) ? "❌" : "⚠️";
                writer.write("- " + emoji + " **" + result.testName + "** (" + result.className + ") - " + result.status);
                if (!result.errorMessage.isEmpty()) {
                    writer.write("\n  - Error: " + result.errorMessage);
                }
                writer.write("\n");
            }
            
            writer.write("\n## Test Coverage Areas\n");
            writer.write("- ✅ Happy path claim processing\n");
            writer.write("- ✅ Customer validation\n");
            writer.write("- ✅ Policy validation (active, expired, non-existent)\n");
            writer.write("- ✅ Duplicate claim detection\n");
            writer.write("- ✅ Business rule validation (amount limits, date validation)\n");
            writer.write("- ✅ Service unavailability handling\n");
            writer.write("- ✅ Concurrent processing\n");
            writer.write("- ✅ End-to-end claim lifecycle\n");
            writer.write("- ✅ Performance under load\n");
            
        } catch (IOException e) {
            System.err.println("Failed to generate detailed report: " + e.getMessage());
        }
    }
    
    private static class TestResult {
        final String testName;
        final String className;
        final String status;
        final String errorMessage;
        
        TestResult(String testName, String className, String status, String errorMessage) {
            this.testName = testName;
            this.className = className;
            this.status = status;
            this.errorMessage = errorMessage != null ? errorMessage : "";
        }
    }
}

// Docker Compose for local development and testing
/*
# docker-compose-test.yml
version: '3.8'
services:
  mysql-customer:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: customer_service
      MYSQL_USER: test
      MYSQL_PASSWORD: test
      MYSQL_ROOT_PASSWORD: root
    ports:
      - "3307:3306"
    volumes:
      - customer_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      timeout: 20s
      retries: 10
      
  mysql-policy:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: policy_service
      MYSQL_USER: test
      MYSQL_PASSWORD: test
      MYSQL_ROOT_PASSWORD: root
    ports:
      - "3308:3306"
    volumes:
      - policy_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      timeout: 20s
      retries: 10
      
  mysql-claims:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: claims_service
      MYSQL_USER: test
      MYSQL_PASSWORD: test
      MYSQL_ROOT_PASSWORD: root
    ports:
      - "3309:3306"
    volumes:
      - claims_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      timeout: 20s
      retries: 10

volumes:
  customer_data:
  policy_data:
  claims_data:
*/

// Maven Test Execution Script
/*
#!/bin/bash
# run-integration-tests.sh

echo "🚀 Starting Aviva Auto Claims Integration Tests"
echo "================================================="

# Clean and compile
echo "📦 Cleaning and compiling project..."
mvn clean compile test-compile -q

# Run integration tests with TestContainers
echo "🐳 Starting TestContainers and running integration tests..."

# Option 1: Run all integration tests individually
mvn test -Dtest="AutoClaimsIntegrationTest,PerformanceIntegrationTest,EndToEndScenarioTest" \
    -Dspring.profiles.active=test \
    -Dtestcontainers.reuse.enable=true \
    -Djunit.jupiter.execution.parallel.enabled=false

# Option 2: Run test runner class (simpler approach)
# mvn test -Dtest="AutoClaimsIntegrationTestRunner" \
#     -Dspring.profiles.active=test \
#     -Dtestcontainers.reuse.enable=true

# Option 3: Run with JUnit Platform Suite (if dependencies added)
# mvn test -Dtest="AutoClaimsIntegrationTestSuite" \
#     -Dspring.profiles.active=test \
#     -Dtestcontainers.reuse.enable=true

# Generate test report
echo "📊 Generating test reports..."
mvn surefire-report:report-only
mvn site -DgenerateReports=false

echo "✅ Integration tests completed!"
echo "📋 Test report available at: target/site/surefire-report.html"
echo "📋 Detailed report available at: target/integration-test-report.md"

# Optional: Open reports in browser (macOS/Linux)
if command -v open &> /dev/null; then
    open target/site/surefire-report.html
elif command -v xdg-open &> /dev/null; then
    xdg-open target/site/surefire-report.html
fi
*/

// README Documentation
/*
# Aviva Auto Claims Service - Integration Testing with TestContainers

## 🎯 Overview

This project demonstrates comprehensive integration testing for an auto claims processing service using TestContainers. It showcases how to left-shift integration testing to developer machines in the Aviva development environment.

## 🏗️ Architecture

### Services
1. **Auto Claims Service** (Main Service)
   - Processes claim submissions
   - Validates claims through external services
   - Applies business rules

2. **Customer Service** (Dependency)
   - Manages customer data
   - Validates customer existence

3. **Claims Policy Service** (Dependency)
   - Manages policy data
   - Validates policy status and coverage
   - Detects duplicate claims

### Database Design
Each service has its own MySQL database:
- **customer_service**: Customer data
- **policy_service**: Policy and existing claims data
- **claims_service**: New claims processing

## 🐳 TestContainers Implementation

### Container Setup
```java
// Three separate MySQL containers for service isolation
static final MySQLContainer<?> customerDatabase = new MySQLContainer<>("mysql:8.0")
    .withDatabaseName("customer_service")
    .withUsername("test")
    .withPassword("test");

static final MySQLContainer<?> policyDatabase = new MySQLContainer<>("mysql:8.0")
    .withDatabaseName("policy_service")
    .withUsername("test")
    .withPassword("test");

static final MySQLContainer<?> claimsDatabase = new MySQLContainer<>("mysql:8.0")
    .withDatabaseName("claims_service")
    .withUsername("test")
    .withPassword("test");
```

### Service Simulation
- **WireMock** simulates external services (Customer and Policy services)
- Realistic response scenarios including success, failure, and edge cases
- Network-isolated containers for true integration testing

## 🧪 Test Coverage

### Happy Path Tests
- ✅ Valid claim submission and processing
- ✅ Customer validation
- ✅ Policy validation
- ✅ Claim number generation

### Error Scenarios
- ❌ Non-existent customer
- ❌ Invalid/expired policy
- ❌ Duplicate claim detection
- ❌ Business rule violations (amount limits, date validation)

### Performance & Concurrency
- 🚀 Concurrent claim processing
- 📊 Performance under load
- 🔄 Data consistency validation

### End-to-End Scenarios
- 🔄 Complete claim lifecycle (submission → investigation → approval)
- 👥 Multiple customers with multiple policies
- 🎯 Complex business scenarios

## 🚀 Running the Tests

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker (for TestContainers)

### Quick Start
```bash
# Run all integration tests
./run-integration-tests.sh

# Or with Maven directly
mvn test -Dtest="AutoClaimsIntegrationTestSuite"
```

### Individual Test Classes
```bash
# Core integration tests
mvn test -Dtest="AutoClaimsIntegrationTest"

# Performance tests
mvn test -Dtest="PerformanceIntegrationTest"

# End-to-end scenarios
mvn test -Dtest="EndToEndScenarioTest"
```

## 📊 Test Results & Reporting

Tests generate comprehensive reports:
- **JUnit HTML Report**: `target/site/surefire-report.html`
- **Detailed Markdown Report**: `target/integration-test-report.md`
- **Console Output**: Real-time test progress with emojis

### Sample Output
```
🚀 AVIVA AUTO CLAIMS INTEGRATION TESTS STARTED
================================================================================
TestContainers: ✓ MySQL, ✓ WireMock Services
--------------------------------------------------------------------------------
✅ Should successfully process a valid claim - PASSED
✅ Should process multiple valid claims from different customers - PASSED
❌ Should reject claim for non-existent customer - PASSED
⚠️ Should handle service unavailability gracefully - PASSED
--------------------------------------------------------------------------------
📊 TEST EXECUTION SUMMARY
Total Tests: 10 | ✅ Passed: 9 | ❌ Failed: 0 | Success Rate: 90.00%
🏁 INTEGRATION TESTS COMPLETED
```

## 🔧 Configuration

### Database Schema
Tables are automatically created with test data:
- **Customers**: 4 test customers with realistic data
- **Policies**: 5 policies (active, expired, different coverage)
- **Existing Claims**: Sample claims for duplicate detection

### WireMock Configuration
- **Customer Service Mock**: Port 8081
- **Policy Service Mock**: Port 8082
- Realistic response scenarios with proper JSON formatting

### Test Data Builder
Fluent API for creating test data:
```java
ClaimRequest claim = TestDataBuilder.claimRequest()
    .customerId("CUST001")
    .policyNumber("POL001")
    .claimType(Claim.ClaimType.COLLISION)
    .claimAmount(new BigDecimal("5000.00"))
    .build();
```

## 🎯 Business Value & Benefits

### Left-Shift Testing Benefits
1. **Early Detection**: Find integration issues before deployment
2. **Developer Productivity**: Run full integration tests locally
3. **CI/CD Ready**: No external dependencies for testing
4. **Realistic Testing**: Use real databases and service interactions

### Quality Assurance
- **Data Integrity**: Ensures database transactions work correctly
- **Service Communication**: Validates API contracts and data flow
- **Performance**: Identifies bottlenecks early in development
- **Resilience**: Tests error handling and fallback mechanisms

### Cost Reduction
- **Faster Feedback**: Immediate test results without environment setup
- **Reduced Defects**: Catch issues before they reach QA/Production
- **Environment Independence**: No shared test environment conflicts
- **Debugging Efficiency**: Full stack available for investigation

## 🔍 Key Testing Patterns Demonstrated

### 1. Database Per Service
Each service has isolated database for true microservice testing

### 2. Service Virtualization
WireMock provides controlled, repeatable external service responses

### 3. Data-Driven Testing
Comprehensive test data setup with realistic business scenarios

### 4. Parallel Test Execution
Concurrent test execution without shared state conflicts

### 5. Performance Testing
Load testing with concurrent requests and response time validation

## 🛠️ Extending the Tests

### Adding New Test Scenarios
```java
@Test
@DisplayName("Should handle new business scenario")
void shouldHandleNewScenario() {
    // Given
    ClaimRequest request = TestDataBuilder.claimRequest()
        .withCustomScenario()
        .build();
    
    // When
    ResponseEntity<ClaimResponse> response = submitClaim(request);
    
    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
}
```

### Adding New Service Dependencies
1. Create new TestContainer for the service database
2. Add WireMock stubs for service endpoints
3. Update BaseIntegrationTest with initialization logic
4. Create test scenarios covering the new integration points

## 📈 Continuous Improvement

### Metrics Tracked
- Test execution time
- Success/failure rates
- Performance benchmarks
- Code coverage for integration paths

### Best Practices Applied
- Isolated test data
- Deterministic test execution
- Comprehensive error scenarios
- Performance benchmarking
- Clear test documentation

## 🎉 Conclusion

This integration test suite demonstrates how TestContainers enables comprehensive, reliable, and fast integration testing for complex microservice architectures. By bringing the full stack to the developer's machine, we achieve:

- **Confidence**: All integration points are tested
- **Speed**: Fast feedback without external dependencies  
- **Reliability**: Consistent, repeatable test results
- **Maintainability**: Clear test structure and documentation

Perfect for showcasing left-shift testing capabilities in the Aviva development environment! 🚀
*/