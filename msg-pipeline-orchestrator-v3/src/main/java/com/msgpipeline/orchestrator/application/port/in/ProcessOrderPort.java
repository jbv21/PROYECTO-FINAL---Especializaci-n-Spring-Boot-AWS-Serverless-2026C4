package com.msgpipeline.orchestrator.application.port.in;

import com.msgpipeline.orchestrator.domain.model.Order;

/**
 * PUERTO DE ENTRADA: ProcessOrderPort
 * Contrato del caso de uso principal del Orchestrator.
 *
 * El handler (adaptador de entrada) invoca este puerto sin conocer
 * los detalles de validacion, persistencia ni publicacion de eventos.
 */
public interface ProcessOrderPort {

    /**
     * Excepcion de negocio: la orden no paso la validacion.
     * El handler la traduce a una respuesta HTTP 400.
     */
    class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }

    /**
     * Procesa una orden: valida -> persiste -> publica evento.
     * @param order Orden construida desde el payload (sin orderId aun).
     * @param requestId Identificador del request de API Gateway.
     * @return La orden procesada (con orderId, status y timestamp).
     * @throws ValidationException si el Validator rechaza la orden.
     */
    Order processOrder(Order order, String requestId);
}
