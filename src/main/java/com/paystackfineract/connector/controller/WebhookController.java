package com.paystackfineract.connector.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paystackfineract.connector.exception.ConnectorExceptions.InvalidWebhookSignatureException;
import com.paystackfineract.connector.model.PaymentTransaction;
import com.paystackfineract.connector.security.PaystackSignatureVerifier;
import com.paystackfineract.connector.service.PaymentReconciliationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);
    private static final String SIGNATURE_HEADER = "x-paystack-signature";

    private final PaystackSignatureVerifier signatureVerifier;
    private final PaymentReconciliationService reconciliationService;
    private final ObjectMapper objectMapper;

    public WebhookController(PaystackSignatureVerifier signatureVerifier,
                              PaymentReconciliationService reconciliationService,
                              ObjectMapper objectMapper) {
        this.signatureVerifier = signatureVerifier;
        this.reconciliationService = reconciliationService;
        this.objectMapper = objectMapper;
    }

    /**
     * IMPORTANT: rawBody must be the exact, unparsed request body — Paystack's
     * HMAC signature is computed over the raw bytes, and re-serializing a
     * parsed object will not match. Register this URL as your webhook endpoint
     * in the Paystack dashboard (Settings -> API Keys & Webhooks).
     */
    @PostMapping("/paystack")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = SIGNATURE_HEADER, required = false) String signature) throws Exception {

        if (!signatureVerifier.isValid(rawBody, signature)) {
            log.warn("Rejected webhook with invalid Paystack signature");
            throw new InvalidWebhookSignatureException("Invalid " + SIGNATURE_HEADER + " header");
        }

        JsonNode payload = objectMapper.readTree(rawBody);
        String event = payload.path("event").asText();

        if (!"charge.success".equals(event)) {
            log.info("Ignoring Paystack event type: {}", event);
            return ResponseEntity.ok("ignored");
        }

        String reference = payload.path("data").path("reference").asText(null);
        if (reference == null) {
            log.warn("charge.success webhook missing data.reference: {}", rawBody);
            return ResponseEntity.badRequest().body("missing reference");
        }

        PaymentTransaction result = reconciliationService.reconcile(reference);
        log.info("Reconciled payment {} -> status {}", reference, result.getStatus());
        return ResponseEntity.ok("processed");
    }
}
