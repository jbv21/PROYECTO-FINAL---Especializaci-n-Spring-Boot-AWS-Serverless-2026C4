package com.msgpipeline.orchestrator.adapter.in.web.dto;

import lombok.Data;

/**
 * DTO de ENTRADA -- Payload del POST /orders-v3
 *
 * Mapea el JSON recibido desde API Gateway:
 * {
 *   "customerId": "CUST-001",
 *   "productId": "PROD-ABC",
 *   "quantity": 2,
 *   "amount": 150.00
 * }
 *
 * Es un objeto del mundo HTTP. El handler lo convierte a la entidad Order.
 */
@Data
public class CreateOrderRequest {
    private String customerId;
    private String productId;
    private Integer quantity;
    private Double amount;
}
