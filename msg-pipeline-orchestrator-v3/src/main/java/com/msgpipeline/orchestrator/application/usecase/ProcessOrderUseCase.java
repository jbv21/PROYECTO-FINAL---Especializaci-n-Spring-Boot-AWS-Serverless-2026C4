package com.msgpipeline.orchestrator.application.usecase;

import com.msgpipeline.orchestrator.application.port.in.ProcessOrderPort;
import com.msgpipeline.orchestrator.domain.model.Order;
import com.msgpipeline.orchestrator.domain.port.out.EventPublisherPort;
import com.msgpipeline.orchestrator.domain.port.out.OrderRepository;
import com.msgpipeline.orchestrator.domain.port.out.ValidatorPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * =========================================================================
 * CLASE: ProcessOrderUseCase -- Caso de Uso del Orchestrator v3
 * CAPA: Aplicacion -- Application Service
 * ARQUITECTURA: Hexagonal + Clean Architecture
 * =========================================================================
 *
 * RESPONSABILIDAD UNICA (SRP -- SOLID):
 *   Orquesta el flujo de procesamiento de una orden:
 *     1. Invocar SINCRONAMENTE al Lambda Validator (ValidatorPort)
 *     2. Si la validacion falla -> lanzar ValidationException (=> HTTP 400)
 *     3. Generar orderId (UUID) y status=PROCESSED
 *     4. Persistir en DynamoDB (OrderRepository)
 *     5. Publicar evento OrderProcessed en EventBridge (EventPublisherPort)
 *
 * No conoce API Gateway, ni Lambda, ni DynamoDB, ni EventBridge.
 * Solo conoce los PUERTOS (interfaces) del dominio. -> DIP (SOLID)
 *
 * FLUJO v3 (segun el diagrama de la implementacion):
 *   Orchestrator -> Validator (paso 3 y 4)
 *   Orchestrator -> DynamoDB  (paso 5)
 *   Orchestrator -> EventBridge (paso 6)
 * =========================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessOrderUseCase implements ProcessOrderPort {

    // Puerto de salida -- validacion via Lambda Validator (invocacion sincrona)
    private final ValidatorPort validatorPort;

    // Puerto de salida -- persistencia DynamoDB
    private final OrderRepository orderRepository;

    // Puerto de salida -- publicacion de eventos EventBridge
    private final EventPublisherPort eventPublisherPort;

    @Override
    public Order processOrder(Order order, String requestId) {
        log.info("Iniciando caso de uso [requestId={}] [customerId={}] [productId={}]",
                requestId, order.getCustomerId(), order.getProductId());

        // -- Paso 1: Asignar orderId (UUID) ---------------------------------
        // El orderId es la PK de DynamoDB. Lo genera el servidor (no el cliente)
        // por seguridad y para garantizar unicidad.
        String orderId = UUID.randomUUID().toString();
        order.setOrderId(orderId);
        log.info("orderId generado [orderId={}]", orderId);

        // -- Paso 2: Validacion sincrona (Lambda Validator v3) --------------
        // El Orchestrator invoca al Validator y espera su respuesta (OK/Error).
        ValidatorPort.ValidationResult validacion = validatorPort.validate(order);
        log.info("Resultado validacion [orderId={}] [valida={}] [motivo={}]",
                orderId, validacion.valida(), validacion.motivo());

        if (!validacion.valida()) {
            // Validacion fallida -> el handler traducira esto a HTTP 400.
            log.warn("Orden rechazada por el Validator [orderId={}] [motivo={}]",
                    orderId, validacion.motivo());
            throw new ValidationException(validacion.motivo());
        }

        // -- Paso 3: Completar datos de la orden ----------------------------
        order.setStatus("PROCESSED");
        order.setTimestamp(Instant.now().toString());

        // -- Paso 4: Persistir en DynamoDB ----------------------------------
        Order saved = orderRepository.save(order);
        log.info("Orden persistida en DynamoDB [orderId={}] [status={}]",
                saved.getOrderId(), saved.getStatus());

        // -- Paso 5: Publicar evento OrderProcessed en EventBridge ----------
        // BEST-EFFORT: si EventBridge falla, la orden ya esta en DynamoDB.
        // La regla de EventBridge enrutara el evento al Lambda Notificacion.
        try {
            String eventId = eventPublisherPort.publishOrderProcessed(saved);
            log.info("Evento OrderProcessed publicado [orderId={}] [eventId={}]",
                    saved.getOrderId(), eventId);
        } catch (Exception e) {
            log.error("Error publicando evento EventBridge [orderId={}]: {}",
                    saved.getOrderId(), e.getMessage(), e);
        }

        log.info("Caso de uso completado [orderId={}] [status={}]",
                saved.getOrderId(), saved.getStatus());
        return saved;
    }
}
