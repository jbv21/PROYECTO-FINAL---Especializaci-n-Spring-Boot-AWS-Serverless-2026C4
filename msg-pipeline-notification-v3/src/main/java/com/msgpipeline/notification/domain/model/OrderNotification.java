package com.msgpipeline.notification.domain.model;

import lombok.Builder;
import lombok.Data;

/**
 * Entidad de dominio: datos de la orden que se notificaran por email.
 * Se construye a partir del 'detail' del evento OrderProcessed.
 */
@Data
@Builder
public class OrderNotification {
    private String orderId;
    private String customerId;
    private String productId;
    private Integer quantity;
    private Double amount;
    private String status;
    private String timestamp;
}
