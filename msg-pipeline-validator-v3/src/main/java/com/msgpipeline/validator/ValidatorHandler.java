package com.msgpipeline.validator;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.msgpipeline.validator.config.ValidatorApplication;
import com.msgpipeline.validator.domain.model.OrderPayload;
import com.msgpipeline.validator.domain.port.in.ValidateOrderPort;
import com.msgpipeline.validator.domain.port.in.ValidateOrderPort.ValidationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.HashMap;
import java.util.Map;

/**
 * =========================================================================
 * CLASE: ValidatorHandler -- Lambda Entry Point (Invocacion directa)
 * CAPA: Infraestructura -- Input Adapter
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * Invocado SINCRONAMENTE por el Lambda Orchestrator v3 (Lambda -> Lambda).
 *
 * INPUT (enviado por el Orchestrator):
 *   {
 *     "orderId": "uuid...",
 *     "customerId": "CUST-001",
 *     "productId": "PROD-ABC",
 *     "quantity": 2,
 *     "amount": 150.00
 *   }
 *
 * OUTPUT (devuelto al Orchestrator):
 *   {
 *     "valida": true/false,
 *     "motivo": "..."
 *   }
 *
 * HANDLER: com.msgpipeline.validator.ValidatorHandler::handleRequest
 *
 * SNAPSTART: el bloque static inicializa Spring en el snapshot,
 * eliminando el cold start desde el alias 'prod'.
 * =========================================================================
 */
@Slf4j
public class ValidatorHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    // -- Bloque static -- Cold Start / Snapshot SnapStart --------------------
    private static final ValidateOrderPort validateOrderPort;

    static {
        log.info("ValidatorHandler -- Cold Start (v3)");
        log.info("Trigger: invocacion sincrona desde el Orchestrator");

        ConfigurableApplicationContext context = new SpringApplicationBuilder(ValidatorApplication.class)
                .web(WebApplicationType.SERVLET)
                .profiles("aws")
                .run();

        validateOrderPort = context.getBean(ValidateOrderPort.class);
        log.info("Contexto Spring inicializado. Puerto de validacion listo.");
    }

    /** Constructor publico sin argumentos -- OBLIGATORIO para AWS Lambda. */
    public ValidatorHandler() { }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        log.info("Evento recibido [requestId={}]", context.getAwsRequestId());

        try {
            // -- Extraer y convertir el payload de entrada ------------------
            OrderPayload payload = OrderPayload.builder()
                    .orderId(asString(event.get("orderId")))
                    .customerId(asString(event.get("customerId")))
                    .productId(asString(event.get("productId")))
                    .quantity(asInteger(event.get("quantity")))
                    .amount(asDouble(event.get("amount")))
                    .build();

            log.info("Validando [orderId={}] [customerId={}] [productId={}]",
                    payload.getOrderId(), payload.getCustomerId(), payload.getProductId());

            // -- Ejecutar el caso de uso ------------------------------------
            ValidationResponse response = validateOrderPort.validate(payload);

            // -- Construir el mapa de salida --------------------------------
            Map<String, Object> output = new HashMap<>();
            output.put("valida", response.valida());
            output.put("motivo", response.motivo());

            log.info("Validacion completada [orderId={}] [valida={}]",
                    payload.getOrderId(), response.valida());
            return output;

        } catch (Exception e) {
            log.error("Error en ValidatorHandler [requestId={}]: {}",
                    context.getAwsRequestId(), e.getMessage(), e);
            Map<String, Object> output = new HashMap<>();
            output.put("valida", false);
            output.put("motivo", "Error interno en validacion: " + e.getMessage());
            return output;
        }
    }

    // -- Helpers de conversion de tipos (el JSON puede traer Number o String) --

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
