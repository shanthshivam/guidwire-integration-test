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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class PerformanceIntegrationTest extends BaseIntegrationTest {
    
    @LocalServerPort
    private int claimsServicePort;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Override
    protected int getCustomerServicePort() {
        return CUSTOMER_SERVICE_PORT;
    }
    
    @Override
    protected int getPolicyServicePort() {
        return POLICY_SERVICE_PORT;
    }
    
    @Test
    @DisplayName("Should handle concurrent claim submissions")
    void shouldHandleConcurrentClaimSubmissions() throws InterruptedException {
        // Given
        int numberOfThreads = 10;
        int claimsPerThread = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads * claimsPerThread);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        List<Future<ResponseEntity<ClaimResponse>>> futures = new ArrayList<>();
        
        // When
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numberOfThreads; i++) {
            for (int j = 0; j < claimsPerThread; j++) {
                final int threadId = i;
                final int claimId = j;
                
                Future<ResponseEntity<ClaimResponse>> future = executorService.submit(() -> {
                    try {
                        ClaimRequest claimRequest = TestDataBuilder.claimRequest()
                                .customerId("CUST001")
                                .policyNumber("POL001")
                                .claimType(Claim.ClaimType.COLLISION)
                                .claimAmount(new BigDecimal("" + (1000 + threadId * 100 + claimId)))
                                .incidentLocation("Location-" + threadId + "-" + claimId)
                                .build();
                        
                        ResponseEntity<ClaimResponse> response = restTemplate.postForEntity(
                                "http://localhost:" + claimsServicePort + "/api/claims",
                                claimRequest,
                                ClaimResponse.class
                        );
                        
                        if (response.getStatusCode() == HttpStatus.OK) {
                            successCount.incrementAndGet();
                        } else {
                            errorCount.incrementAndGet();
                        }
                        
                        return response;
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        throw new RuntimeException("Failed to submit claim", e);
                    } finally {
                        latch.countDown();
                    }
                });
                
                futures.add(future);
            }
        }
        
        // Wait for all requests to complete (with timeout)
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // Then
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isGreaterThan(0);
        
        // Performance assertions
        double averageResponseTime = (double) totalTime / (numberOfThreads * claimsPerThread);
        assertThat(averageResponseTime).isLessThan(5000); // Average response time should be less than 5 seconds
        
        System.out.println("Performance Test Results:");
        System.out.println("Total time: " + totalTime + "ms");
        System.out.println("Average response time: " + averageResponseTime + "ms");
        System.out.println("Successful requests: " + successCount.get());
        System.out.println("Failed requests: " + errorCount.get());
        System.out.println("Throughput: " + ((double) successCount.get() / totalTime * 1000) + " requests/second");
        
        executorService.shutdown();
    }
    
    @Test
    @DisplayName("Should maintain data consistency under concurrent load")
    void shouldMaintainDataConsistencyUnderLoad() throws InterruptedException {
        // Given
        int numberOfRequests = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        List<String> claimNumbers = new CopyOnWriteArrayList<>();
        
        // When - Submit multiple claims concurrently
        for (int i = 0; i < numberOfRequests; i++) {
            final int requestId = i;
            executorService.submit(() -> {
                try {
                    ClaimRequest claimRequest = TestDataBuilder.claimRequest()
                            .customerId("CUST001")
                            .policyNumber("POL001")
                            .claimAmount(new BigDecimal("" + (2000 + requestId * 100)))
                            .incidentLocation("Consistency-Test-Location-" + requestId)
                            .build();
                    
                    ResponseEntity<ClaimResponse> response = restTemplate.postForEntity(
                            "http://localhost:" + claimsServicePort + "/api/claims",
                            claimRequest,
                            ClaimResponse.class
                    );
                    
                    if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                        claimNumbers.add(response.getBody().getClaimNumber());
                    }
                } catch (Exception e) {
                    System.err.println("Error in concurrent request " + requestId + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();
        
        // Then - Verify data consistency
        assertThat(completed).isTrue();
        assertThat(claimNumbers).isNotEmpty();
        
        // All claim numbers should be unique
        long uniqueClaimNumbers = claimNumbers.stream().distinct().count();
        assertThat(uniqueClaimNumbers).isEqualTo(claimNumbers.size());
        
        // All claim numbers should follow the expected format
        claimNumbers.forEach(claimNumber -> 
            assertThat(claimNumber).matches("CLM-[A-Z0-9]{8}"));
    }
}

// End-to-End Scenario Test
