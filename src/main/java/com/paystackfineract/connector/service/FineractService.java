package com.paystackfineract.connector.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.paystackfineract.connector.config.FineractProperties;
import com.paystackfineract.connector.exception.ConnectorExceptions.FineractApiException;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Posts confirmed Paystack payments into Fineract as either a loan repayment
 * or a savings deposit transaction, using Fineract's standard "commands" API.
 * Reference: https://fineract.apache.org/legacy-docs/apiLive.htm#loans / #savingsaccounts
 */
@Service
public class FineractService {

    private final RestTemplate fineractRestTemplate;
    private final FineractProperties properties;

    public FineractService(RestTemplate fineractRestTemplate, FineractProperties properties) {
        this.fineractRestTemplate = fineractRestTemplate;
        this.properties = properties;
    }

    /**
     * POST {baseUrl}/loans/{loanId}/transactions?command=repayment
     * Returns the Fineract-assigned transaction id (resourceId) as a string.
     */
    public String postLoanRepayment(Long loanId, BigDecimal amount, String externalReference) {
        Map<String, Object> body = buildTransactionBody(amount, externalReference);
        String url = properties.getBaseUrl() + "/loans/" + loanId + "/transactions?command=repayment";
        return postAndExtractResourceId(url, body, "loan repayment for loan " + loanId);
    }

    /**
     * POST {baseUrl}/savingsaccounts/{savingsId}/transactions?command=deposit
     */
    public String postSavingsDeposit(Long savingsAccountId, BigDecimal amount, String externalReference) {
        Map<String, Object> body = buildTransactionBody(amount, externalReference);
        String url = properties.getBaseUrl() + "/savingsaccounts/" + savingsAccountId + "/transactions?command=deposit";
        return postAndExtractResourceId(url, body, "savings deposit for account " + savingsAccountId);
    }

    private Map<String, Object> buildTransactionBody(BigDecimal amount, String externalReference) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("transactionDate", LocalDate.now().format(
                DateTimeFormatter.ofPattern(properties.getDateFormat(), Locale.ENGLISH)));
        body.put("transactionAmount", amount);
        body.put("locale", properties.getLocale());
        body.put("dateFormat", properties.getDateFormat());
        if (properties.getPaystackPaymentTypeId() != null) {
            body.put("paymentTypeId", properties.getPaystackPaymentTypeId());
        }
        // Fineract doesn't have a dedicated "external reference" field on this
        // command, but note it for reconciliation/audit trails.
        body.put("note", "Paystack reference: " + externalReference);
        return body;
    }

    private String postAndExtractResourceId(String url, Map<String, Object> body, String description) {
        try {
            ResponseEntity<JsonNode> response = fineractRestTemplate.postForEntity(url, new HttpEntity<>(body), JsonNode.class);
            JsonNode responseBody = response.getBody();
            if (responseBody == null || !responseBody.has("resourceId")) {
                throw new FineractApiException("Fineract did not return a resourceId for " + description
                        + " (response: " + responseBody + ")");
            }
            return responseBody.get("resourceId").asText();
        } catch (RestClientException e) {
            throw new FineractApiException("Failed to post " + description + " to Fineract", e);
        }
    }
}
