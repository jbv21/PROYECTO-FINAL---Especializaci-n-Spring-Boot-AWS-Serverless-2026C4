package com.msgpipeline.orchestrator.domain.port.out;

import com.msgpipeline.orchestrator.domain.model.Order;

/**
 * PUERTO DE SALIDA: EventPublisherPort
 * Contrato para publicar el evento OrderProcessed en un bus de eventos.
 *
 * Implementaciones:
 *   - EventBridgeAdapter (perfil 'aws')   -> EventBridge PutEvents real
 *   - InMemoryEventAdapter (perfil 'local') -> log local para pruebas
 *
 * PATRON OBSERVER: el Orchestrator publica el evento sin conocer
 * a sus suscriptores (la regla de EventBridge enruta al Lambda Notificacion).
 */
public interface EventPublisherPort {

    /**
     * Publica el evento 'OrderProcessed' para la orden indicada.
     * @param order Orden ya persistida.
     * @return El ID del evento generado por el bus (para trazabilidad).
     */
    String publishOrderProcessed(Order order);
}
