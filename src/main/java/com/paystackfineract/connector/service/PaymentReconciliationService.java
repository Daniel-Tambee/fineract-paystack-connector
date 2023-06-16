package com.paystackfineract.connector.service;

import com.paystackfineract.connector.dto.PaystackVerifyResponse;
import com.paystackfineract.connector.exception.ConnectorExceptions.AmountMismatchException;
import com.paystackfineract.connector.exception.ConnectorExceptions.PaymentNotFoundException;
import com.paystackfineract.connector.model.PaymentStatus;
import com.paystackfineract.connector.model.PaymentTransaction;
import com.paystackfineract.connector.repository.PaymentTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PaymentReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationService.class);

    private final PaymentTransactionRepository repository;
    private final PaystackService paystackService;
    private final FineractService fineractService;

    public PaymentReconciliationService(PaymentTransactionRepository repository,
                                         PaystackService paystackService,
                                         FineractService fineractService) {
        this.repository = repository;
        this.paystackService = paystackService;
        this.fineractService = fineractService;
    }

    /**
     * Called from the webhook handler (and safe to call again from a
     * reconciliation job/polling endpoint — it's idempotent on transaction status).
     */
    @Transactional
    public PaymentTransaction reconcile(String reference) {
        PaymentTransaction transaction = repository.findByReference(reference)
                .orElseThrow(() -> new PaymentNotFoundException(reference));

        if (transaction.getStatus() != PaymentStatus.PENDING) {
            // Already processed — webhooks can legitimately fire more than once.
            log.info("Payment {} already in terminal state {}, skipping", reference, transaction.getStatus());
            return transaction;
        }

        // Never trust the webhook body's amount/status alone — verify server-to-server.
        PaystackVerifyResponse verify = paystackService.verifyTransaction(reference);
        PaystackVerifyResponse.Data data = verify.getData();

        if (data == null || !"success".equalsIgnoreCase(data.getStatus())) {
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setFailureReason(data != null ? data.getGateway_response() : "Verification returned no data");
            return repository.save(transaction);
        }

        if (!data.getAmount().equals(transaction.getAmountMinorUnits())) {
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setFailureReason(String.format(
                    "Amount mismatch: expected %d, Paystack reports %d",
                    transaction.getAmountMinorUnits(), data.getAmount()));
            repository.save(transaction);
            throw new AmountMismatchException(transaction.getFailureReason());
        }

        transaction.setPaystackTransactionId(data.getId());

        try {
            BigDecimal majorUnitAmount = BigDecimal.valueOf(transaction.getAmountMinorUnits())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            String fineractTxnId = switch (transaction.getAccountType()) {
                case LOAN -> fineractService.postLoanRepayment(
                        transaction.getFineractAccountId(), majorUnitAmount, reference);
                case SAVINGS -> fineractService.postSavingsDeposit(
                        transaction.getFineractAccountId(), majorUnitAmount, reference);
            };

            transaction.setFineractTransactionId(fineractTxnId);
            transaction.setStatus(PaymentStatus.SUCCESS);
        } catch (Exception e) {
            // Payment succeeded on Paystack but posting to Fineract failed.
            // Leave status as PENDING (not FAILED) so this can be retried/reconciled
            // manually rather than silently dropping a confirmed payment.
            log.error("Paystack payment {} verified but Fineract posting failed — needs manual reconciliation", reference, e);
            transaction.setFailureReason("Fineract posting failed: " + e.getMessage());
            repository.save(transaction);
            throw e;
        }

        return repository.save(transaction);
    }
}
