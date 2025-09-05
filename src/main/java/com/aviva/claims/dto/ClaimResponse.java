package com.aviva.claims.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.aviva.claims.model.Claim;

public class ClaimResponse {
    private String claimNumber;
    private String customerId;
    private String policyNumber;
    private Claim.ClaimStatus status;
    private Claim.ClaimType claimType;
    private BigDecimal claimAmount;
    private String description;
    private LocalDateTime incidentDate;
    private String incidentLocation;
    private LocalDateTime createdAt;
    
    // Constructors
    public ClaimResponse() {}
    
    public ClaimResponse(Claim claim) {
        this.claimNumber = claim.getClaimNumber();
        this.customerId = claim.getCustomerId();
        this.policyNumber = claim.getPolicyNumber();
        this.status = claim.getStatus();
        this.claimType = claim.getClaimType();
        this.claimAmount = claim.getClaimAmount();
        this.description = claim.getDescription();
        this.incidentDate = claim.getIncidentDate();
        this.incidentLocation = claim.getIncidentLocation();
        this.createdAt = claim.getCreatedAt();
    }
    
    // Getters and Setters
    public String getClaimNumber() { return claimNumber; }
    public void setClaimNumber(String claimNumber) { this.claimNumber = claimNumber; }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public String getPolicyNumber() { return policyNumber; }
    public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }
    
    public Claim.ClaimStatus getStatus() { return status; }
    public void setStatus(Claim.ClaimStatus status) { this.status = status; }
    
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
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}