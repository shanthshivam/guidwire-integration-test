package com.aviva.claims.dto;

import com.aviva.claims.model.Claim;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ClaimRequest {
    @NotBlank
    private String customerId;
    
    @NotBlank
    private String policyNumber;
    
    @NotNull
    private Claim.ClaimType claimType;
    
    @DecimalMin("0.0")
    private BigDecimal claimAmount;
    
    private String description;
    
    @NotNull
    private LocalDateTime incidentDate;
    
    private String incidentLocation;
    
    // Constructors
    public ClaimRequest() {}
    
    public ClaimRequest(String customerId, String policyNumber, Claim.ClaimType claimType,
                       BigDecimal claimAmount, String description, LocalDateTime incidentDate, String incidentLocation) {
        this.customerId = customerId;
        this.policyNumber = policyNumber;
        this.claimType = claimType;
        this.claimAmount = claimAmount;
        this.description = description;
        this.incidentDate = incidentDate;
        this.incidentLocation = incidentLocation;
    }
    
    // Getters and Setters
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public String getPolicyNumber() { return policyNumber; }
    public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }
    
    public Claim.ClaimType getClaimType() { return claimType; }
    public void setClaimType(Claim.ClaimType claimType) { this.claimType = claimType; }
    
    public BigDecimal getClaimAmount() { return claimAmount; }
    public void setClaimAmount(BigDecimal claimAmount) { this.claimAmount = claimAmount; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getIncidentDate() { return incidentDate; }
    public void setIncidentDate(LocalDateTime incidentDate) { this.incidentDate = incidentDate; }
    
    public String getIncidentLocation() { return incidentLocation; }
    public void setIncidentLocation(String incidentLocation) { this.incidentLocation = incidentLocation; }
}

