package com.msgpipeline.orchestrator.domain.port.out;

import com.msgpipeline.orchestrator.domain.model.Order;

/**
 * PUERTO DE SALIDA: OrderRepository
 * Define el contrato de persistencia de ordenes SIN acoplarse a DynamoDB.
 *
 * Implementaciones:
 *   - DynamoOrderRepository (perfil 'aws')  -> DynamoDB PutItem real
 *   - InMemoryOrderRepository (perfil 'local') -> mapa en memoria para pruebas
 *
 * PRINCIPIO DIP (SOLID): el caso de uso depende de esta interfaz,
 * no de la implementacion concreta.
 */
public interface OrderRepository {

    /**
     * Persiste la orden en el almacen de datos.
     * @param order Orden a guardar (con orderId ya asignado).
     * @return La orden guardada.
     */
    Order save(Order order);
}
