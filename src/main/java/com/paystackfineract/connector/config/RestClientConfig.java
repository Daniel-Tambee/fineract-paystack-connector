package com.paystackfineract.connector.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * Two separate RestTemplate beans:
 *  - paystackRestTemplate: talks to api.paystack.co with Bearer auth.
 *  - fineractRestTemplate: talks to your Fineract instance with Basic auth +
 *    Fineract-Platform-TenantId header, optionally trusting a self-signed cert
 *    (Fineract's default demo setup ships with one).
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestTemplate paystackRestTemplate(PaystackProperties props) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + props.getSecretKey());
            request.getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json");
            return execution.execute(request, body);
        });
        return restTemplate;
    }

    @Bean
    public RestTemplate fineractRestTemplate(FineractProperties props) throws Exception {
        RestTemplate restTemplate;

        if (props.isTrustSelfSigned()) {
            restTemplate = new RestTemplate(trustAllHttpRequestFactory());
        } else {
            restTemplate = new RestTemplate();
        }

        String basicAuth = Base64.getEncoder().encodeToString(
                (props.getUsername() + ":" + props.getPassword()).getBytes(StandardCharsets.UTF_8));

        ClientHttpRequestInterceptor authInterceptor = (request, body, execution) -> {
            request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth);
            request.getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json");
            request.getHeaders().set("Fineract-Platform-TenantId", props.getTenantId());
            return execution.execute(request, body);
        };
        restTemplate.getInterceptors().add(authInterceptor);
        return restTemplate;
    }

    /**
     * ONLY for dev/sandbox Fineract instances running self-signed certs.
     * Do not enable connector.fineract.trust-self-signed in production —
     * install the real cert in the JVM truststore instead.
     */
    private HttpComponentsClientHttpRequestFactory trustAllHttpRequestFactory() throws Exception {
        SSLContext sslContext = org.apache.hc.core5.ssl.SSLContextBuilder.create()
                .loadTrustMaterial((X509Certificate[] chain, String authType) -> true)
                .build();

        SSLConnectionSocketFactory sslSocketFactory =
                new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(
                RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("https", sslSocketFactory)
                        .register("http", new PlainConnectionSocketFactory())
                        .build());

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }
}
