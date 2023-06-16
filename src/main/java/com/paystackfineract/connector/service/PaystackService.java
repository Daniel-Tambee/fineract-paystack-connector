package com.paystackfineract.connector.service;

import com.paystackfineract.connector.config.PaystackProperties;
import com.paystackfineract.connector.dto.PaystackInitializeResponse;
import com.paystackfineract.connector.dto.PaystackVerifyResponse;
import com.paystackfineract.connector.exception.ConnectorExceptions.PaystackApiException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PaystackService {

    private final RestTemplate paystackRestTemplate;
    private final PaystackProperties properties;

    public PaystackService(RestTemplate paystackRestTemplate, PaystackProperties properties) {
        this.paystackRestTemplate = paystackRestTemplate;
        this.properties = properties;
    }

    /**
     * Kicks off a Paystack checkout. amountMinorUnits must already be in the
     * currency's smallest unit (e.g. kobo for NGN, cents for USD/GHS pesewas etc).
     */
    public PaystackInitializeResponse initializeTransaction(String email, long amountMinorUnits,
                                                             String currency, String reference,
                                                             String callbackUrl) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("amount", amountMinorUnits);
        body.put("currency", currency);
        body.put("reference", reference);
        if (callbackUrl != null && !callbackUrl.isBlank()) {
            body.put("callback_url", callbackUrl);
        } else if (properties.getDefaultCallbackUrl() != null) {
            body.put("callback_url", properties.getDefaultCallbackUrl());
        }

        try {
            ResponseEntity<PaystackInitializeResponse> response = paystackRestTemplate.postForEntity(
                    properties.getBaseUrl() + "/transaction/initialize",
                    new HttpEntity<>(body),
                    PaystackInitializeResponse.class);

            PaystackInitializeResponse result = response.getBody();
            if (result == null || !result.isStatus()) {
                throw new PaystackApiException(
                        result != null ? result.getMessage() : "Empty response from Paystack");
            }
            return result;
        } catch (RestClientException e) {
            throw new PaystackApiException("Failed to initialize transaction with Paystack", e);
        }
    }

    /**
     * Always re-verify with Paystack server-to-server before crediting anything —
     * never trust the webhook payload's amount/status fields on their own.
     */
    public PaystackVerifyResponse verifyTransaction(String reference) {
        try {
            ResponseEntity<PaystackVerifyResponse> response = paystackRestTemplate.exchange(
                    properties.getBaseUrl() + "/transaction/verify/{reference}",
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    PaystackVerifyResponse.class,
                    reference);

            PaystackVerifyResponse result = response.getBody();
            if (result == null) {
                throw new PaystackApiException("Empty verify response from Paystack for reference " + reference);
            }
            return result;
        } catch (RestClientException e) {
            throw new PaystackApiException("Failed to verify transaction " + reference + " with Paystack", e);
        }
    }
}
