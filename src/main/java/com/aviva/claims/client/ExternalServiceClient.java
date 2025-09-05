package com.aviva.claims.client;

import com.aviva.claims.model.Customer;
import com.aviva.policy.dto.PolicyValidationResponse;
import com.aviva.policy.dto.ClaimValidationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class ExternalServiceClient {
    
    private final WebClient webClient;
    private final String customerServiceUrl;
    private final String policyServiceUrl;
    
    public ExternalServiceClient(WebClient.Builder webClientBuilder,
                               @Value("${customer.service.url:http://localhost:8081}") String customerServiceUrl,
                               @Value("${policy.service.url:http://localhost:8082}") String policyServiceUrl) {
        this.webClient = webClientBuilder.build();
        this.customerServiceUrl = customerServiceUrl;
        this.policyServiceUrl = policyServiceUrl;
    }
    
    public Mono<Customer> getCustomer(String customerId) {
        return webClient.get()
            .uri(customerServiceUrl + "/api/customers/{customerId}", customerId)
            .retrieve()
            .bodyToMono(Customer.class)
            .onErrorReturn(new Customer()); // Return empty customer on error
    }
    
    public Mono<Boolean> customerExists(String customerId) {
        return webClient.get()
            .uri(customerServiceUrl + "/api/customers/{customerId}/exists", customerId)
            .retrieve()
            .bodyToMono(Boolean.class)
            .onErrorReturn(false);
    }
    
    public Mono<PolicyValidationResponse> validatePolicy(String policyNumber, String customerId) {
        return webClient.get()
            .uri(policyServiceUrl + "/api/policies/{policyNumber}/validate?customerId={customerId}", 
                 policyNumber, customerId)
            .retrieve()
            .bodyToMono(PolicyValidationResponse.class)
            .onErrorReturn(new PolicyValidationResponse(false, policyNumber, customerId, false, "Service unavailable"));
    }
    
    public Mono<ClaimValidationResponse> validateClaim(String policyNumber, String customerId, 
                                                      String incidentLocation, LocalDateTime incidentDate) {
        String dateStr = incidentDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return webClient.get()
            .uri(policyServiceUrl + "/api/policies/{policyNumber}/claims/validate?customerId={customerId}&incidentLocation={location}&incidentDate={date}",
                 policyNumber, customerId, incidentLocation, dateStr)
            .retrieve()
            .bodyToMono(ClaimValidationResponse.class)
            .onErrorReturn(new ClaimValidationResponse(false, false, null, "Service unavailable"));
    }
}

// Claims Validation Service
