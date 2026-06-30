package com.msgpipeline.orchestrator.adapter.out.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msgpipeline.orchestrator.domain.model.Order;
import com.msgpipeline.orchestrator.domain.port.out.ValidatorPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * =========================================================================
 * CLASE: LambdaValidatorAdapter -- Adaptador de Salida (Invocacion Lambda)
 * CAPA: Infraestructura -- Output Adapter
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * @Profile("aws"): Solo activo en Lambda.
 *
 * Implementa ValidatorPort invocando SINCRONAMENTE al Lambda Validator v3.
 *   InvocationType: REQUEST_RESPONSE (sincrono -- esperamos la respuesta)
 *   Funcion:        msg-pipeline-validator-v3 (por nombre, env VALIDATOR_FUNCTION_NAME)
 *
 * CONTRATO CON EL VALIDATOR:
 *   Enviamos un JSON con los campos de la orden:
 *     { "orderId","customerId","productId","quantity","amount" }
 *   El Validator responde:
 *     { "valida": true/false, "motivo": "..." }
 *
 * PERMISO IAM REQUERIDO en el rol del Orchestrator:
 *   lambda:InvokeFunction sobre msg-pipeline-validator-v3
 * =========================================================================
 */
@Slf4j
@Component
@Profile("aws")
public class LambdaValidatorAdapter implements ValidatorPort {

    // Cliente Lambda -- inicializado una vez en el cold start (thread-safe)
    private static final LambdaClient lambdaClient =
            LambdaClient.builder().region(Region.US_EAST_1).build();

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Nombre de la funcion Validator, inyectado desde VALIDATOR_FUNCTION_NAME
    @Value("${app.aws.validator-function-name:msg-pipeline-validator-v3}")
    private String validatorFunctionName;

    @Override
    public ValidationResult validate(Order order) {
        log.info("Invocando Lambda Validator [funcion={}] [orderId={}]",
                validatorFunctionName, order.getOrderId());

        try {
            // -- Construir el payload de entrada para el Validator ----------
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("orderId",    order.getOrderId());
            payload.put("customerId", order.getCustomerId());
            payload.put("productId",  order.getProductId());
            payload.put("quantity",   order.getQuantity());
            payload.put("amount",     order.getAmount());

            String jsonPayload = objectMapper.writeValueAsString(payload);

            // -- Invocacion sincrona (REQUEST_RESPONSE) ---------------------
            InvokeResponse response = lambdaClient.invoke(InvokeRequest.builder()
                    .functionName(validatorFunctionName)
                    .invocationType(InvocationType.REQUEST_RESPONSE)
                    .payload(SdkBytes.fromUtf8String(jsonPayload))
                    .build());

            // -- Detectar errores de funcion (FunctionError) ----------------
            if (response.functionError() != null) {
                String errorPayload = response.payload().asUtf8String();
                log.error("El Validator devolvio FunctionError [orderId={}]: {}",
                        order.getOrderId(), errorPayload);
                return new ValidationResult(false,
                        "Error interno del Validator: " + response.functionError());
            }

            // -- Parsear la respuesta del Validator -------------------------
            String responseBody = response.payload().asUtf8String();
            log.info("Respuesta del Validator [orderId={}]: {}",
                    order.getOrderId(), responseBody);

            JsonNode node = objectMapper.readTree(responseBody);
            boolean valida = node.path("valida").asBoolean(false);
            String motivo = node.path("motivo").asText("Sin motivo especificado");

            return new ValidationResult(valida, motivo);

        } catch (Exception e) {
            log.error("Error invocando Lambda Validator [orderId={}]: {}",
                    order.getOrderId(), e.getMessage(), e);
            // Si no podemos validar, tratamos la orden como invalida (fail-safe)
            return new ValidationResult(false,
                    "No se pudo invocar al Validator: " + e.getMessage());
        }
    }
}
