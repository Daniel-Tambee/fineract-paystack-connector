package com.paystackfineract.connector.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "paystack")
public class PaystackProperties {

    /** Secret key used to authorize server-to-server calls and verify webhook signatures. */
    private String secretKey;

    /** Public key, exposed to frontend clients if needed (not used server-side). */
    private String publicKey;

    /** Base URL of the Paystack API. */
    private String baseUrl = "https://api.paystack.co";

    /** Where Paystack should redirect the payer after checkout. */
    private String defaultCallbackUrl;

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getDefaultCallbackUrl() {
        return defaultCallbackUrl;
    }

    public void setDefaultCallbackUrl(String defaultCallbackUrl) {
        this.defaultCallbackUrl = defaultCallbackUrl;
    }
}
