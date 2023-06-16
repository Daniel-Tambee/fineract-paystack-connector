package com.paystackfineract.connector.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fineract")
public class FineractProperties {

    /** e.g. https://localhost/fineract-provider/api/v1 */
    private String baseUrl;

    private String username;
    private String password;

    /** Fineract-Platform-TenantId header value, e.g. "default". */
    private String tenantId = "default";

    /**
     * Fineract's demo/self-hosted instances commonly run on a self-signed cert.
     * Only enable this for local/dev/sandbox environments — never in production.
     */
    private boolean trustSelfSigned = false;

    /** Fineract payment type id to tag transactions originating from Paystack. */
    private Long paystackPaymentTypeId;

    /** Locale/date format Fineract expects on transaction commands. */
    private String locale = "en";
    private String dateFormat = "dd MMMM yyyy";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public boolean isTrustSelfSigned() {
        return trustSelfSigned;
    }

    public void setTrustSelfSigned(boolean trustSelfSigned) {
        this.trustSelfSigned = trustSelfSigned;
    }

    public Long getPaystackPaymentTypeId() {
        return paystackPaymentTypeId;
    }

    public void setPaystackPaymentTypeId(Long paystackPaymentTypeId) {
        this.paystackPaymentTypeId = paystackPaymentTypeId;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }
}
