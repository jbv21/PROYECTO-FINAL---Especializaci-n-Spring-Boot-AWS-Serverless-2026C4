package com.msgpipeline.orchestrator.adapter.out.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msgpipeline.orchestrator.domain.model.Order;
import com.msgpipeline.orchestrator.domain.port.out.EventPublisherPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * =========================================================================
 * CLASE: EventBridgeAdapter -- Adaptador de Salida (AWS EventBridge)
 * CAPA: Infraestructura -- Output Adapter
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * @Profile("aws"): Solo activo en Lambda.
 *
 * Publica el evento 'OrderProcessed' en el bus personalizado v3.
 *   Bus:        msg-pipeline-bus-v3
 *   Source:     com.msgpipeline.orchestrator
 *   DetailType: OrderProcessed   (la Rule filtra por este detail-type)
 *   Target:     Lambda Notificacion v3
 *
 * PATRON OBSERVER: publica sin conocer al suscriptor; EventBridge enruta.
 * =========================================================================
 */
@Slf4j
@Component
@Profile("aws")
public class EventBridgeAdapter implements EventPublisherPort {

    // Cliente EventBridge -- inicializado una vez en el cold start (thread-safe)
    private static final EventBridgeClient eventBridgeClient =
            EventBridgeClient.builder().region(Region.US_EAST_1).build();

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Nombre del bus, inyectado desde la variable de entorno EVENT_BUS_NAME
    @Value("${app.aws.event-bus-name:msg-pipeline-bus-v3}")
    private String eventBusName;

    @Override
    public String publishOrderProcessed(Order order) {
        log.info("EventBridge PutEvents [orderId={}] [bus={}] [detailType=OrderProcessed]",
                order.getOrderId(), eventBusName);

        try {
            // Construimos el 'detail' del evento (lo que recibira el Notificacion)
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("orderId",    order.getOrderId());
            detail.put("customerId", order.getCustomerId());
            detail.put("productId",  order.getProductId());
            detail.put("quantity",   order.getQuantity());
            detail.put("amount",     order.getAmount());
            detail.put("status",     order.getStatus());
            detail.put("timestamp",  order.getTimestamp());

            PutEventsResponse response = eventBridgeClient.putEvents(
                    PutEventsRequest.builder()
                            .entries(PutEventsRequestEntry.builder()
                                    .eventBusName(eventBusName)
                                    .source("com.msgpipeline.orchestrator")
                                    .detailType("OrderProcessed")
                                    .detail(objectMapper.writeValueAsString(detail))
                                    .build())
                            .build());

            // El ID del primer entry sirve para correlacion en la consola
            String eventId = response.entries().isEmpty()
                    ? "unknown"
                    : response.entries().get(0).eventId();

            log.info("EventBridge PutEvents exitoso [orderId={}] [eventId={}] [failedCount={}]",
                    order.getOrderId(), eventId, response.failedEntryCount());
            return eventId;

        } catch (Exception e) {
            log.error("Error en EventBridge PutEvents [orderId={}]: {}",
                    order.getOrderId(), e.getMessage(), e);
            throw new RuntimeException("Fallo al publicar evento OrderProcessed", e);
        }
    }
}
