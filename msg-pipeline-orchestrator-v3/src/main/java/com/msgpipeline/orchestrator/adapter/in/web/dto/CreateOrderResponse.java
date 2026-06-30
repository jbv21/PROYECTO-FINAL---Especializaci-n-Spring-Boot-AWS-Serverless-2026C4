package com.msgpipeline.orchestrator.adapter.in.web.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO de SALIDA -- Respuesta del POST /orders-v3
 *
 * Devuelve al cliente el resultado del procesamiento de la orden.
 */
@Data
@Builder
public class CreateOrderResponse {
    private String orderId;
    private String status;
    private String message;
    private String timestamp;
    private String requestId;
}
