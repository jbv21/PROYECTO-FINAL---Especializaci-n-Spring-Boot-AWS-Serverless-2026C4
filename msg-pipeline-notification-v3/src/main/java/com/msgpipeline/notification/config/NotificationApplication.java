package com.msgpipeline.notification.config;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase de arranque de Spring Boot para el Notification v3.
 * Escanea el paquete base com.msgpipeline.notification.
 */
@SpringBootApplication(scanBasePackages = "com.msgpipeline.notification")
public class NotificationApplication {
    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(NotificationApplication.class, args);
    }
}
