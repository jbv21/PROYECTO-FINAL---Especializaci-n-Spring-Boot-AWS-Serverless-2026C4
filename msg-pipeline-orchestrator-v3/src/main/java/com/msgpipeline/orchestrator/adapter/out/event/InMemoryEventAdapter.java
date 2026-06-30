package com.msgpipeline.orchestrator.adapter.out.event;

import com.msgpipeline.orchestrator.domain.model.Order;
import com.msgpipeline.orchestrator.domain.port.out.EventPublisherPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adaptador de eventos EN MEMORIA -- perfil 'local'.
 * Simula la publicacion del evento sin EventBridge real.
 */
@Slf4j
@Component
@Profile("local")
public class InMemoryEventAdapter implements EventPublisherPort {

    @Override
    public String publishOrderProcessed(Order order) {
        String eventId = UUID.randomUUID().toString();
        log.info("[LOCAL] Evento OrderProcessed simulado [orderId={}] [eventId={}]",
                order.getOrderId(), eventId);
        return eventId;
    }
}
