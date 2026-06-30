package com.msgpipeline.orchestrator.domain.port.out;

import com.msgpipeline.orchestrator.domain.model.Order;

/**
 * PUERTO DE SALIDA: ValidatorPort
 * Contrato para validar una orden invocando al Lambda Validator v3.
 *
 * Implementaciones:
 *   - LambdaValidatorAdapter (perfil 'aws')   -> Invoca Lambda Validator real
 *   - InMemoryValidatorAdapter (perfil 'local') -> validacion local para pruebas
 *
 * Esta abstraccion permite que el caso de uso NO sepa que la validacion
 * se hace via invocacion sincrona de otro Lambda.
 */
public interface ValidatorPort {

    /**
     * Resultado de la validacion devuelto por el Lambda Validator.
     * @param valida true si la orden es valida, false en caso contrario.
     * @param motivo Descripcion del resultado o del error de validacion.
     */
    record ValidationResult(boolean valida, String motivo) {}

    /**
     * Valida la orden invocando sincronamente al Lambda Validator v3.
     * @param order Orden a validar.
     * @return Resultado con el veredicto y el motivo.
     */
    ValidationResult validate(Order order);
}
