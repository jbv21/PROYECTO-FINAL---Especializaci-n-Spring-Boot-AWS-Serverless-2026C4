package com.msgpipeline.validator.application.usecase;

import com.msgpipeline.validator.application.validation.OrderValidator;
import com.msgpipeline.validator.domain.model.OrderPayload;
import com.msgpipeline.validator.domain.port.in.ValidateOrderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * =========================================================================
 * CLASE: ValidateOrderUseCase -- Caso de Uso de Validacion
 * CAPA: Aplicacion -- Application Service
 * ARQUITECTURA: Hexagonal + Clean Architecture
 * =========================================================================
 *
 * RESPONSABILIDAD UNICA (SRP): orquestar la validacion de la orden.
 * Delega las reglas concretas en OrderValidator. No conoce AWS ni Lambda.
 * =========================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValidateOrderUseCase implements ValidateOrderPort {

    private final OrderValidator orderValidator;

    @Override
    public ValidationResponse validate(OrderPayload payload) {
        log.info("Iniciando validacion [orderId={}] [customerId={}]",
                payload.getOrderId(), payload.getCustomerId());

        ValidationResponse response = orderValidator.validar(payload);

        if (response.valida()) {
            log.info("Validacion exitosa [orderId={}]", payload.getOrderId());
        } else {
            log.warn("Validacion fallida [orderId={}] [motivo={}]",
                    payload.getOrderId(), response.motivo());
        }
        return response;
    }
}
