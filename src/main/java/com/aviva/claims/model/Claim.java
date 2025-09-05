package com.aviva.claims.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "claims")
public class Claim {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(unique = true)
    private String claimNumber;
    
    @NotBlank
    private String customerId;
    
    @NotBlank
    private String policyNumber;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    private ClaimStatus status;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    private ClaimType claimType;
    
    @DecimalMin("0.0")
    private BigDecimal claimAmount;
    
    private String description;
    private LocalDateTime incidentDate;
    private String incidentLocation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public enum ClaimStatus {
        SUBMITTED, PENDING, INVESTIGATING, APPROVED, REJECTED, PAID, CLOSED
    }
    
    public enum ClaimType {
        COLLISION, COMPREHENSIVE, LIABILITY, PERSONAL_INJURY, THEFT, VANDALISM
    }
    
    // Constructors
    public Claim() {}
    
    public Claim(String claimNumber, String customerId, String policyNumber, ClaimType claimType,
                 BigDecimal claimAmount, String description, LocalDateTime incidentDate, String incidentLocation) {
        this.claimNumber = claimNumber;
        this.customerId = customerId;
        this.policyNumber = policyNumber;
        this.status = ClaimStatus.SUBMITTED;
        this.claimType = claimType;
        this.claimAmount = claimAmount;
        this.description = description;
        this.incidentDate = incidentDate;
        this.incidentLocation = incidentLocation;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters (abbreviated)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getClaimNumber() { return claimNumber; }
    public void setClaimNumber(String claimNumber) { this.claimNumber = claimNumber; }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public String getPolicyNumber() { return policyNumber; }
    public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }
    
    public ClaimStatus getStatus() { return status; }
    public void setStatus(ClaimStatus status) { this.status = status; }
    
    public ClaimType getClaimType() { return claimType; }
    public void setClaimType(ClaimType claimType) { this.claimType = claimType; }
    
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
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

// DTOs for API Communication
