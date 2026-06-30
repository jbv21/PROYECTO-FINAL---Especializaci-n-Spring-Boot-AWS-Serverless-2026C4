package com.msgpipeline.validator.config;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase de arranque de Spring Boot para el Validator v3.
 * Escanea el paquete base com.msgpipeline.validator.
 */
@SpringBootApplication(scanBasePackages = "com.msgpipeline.validator")
public class ValidatorApplication {
    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(ValidatorApplication.class, args);
    }
}
