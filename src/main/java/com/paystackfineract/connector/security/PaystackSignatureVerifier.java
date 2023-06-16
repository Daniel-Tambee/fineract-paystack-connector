package com.paystackfineract.connector.security;

import com.paystackfineract.connector.config.PaystackProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Paystack signs every webhook payload with HMAC-SHA512 using your secret key,
 * sent in the "x-paystack-signature" header. Always verify this before trusting
 * a webhook body — it's the only thing standing between you and a spoofed
 * "charge.success" event.
 * https://paystack.com/docs/payments/webhooks/
 */
@Component
public class PaystackSignatureVerifier {

    private static final String HMAC_ALGO = "HmacSHA512";

    private final PaystackProperties properties;

    public PaystackSignatureVerifier(PaystackProperties properties) {
        this.properties = properties;
    }

    public boolean isValid(String rawBody, String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank() || rawBody == null) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(properties.getSecretKey().getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] computed = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String computedHex = toHex(computed);
            return MessageDigest.isEqual(
                    computedHex.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
