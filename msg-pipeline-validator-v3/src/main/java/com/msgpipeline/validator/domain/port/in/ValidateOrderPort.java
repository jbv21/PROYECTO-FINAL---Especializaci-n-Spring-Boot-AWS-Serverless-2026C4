package com.msgpipeline.validator.domain.port.in;

import com.msgpipeline.validator.domain.model.OrderPayload;

/**
 * PUERTO DE ENTRADA: ValidateOrderPort
 * Contrato del caso de uso de validacion de ordenes.
 */
public interface ValidateOrderPort {

    /**
     * Respuesta de validacion.
     * @param valida true si la orden cumple todas las reglas.
     * @param motivo Descripcion del resultado o del primer error encontrado.
     */
    record ValidationResponse(boolean valida, String motivo) {
        public static ValidationResponse valido(String motivo) {
            return new ValidationResponse(true, motivo);
        }
        public static ValidationResponse invalido(String motivo) {
            return new ValidationResponse(false, motivo);
        }
    }

    /**
     * Valida la orden segun las reglas de negocio.
     * @param payload Orden a validar.
     * @return Resultado con veredicto y motivo.
     */
    ValidationResponse validate(OrderPayload payload);
}
