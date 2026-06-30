package com.msgpipeline.orchestrator.adapter.out.persistence;

import com.msgpipeline.orchestrator.domain.model.Order;
import com.msgpipeline.orchestrator.domain.port.out.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador de persistencia EN MEMORIA -- perfil 'local'.
 * Permite ejecutar y probar el caso de uso sin DynamoDB real.
 */
@Slf4j
@Repository
@Profile("local")
public class InMemoryOrderRepository implements OrderRepository {

    private final ConcurrentHashMap<String, Order> store = new ConcurrentHashMap<>();

    @Override
    public Order save(Order order) {
        store.put(order.getOrderId(), order);
        log.info("[LOCAL] Orden guardada en memoria [orderId={}] [total={}]",
                order.getOrderId(), store.size());
        return order;
    }
}
