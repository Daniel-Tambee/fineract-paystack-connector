package com.paystackfineract.connector.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "payment_transactions", indexes = {
        @Index(name = "idx_payment_reference", columnList = "reference", unique = true)
})
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique reference sent to Paystack; also our idempotency key. */
    @Column(nullable = false, unique = true, length = 100)
    private String reference;

    @Column(nullable = false)
    private String payerEmail;

    /** Amount in the currency's smallest unit (e.g. kobo for NGN), as sent to Paystack. */
    @Column(nullable = false)
    private Long amountMinorUnits;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false)
    private Long fineractClientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FineractAccountType accountType;

    /** Loan id or Savings account id in Fineract, depending on accountType. */
    @Column(nullable = false)
    private Long fineractAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    private String paystackTransactionId;

    private String fineractTransactionId;

    @Column(length = 500)
    private String failureReason;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // --- getters/setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getPayerEmail() {
        return payerEmail;
    }

    public void setPayerEmail(String payerEmail) {
        this.payerEmail = payerEmail;
    }

    public Long getAmountMinorUnits() {
        return amountMinorUnits;
    }

    public void setAmountMinorUnits(Long amountMinorUnits) {
        this.amountMinorUnits = amountMinorUnits;
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

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getPaystackTransactionId() {
        return paystackTransactionId;
    }

    public void setPaystackTransactionId(String paystackTransactionId) {
        this.paystackTransactionId = paystackTransactionId;
    }

    public String getFineractTransactionId() {
        return fineractTransactionId;
    }

    public void setFineractTransactionId(String fineractTransactionId) {
        this.fineractTransactionId = fineractTransactionId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
