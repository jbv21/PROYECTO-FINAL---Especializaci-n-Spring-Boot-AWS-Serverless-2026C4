package com.msgpipeline.validator.application.validation;

import com.msgpipeline.validator.domain.model.OrderPayload;
import com.msgpipeline.validator.domain.port.in.ValidateOrderPort.ValidationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * =========================================================================
 * CLASE: OrderValidator -- Reglas de validacion de la orden
 * CAPA: Aplicacion -- Logica de validacion
 * =========================================================================
 *
 * Aplica las reglas de negocio sobre el payload de la orden:
 *   - customerId: obligatorio, no vacio
 *   - productId:  obligatorio, no vacio
 *   - quantity:   obligatorio, mayor a 0
 *   - amount:     obligatorio, mayor a 0
 *
 * Devuelve el PRIMER error encontrado (fail-fast) o un resultado valido.
 * =========================================================================
 */
@Slf4j
@Component
public class OrderValidator {

    public ValidationResponse validar(OrderPayload p) {
        // Regla 1: customerId obligatorio
        if (isBlank(p.getCustomerId())) {
            return ValidationResponse.invalido("El campo 'customerId' es obligatorio");
        }
        // Regla 2: productId obligatorio
        if (isBlank(p.getProductId())) {
            return ValidationResponse.invalido("El campo 'productId' es obligatorio");
        }
        // Regla 3: quantity > 0
        if (p.getQuantity() == null) {
            return ValidationResponse.invalido("El campo 'quantity' es obligatorio");
        }
        if (p.getQuantity() <= 0) {
            return ValidationResponse.invalido("El campo 'quantity' debe ser mayor a 0");
        }
        // Regla 4: amount > 0
        if (p.getAmount() == null) {
            return ValidationResponse.invalido("El campo 'amount' es obligatorio");
        }
        if (p.getAmount() <= 0) {
            return ValidationResponse.invalido("El campo 'amount' debe ser mayor a 0");
        }
        return ValidationResponse.valido("Orden valida -- customerId: " + p.getCustomerId());
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
