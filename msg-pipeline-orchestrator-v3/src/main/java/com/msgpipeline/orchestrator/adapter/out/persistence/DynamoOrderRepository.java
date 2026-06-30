package com.msgpipeline.orchestrator.adapter.out.persistence;

import com.msgpipeline.orchestrator.domain.model.Order;
import com.msgpipeline.orchestrator.domain.port.out.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * =========================================================================
 * CLASE: DynamoOrderRepository -- Adaptador de Salida (DynamoDB PutItem)
 * CAPA: Infraestructura -- Output Adapter
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * @Profile("aws"): Solo activo cuando el Lambda corre en AWS.
 *
 * Persiste la orden en la tabla DynamoDB (modo on-demand).
 *   PK: orderId (String)
 *   Atributos: customerId, productId, quantity, amount, status, timestamp
 *
 * El DynamoDbClient se inicializa UNA SOLA VEZ en el bloque static
 * (cold start). Es thread-safe. Region fija us-east-1.
 * =========================================================================
 */
@Slf4j
@Repository
@Profile("aws")
public class DynamoOrderRepository implements OrderRepository {

    // Cliente DynamoDB -- inicializado una vez en el cold start (thread-safe)
    private static final DynamoDbClient dynamoDbClient =
            DynamoDbClient.builder().region(Region.US_EAST_1).build();

    // Nombre de la tabla, inyectado desde la variable de entorno DYNAMODB_TABLE_NAME
    @Value("${app.aws.dynamodb-table:msg-pipeline-orders-v3}")
    private String tableName;

    @Override
    public Order save(Order order) {
        log.info("DynamoDB PutItem [orderId={}] [tabla={}] [status={}]",
                order.getOrderId(), tableName, order.getStatus());

        // Construimos el item con sus atributos tipados
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("orderId", AttributeValue.fromS(order.getOrderId()));     // PK
        putStr(item, "customerId", order.getCustomerId());
        putStr(item, "productId",  order.getProductId());
        if (order.getQuantity() != null) {
            item.put("quantity", AttributeValue.fromN(order.getQuantity().toString()));
        }
        if (order.getAmount() != null) {
            item.put("amount", AttributeValue.fromN(order.getAmount().toString()));
        }
        putStr(item, "status",    order.getStatus());
        putStr(item, "timestamp", order.getTimestamp());

        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build());

        log.info("PutItem exitoso [orderId={}]", order.getOrderId());
        return order;
    }

    /** Inserta un atributo String solo si no es nulo ni vacio. */
    private void putStr(Map<String, AttributeValue> item, String key, String value) {
        if (value != null && !value.isBlank()) {
            item.put(key, AttributeValue.fromS(value));
        }
    }
}
