package com.msgpipeline.orchestrator.domain.model;

import lombok.Builder;
import lombok.Data;

/**
 * =========================================================================
 * CLASE: Order -- Entidad de Dominio (Orden de compra)
 * CAPA: Dominio -- Modelo de negocio puro
 * ARQUITECTURA: Hexagonal + Clean Architecture
 * =========================================================================
 *
 * Representa una orden procesada por el pipeline serverless v3.
 * Es un POJO de dominio: NO depende de AWS, HTTP ni frameworks.
 *
 * CAMPOS:
 *   orderId    -> Clave primaria en DynamoDB (PK). Generado con UUID.
 *   customerId -> Identificador del cliente (viene del payload).
 *   productId  -> Identificador del producto (viene del payload).
 *   quantity   -> Cantidad solicitada (debe ser > 0).
 *   amount     -> Monto total de la orden (debe ser > 0).
 *   status     -> Estado de la orden (PROCESSED tras persistir).
 *   timestamp  -> Marca de tiempo ISO-8601 de cuando se proceso.
 * =========================================================================
 */
@Data
@Builder
public class Order {

    /** Clave primaria en DynamoDB. Generada por el Orchestrator (UUID). */
    private String orderId;

    /** Identificador del cliente que realiza la orden. */
    private String customerId;

    /** Identificador del producto solicitado. */
    private String productId;

    /** Cantidad de unidades solicitadas. */
    private Integer quantity;

    /** Monto total de la orden. */
    private Double amount;

    /** Estado de la orden (PROCESSED cuando se persiste correctamente). */
    private String status;

    /** Marca de tiempo ISO-8601 del procesamiento. */
    private String timestamp;
}
