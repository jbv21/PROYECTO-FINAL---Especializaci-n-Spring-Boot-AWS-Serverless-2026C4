package com.msgpipeline.notification.application.port.in;

import com.msgpipeline.notification.domain.model.OrderNotification;

/**
 * PUERTO DE ENTRADA: NotifyOrderPort
 * Contrato del caso de uso de notificacion.
 */
public interface NotifyOrderPort {

    /**
     * Procesa el evento OrderProcessed y envia la notificacion.
     * @param notification Datos de la orden extraidos del evento.
     */
    void notifyOrderProcessed(OrderNotification notification);
}
