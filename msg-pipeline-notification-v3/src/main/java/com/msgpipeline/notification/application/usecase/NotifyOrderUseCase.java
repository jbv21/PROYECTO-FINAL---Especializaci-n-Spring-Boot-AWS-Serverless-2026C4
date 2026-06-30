package com.msgpipeline.notification.application.usecase;

import com.msgpipeline.notification.application.port.in.NotifyOrderPort;
import com.msgpipeline.notification.domain.model.OrderNotification;
import com.msgpipeline.notification.domain.port.out.NotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * =========================================================================
 * CLASE: NotifyOrderUseCase -- Caso de Uso de Notificacion
 * CAPA: Aplicacion -- Application Service
 * ARQUITECTURA: Hexagonal + Clean Architecture
 * =========================================================================
 *
 * RESPONSABILIDAD UNICA (SRP): coordinar el envio de la notificacion.
 * Delega el envio concreto en NotificationPort (SNS en perfil 'aws').
 * No conoce EventBridge ni SNS directamente. -> DIP (SOLID)
 * =========================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyOrderUseCase implements NotifyOrderPort {

    private final NotificationPort notificationPort;

    @Override
    public void notifyOrderProcessed(OrderNotification notification) {
        log.info("Procesando notificacion [orderId={}] [customerId={}]",
                notification.getOrderId(), notification.getCustomerId());

        notificationPort.notificar(notification);

        log.info("Notificacion enviada [orderId={}]", notification.getOrderId());
    }
}
