package com.msgpipeline.validator.domain.model;

import lombok.Builder;
import lombok.Data;

/**
 * Entidad de dominio: representa la orden a validar.
 * POJO puro, sin dependencias de AWS ni frameworks.
 */
@Data
@Builder
public class OrderPayload {
    private String orderId;
    private String customerId;
    private String productId;
    private Integer quantity;
    private Double amount;
}
