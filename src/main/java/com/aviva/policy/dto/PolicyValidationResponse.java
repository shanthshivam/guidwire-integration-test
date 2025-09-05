package com.aviva.policy.dto;

public class PolicyValidationResponse {
    private boolean valid;
    private String policyNumber;
    private String customerId;
    private boolean active;
    private String validationMessage;
    
    public PolicyValidationResponse() {}
    
    public PolicyValidationResponse(boolean valid, String policyNumber, String customerId, 
                                  boolean active, String validationMessage) {
        this.valid = valid;
        this.policyNumber = policyNumber;
        this.customerId = customerId;
        this.active = active;
        this.validationMessage = validationMessage;
    }
    
    // Getters and Setters
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    
    public String getPolicyNumber() { return policyNumber; }
    public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public String getValidationMessage() { return validationMessage; }
    public void setValidationMessage(String validationMessage) { this.validationMessage = validationMessage; }
}

// Claim Validation Response DTO
