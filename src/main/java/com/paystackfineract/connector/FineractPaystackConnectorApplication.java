package com.paystackfineract.connector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FineractPaystackConnectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(FineractPaystackConnectorApplication.class, args);
    }
}
