package com.msgpipeline.notification.adapter.out.notification;

import com.msgpipeline.notification.domain.model.OrderNotification;
import com.msgpipeline.notification.domain.port.out.NotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Adaptador de notificacion EN MEMORIA -- perfil 'local'.
 * Simula el envio de email registrando el contenido en el log.
 */
@Slf4j
@Component
@Profile("local")
public class InMemoryNotificationAdapter implements NotificationPort {

    @Override
    public void notificar(OrderNotification n) {
        log.info("[LOCAL] Email simulado [orderId={}] [customerId={}] [amount={}]",
                n.getOrderId(), n.getCustomerId(), n.getAmount());
    }
}
