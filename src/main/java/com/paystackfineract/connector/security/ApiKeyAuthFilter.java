package com.paystackfineract.connector.security;

import com.paystackfineract.connector.config.ConnectorProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lightweight guard for the payment-initiation/status endpoints so this
 * connector isn't wide open. Swap this out for OAuth2/mTLS behind your
 * API gateway in a real production deployment — this is a minimum bar, not
 * a complete auth solution.
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-API-KEY";

    private final ConnectorProperties properties;

    public ApiKeyAuthFilter(ConnectorProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Webhook is authenticated via Paystack's HMAC signature instead.
        return request.getRequestURI().startsWith("/api/v1/webhooks/")
                || request.getRequestURI().startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            // No key configured -> filter is a no-op (dev convenience only).
            filterChain.doFilter(request, response);
            return;
        }

        String provided = request.getHeader(HEADER);
        if (provided == null || !provided.equals(properties.getApiKey())) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Missing or invalid " + HEADER + " header\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
