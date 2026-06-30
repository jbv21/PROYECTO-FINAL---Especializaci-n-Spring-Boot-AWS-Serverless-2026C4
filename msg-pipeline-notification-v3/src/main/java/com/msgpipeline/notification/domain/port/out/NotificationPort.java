package com.msgpipeline.notification.domain.port.out;

import com.msgpipeline.notification.domain.model.OrderNotification;

/**
 * PUERTO DE SALIDA: NotificationPort
 * Contrato para enviar la notificacion de la orden procesada.
 *
 * Implementaciones:
 *   - SnsNotificationAdapter (perfil 'aws')   -> SNS Publish real
 *   - InMemoryNotificationAdapter (perfil 'local') -> log local
 */
public interface NotificationPort {

    /**
     * Envia la notificacion de la orden procesada.
     * @param notification Datos de la orden a notificar.
     */
    void notificar(OrderNotification notification);
}
