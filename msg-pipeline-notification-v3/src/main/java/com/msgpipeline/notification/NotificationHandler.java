package com.msgpipeline.notification;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.msgpipeline.notification.application.port.in.NotifyOrderPort;
import com.msgpipeline.notification.config.NotificationApplication;
import com.msgpipeline.notification.domain.model.OrderNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;

/**
 * =========================================================================
 * CLASE: NotificationHandler -- Lambda Entry Point (EventBridge Consumer)
 * CAPA: Infraestructura -- Input Adapter
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * Suscrito a la regla de EventBridge que filtra el evento OrderProcessed.
 *
 * EVENTO EVENTBRIDGE RECIBIDO (estructura estandar):
 * {
 *   "version": "0",
 *   "id": "event-uuid",
 *   "source": "com.msgpipeline.orchestrator",
 *   "detail-type": "OrderProcessed",
 *   "detail": {
 *     "orderId": "uuid...",
 *     "customerId": "CUST-001",
 *     "productId": "PROD-ABC",
 *     "quantity": 2,
 *     "amount": 150.00,
 *     "status": "PROCESSED",
 *     "timestamp": "2026-..."
 *   }
 * }
 *
 * El campo 'detail' contiene los datos de la orden. De ahi extraemos
 * la informacion para enviar el email via SNS.
 *
 * HANDLER: com.msgpipeline.notification.NotificationHandler::handleRequest
 * ENV:     SNS_TOPIC_ARN, AWS_REGION_NAME
 *
 * SNAPSTART: el bloque static inicializa Spring en el snapshot.
 * =========================================================================
 */
@Slf4j
public class NotificationHandler implements RequestHandler<Map<String, Object>, Void> {

    // -- Bloque static -- Cold Start / Snapshot SnapStart --------------------
    private static final NotifyOrderPort notifyOrderPort;

    static {
        log.info("NotificationHandler -- Cold Start (v3)");
        log.info("Trigger: EventBridge Rule (OrderProcessed) -> SNS Publish");

        ConfigurableApplicationContext context = new SpringApplicationBuilder(NotificationApplication.class)
                .web(WebApplicationType.SERVLET)
                .profiles("aws")
                .run();

        notifyOrderPort = context.getBean(NotifyOrderPort.class);
        log.info("Contexto Spring inicializado. SNS Topic: {}",
                context.getEnvironment().getProperty("app.aws.sns-topic-arn"));
    }

    /** Constructor publico sin argumentos -- OBLIGATORIO para AWS Lambda. */
    public NotificationHandler() { }

    @Override
    @SuppressWarnings("unchecked")
    public Void handleRequest(Map<String, Object> event, Context context) {
        log.info("Evento EventBridge recibido [requestId={}]", context.getAwsRequestId());

        try {
            // -- Extraer el 'detail' del evento de EventBridge --------------
            Object detailObj = event.get("detail");
            if (!(detailObj instanceof Map)) {
                log.error("El evento no contiene un 'detail' valido");
                return null;
            }
            Map<String, Object> detail = (Map<String, Object>) detailObj;

            // -- Construir la entidad de dominio ----------------------------
            OrderNotification notification = OrderNotification.builder()
                    .orderId(asString(detail.get("orderId")))
                    .customerId(asString(detail.get("customerId")))
                    .productId(asString(detail.get("productId")))
                    .quantity(asInteger(detail.get("quantity")))
                    .amount(asDouble(detail.get("amount")))
                    .status(asString(detail.get("status")))
                    .timestamp(asString(detail.get("timestamp")))
                    .build();

            log.info("Notificando orden [orderId={}] [customerId={}]",
                    notification.getOrderId(), notification.getCustomerId());

            // -- Ejecutar el caso de uso ------------------------------------
            notifyOrderPort.notifyOrderProcessed(notification);

            log.info("Evento procesado correctamente [orderId={}]", notification.getOrderId());
            return null;

        } catch (Exception e) {
            log.error("Error en NotificationHandler [requestId={}]: {}",
                    context.getAwsRequestId(), e.getMessage(), e);
            // Relanzamos para que Lambda registre el fallo y reintente segun politica
            throw new RuntimeException("Error procesando evento OrderProcessed: "
                    + e.getMessage(), e);
        }
    }

    // -- Helpers de conversion de tipos --

    private String asString(Object o) {
        return o == null ? null : o.toString();
    }

    private Integer asInteger(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try { return Integer.valueOf(o.toString()); }
        catch (NumberFormatException e) { return null; }
    }

    private Double asDouble(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.valueOf(o.toString()); }
        catch (NumberFormatException e) { return null; }
    }
}
