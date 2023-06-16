package com.paystackfineract.connector.exception;

public class ConnectorExceptions {

    public static class PaystackApiException extends RuntimeException {
        public PaystackApiException(String message) {
            super(message);
        }

        public PaystackApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class FineractApiException extends RuntimeException {
        public FineractApiException(String message) {
            super(message);
        }

        public FineractApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class InvalidWebhookSignatureException extends RuntimeException {
        public InvalidWebhookSignatureException(String message) {
            super(message);
        }
    }

    public static class PaymentNotFoundException extends RuntimeException {
        public PaymentNotFoundException(String reference) {
            super("No payment found for reference: " + reference);
        }
    }

    public static class AmountMismatchException extends RuntimeException {
        public AmountMismatchException(String message) {
            super(message);
        }
    }
}
