package com.paystackfineract.connector.controller;

import com.paystackfineract.connector.dto.InitializePaymentRequest;
import com.paystackfineract.connector.dto.InitializePaymentResponse;
import com.paystackfineract.connector.dto.PaystackInitializeResponse;
import com.paystackfineract.connector.exception.ConnectorExceptions.PaymentNotFoundException;
import com.paystackfineract.connector.model.PaymentStatus;
import com.paystackfineract.connector.model.PaymentTransaction;
import com.paystackfineract.connector.repository.PaymentTransactionRepository;
import com.paystackfineract.connector.service.PaystackService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaystackService paystackService;
    private final PaymentTransactionRepository repository;

    public PaymentController(PaystackService paystackService, PaymentTransactionRepository repository) {
        this.paystackService = paystackService;
        this.repository = repository;
    }

    /**
     * Kicks off a Paystack checkout for a loan repayment or savings deposit.
     * The caller (your USSD/web/mobile front-end) should redirect the payer to
     * the returned authorizationUrl to complete payment on Paystack's page.
     */
    @PostMapping("/initialize")
    public ResponseEntity<InitializePaymentResponse> initialize(@Valid @RequestBody InitializePaymentRequest request) {
        String reference = "PSTK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();

        long amountMinorUnits = request.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setReference(reference);
        transaction.setPayerEmail(request.getEmail());
        transaction.setAmountMinorUnits(amountMinorUnits);
        transaction.setCurrency(request.getCurrency());
        transaction.setFineractClientId(request.getFineractClientId());
        transaction.setAccountType(request.getAccountType());
        transaction.setFineractAccountId(request.getFineractAccountId());
        transaction.setStatus(PaymentStatus.PENDING);
        repository.save(transaction);

        PaystackInitializeResponse paystackResponse = paystackService.initializeTransaction(
                request.getEmail(), amountMinorUnits, request.getCurrency(), reference, request.getCallbackUrl());

        return ResponseEntity.ok(new InitializePaymentResponse(
                reference,
                paystackResponse.getData().getAuthorizationUrl(),
                paystackResponse.getData().getAccessCode()));
    }

    /** Lets your front-end poll for status instead of/alongside handling the callback redirect. */
    @GetMapping("/{reference}")
    public ResponseEntity<PaymentTransaction> getStatus(@PathVariable String reference) {
        PaymentTransaction transaction = repository.findByReference(reference)
                .orElseThrow(() -> new PaymentNotFoundException(reference));
        return ResponseEntity.ok(transaction);
    }
}
