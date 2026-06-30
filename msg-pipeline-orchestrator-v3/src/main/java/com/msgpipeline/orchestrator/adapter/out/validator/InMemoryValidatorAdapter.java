package com.msgpipeline.orchestrator.adapter.out.validator;

import com.msgpipeline.orchestrator.domain.model.Order;
import com.msgpipeline.orchestrator.domain.port.out.ValidatorPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Adaptador de validacion EN MEMORIA -- perfil 'local'.
 * Aplica las MISMAS reglas que el Lambda Validator v3, pero sin invocacion
 * remota, para poder probar el Orchestrator localmente.
 */
@Slf4j
@Component
@Profile("local")
public class InMemoryValidatorAdapter implements ValidatorPort {

    @Override
    public ValidationResult validate(Order order) {
        log.info("[LOCAL] Validando orden en memoria [orderId={}]", order.getOrderId());

        if (order.getCustomerId() == null || order.getCustomerId().isBlank()) {
            return new ValidationResult(false, "customerId es obligatorio");
        }
        if (order.getProductId() == null || order.getProductId().isBlank()) {
            return new ValidationResult(false, "productId es obligatorio");
        }
        if (order.getQuantity() == null || order.getQuantity() <= 0) {
            return new ValidationResult(false, "quantity debe ser mayor a 0");
        }
        if (order.getAmount() == null || order.getAmount() <= 0) {
            return new ValidationResult(false, "amount debe ser mayor a 0");
        }
        return new ValidationResult(true, "Orden valida");
    }
}
