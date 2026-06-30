package com.msgpipeline.orchestrator;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msgpipeline.orchestrator.adapter.in.web.dto.CreateOrderRequest;
import com.msgpipeline.orchestrator.adapter.in.web.dto.CreateOrderResponse;
import com.msgpipeline.orchestrator.application.port.in.ProcessOrderPort;
import com.msgpipeline.orchestrator.config.OrchestratorApplication;
import com.msgpipeline.orchestrator.domain.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;

/**
 * =========================================================================
 * CLASE: OrchestratorHandler -- Lambda Entry Point (API Gateway Proxy)
 * CAPA: Infraestructura -- Input Adapter
 * ARQUITECTURA: Hexagonal + Clean Architecture
 * =========================================================================
 *
 * Punto de entrada del Lambda Orchestrator v3.
 *
 * FLUJO v3:
 *   Postman -> POST /orders-v3 (SIN autenticacion)
 *   API Gateway (proxy) -> OrchestratorHandler::handleRequest
 *   -> ProcessOrderPort.processOrder()
 *        -> Lambda Validator v3 (invocacion sincrona)
 *        -> DynamoDB PutItem
 *        -> EventBridge PutEvents (OrderProcessed)
 *   -> 201 Created (orden procesada) o 400 (validacion fallida)
 *
 * HANDLER: com.msgpipeline.orchestrator.OrchestratorHandler::handleRequest
 * ENV:     DYNAMODB_TABLE_NAME, EVENT_BUS_NAME, VALIDATOR_FUNCTION_NAME, AWS_REGION_NAME
 *
 * IMPORTANTE -- WebApplicationType.SERVLET:
 *   Requerido en Spring Boot 3.5. NONE puede causar ClassCastException.
 *
 * SNAPSTART:
 *   El bloque static inicializa Spring durante la fase de snapshot,
 *   eliminando el cold start en las invocaciones desde el alias 'prod'.
 * =========================================================================
 */
@Slf4j
public class OrchestratorHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    // -- Bloque static -- Cold Start / Snapshot SnapStart --------------------
    // Se ejecuta UNA vez. Con SnapStart, Spring se inicializa en el snapshot.
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ProcessOrderPort processOrderPort;

    static {
        log.info("OrchestratorHandler -- Cold Start (v3)");
        log.info("Trigger: API Gateway (sin auth) -> Validator + DynamoDB + EventBridge");

        ConfigurableApplicationContext context = new SpringApplicationBuilder(OrchestratorApplication.class)
                .web(WebApplicationType.SERVLET)
                .profiles("aws")
                .run();

        processOrderPort = context.getBean(ProcessOrderPort.class);
        log.info("Contexto Spring inicializado. Tabla: {} | Bus: {} | Validator: {}",
                context.getEnvironment().getProperty("app.aws.dynamodb-table"),
                context.getEnvironment().getProperty("app.aws.event-bus-name"),
                context.getEnvironment().getProperty("app.aws.validator-function-name"));
    }

    /** Constructor publico sin argumentos -- OBLIGATORIO para AWS Lambda. */
    public OrchestratorHandler() { }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event, Context context) {

        log.info("Request recibido [httpMethod={}] [path={}] [requestId={}]",
                event.getHttpMethod(), event.getPath(), context.getAwsRequestId());

        try {
            // -- Paso 1: Validar metodo HTTP --------------------------------
            if (!"POST".equalsIgnoreCase(event.getHttpMethod())) {
                return buildResponse(405, "{\"error\":\"Metodo no permitido\"}");
            }

            // -- Paso 2: Validar y parsear el body --------------------------
            String body = event.getBody();
            if (body == null || body.isBlank()) {
                return buildResponse(400, "{\"error\":\"El body no puede estar vacio\"}");
            }

            CreateOrderRequest request = objectMapper.readValue(body, CreateOrderRequest.class);
            log.info("Payload [customerId={}] [productId={}] [quantity={}] [amount={}]",
                    request.getCustomerId(), request.getProductId(),
                    request.getQuantity(), request.getAmount());

            // -- Paso 3: Convertir DTO -> entidad de dominio ----------------
            Order order = Order.builder()
                    .customerId(request.getCustomerId())
                    .productId(request.getProductId())
                    .quantity(request.getQuantity())
                    .amount(request.getAmount())
                    .build();

            // -- Paso 4: Ejecutar el caso de uso ----------------------------
            Order processed = processOrderPort.processOrder(order, context.getAwsRequestId());

            // -- Paso 5: Respuesta 201 Created ------------------------------
            CreateOrderResponse response = CreateOrderResponse.builder()
                    .orderId(processed.getOrderId())
                    .status(processed.getStatus())
                    .message("Orden procesada correctamente")
                    .timestamp(processed.getTimestamp())
                    .requestId(context.getAwsRequestId())
                    .build();

            return buildResponse(201, objectMapper.writeValueAsString(response));

        } catch (ProcessOrderPort.ValidationException e) {
            // -- Validacion fallida -> HTTP 400 -----------------------------
            log.warn("Validacion fallida [requestId={}]: {}",
                    context.getAwsRequestId(), e.getMessage());
            return buildResponse(400, "{\"error\":\"Validacion fallida\",\"motivo\":\""
                    + escape(e.getMessage()) + "\"}");

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("JSON invalido en el body: {}", e.getMessage());
            return buildResponse(400, "{\"error\":\"JSON invalido en el body\"}");

        } catch (Exception e) {
            log.error("Error inesperado [requestId={}]: {}",
                    context.getAwsRequestId(), e.getMessage(), e);
            return buildResponse(500, "{\"error\":\"Error interno\",\"requestId\":\""
                    + context.getAwsRequestId() + "\"}");
        }
    }

    /** Construye la respuesta HTTP para API Gateway (proxy mode) con CORS. */
    private APIGatewayProxyResponseEvent buildResponse(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(Map.of(
                        "Content-Type",                "application/json",
                        "Access-Control-Allow-Origin", "*",
                        "Access-Control-Allow-Headers","Content-Type",
                        "Access-Control-Allow-Methods","POST,OPTIONS"
                ))
                .withBody(body);
    }

    /** Escapa comillas dobles para construir JSON manualmente de forma segura. */
    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}
