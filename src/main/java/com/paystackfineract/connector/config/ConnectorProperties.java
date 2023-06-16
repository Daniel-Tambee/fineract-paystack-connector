package com.paystackfineract.connector.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "connector")
public class ConnectorProperties {

    /**
     * Simple shared-secret API key required (via X-API-KEY header) on the
     * outward-facing /api/v1/payments/** endpoints. This is NOT used on the
     * webhook endpoint, which is authenticated via the Paystack signature instead.
     * Put this connector behind a proper gateway/OAuth layer for production use.
     */
    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
