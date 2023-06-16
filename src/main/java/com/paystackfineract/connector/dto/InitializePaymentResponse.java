package com.paystackfineract.connector.dto;

public class InitializePaymentResponse {

    private String reference;
    private String authorizationUrl;
    private String accessCode;

    public InitializePaymentResponse(String reference, String authorizationUrl, String accessCode) {
        this.reference = reference;
        this.authorizationUrl = authorizationUrl;
        this.accessCode = accessCode;
    }

    public String getReference() {
        return reference;
    }

    public String getAuthorizationUrl() {
        return authorizationUrl;
    }

    public String getAccessCode() {
        return accessCode;
    }
}
