package com.paystackfineract.connector.dto;

import com.paystackfineract.connector.model.FineractAccountType;
import jakarta.validation.constraints.*;

public class InitializePaymentRequest {

    @NotBlank
    @Email
    private String email;

    /** Amount in the MAJOR currency unit, e.g. 5000.00 for ₦5,000. Converted to kobo internally. */
    @NotNull
    @DecimalMin(value = "1.0")
    private java.math.BigDecimal amount;

    private String currency = "NGN";

    @NotNull
    private Long fineractClientId;

    @NotNull
    private FineractAccountType accountType;

    /** Loan id if accountType=LOAN, savings account id if accountType=SAVINGS. */
    @NotNull
    private Long fineractAccountId;

    /** Optional override; falls back to paystack.default-callback-url if omitted. */
    private String callbackUrl;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public java.math.BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(java.math.BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Long getFineractClientId() {
        return fineractClientId;
    }

    public void setFineractClientId(Long fineractClientId) {
        this.fineractClientId = fineractClientId;
    }

    public FineractAccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(FineractAccountType accountType) {
        this.accountType = accountType;
    }

    public Long getFineractAccountId() {
        return fineractAccountId;
    }

    public void setFineractAccountId(Long fineractAccountId) {
        this.fineractAccountId = fineractAccountId;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }
}
