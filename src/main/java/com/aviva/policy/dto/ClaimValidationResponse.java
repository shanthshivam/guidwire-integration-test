package com.aviva.policy.dto;

import com.aviva.claims.model.Claim;
import java.util.List;

public class ClaimValidationResponse {
    private boolean isDuplicate;
    private boolean hasExistingClaims;
    private List<Claim> existingClaims;
    private String validationMessage;
    
    public ClaimValidationResponse() {}
    
    public ClaimValidationResponse(boolean isDuplicate, boolean hasExistingClaims, 
                                 List<Claim> existingClaims, String validationMessage) {
        this.isDuplicate = isDuplicate;
        this.hasExistingClaims = hasExistingClaims;
        this.existingClaims = existingClaims;
        this.validationMessage = validationMessage;
    }
    
    // Getters and Setters
    public boolean isDuplicate() { return isDuplicate; }
    public void setDuplicate(boolean duplicate) { isDuplicate = duplicate; }
    
    public boolean isHasExistingClaims() { return hasExistingClaims; }
    public void setHasExistingClaims(boolean hasExistingClaims) { this.hasExistingClaims = hasExistingClaims; }
    
    public List<Claim> getExistingClaims() { return existingClaims; }
    public void setExistingClaims(List<Claim> existingClaims) { this.existingClaims = existingClaims; }
    
    public String getValidationMessage() { return validationMessage; }
    public void setValidationMessage(String validationMessage) { this.validationMessage = validationMessage; }
}

// Policy Controller
