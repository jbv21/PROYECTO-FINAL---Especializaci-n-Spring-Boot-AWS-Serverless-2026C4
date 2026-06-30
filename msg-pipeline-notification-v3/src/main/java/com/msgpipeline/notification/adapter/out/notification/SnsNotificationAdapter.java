package com.msgpipeline.notification.adapter.out.notification;

import com.msgpipeline.notification.domain.model.OrderNotification;
import com.msgpipeline.notification.domain.port.out.NotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

/**
 * =========================================================================
 * CLASE: SnsNotificationAdapter -- Adaptador de Salida (AWS SNS)
 * CAPA: Infraestructura -- Output Adapter
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * @Profile("aws"): Solo activo en Lambda.
 *
 * Publica un email en el topic SNS con los datos de la orden procesada.
 *   Topic: msg-pipeline-notifications-v3 (ARN en env SNS_TOPIC_ARN)
 *
 * El SnsClient se inicializa UNA vez en el cold start (thread-safe).
 * Region fija us-east-1.
 * =========================================================================
 */
@Slf4j
@Component
@Profile("aws")
public class SnsNotificationAdapter implements NotificationPort {

    private static final SnsClient snsClient =
            SnsClient.builder().region(Region.US_EAST_1).build();

    // ARN del topic, inyectado desde la variable de entorno SNS_TOPIC_ARN
    @Value("${app.aws.sns-topic-arn:}")
    private String snsTopicArn;

    @Override
    public void notificar(OrderNotification n) {
        if (snsTopicArn == null || snsTopicArn.isBlank()) {
            log.warn("SNS_TOPIC_ARN no configurado [orderId={}]", n.getOrderId());
            return;
        }

        // Cuerpo del email de notificacion
        String cuerpo = "Orden Procesada -- msg-pipeline v3\n\n"
                + "Order ID:    " + n.getOrderId()    + "\n"
                + "Customer ID: " + n.getCustomerId() + "\n"
                + "Product ID:  " + n.getProductId()  + "\n"
                + "Quantity:    " + n.getQuantity()   + "\n"
                + "Amount:      " + n.getAmount()     + "\n"
                + "Status:      " + n.getStatus()     + "\n"
                + "Timestamp:   " + n.getTimestamp()  + "\n\n"
                + "Este es un mensaje automatico del pipeline serverless v3.";

        PublishResponse response = snsClient.publish(PublishRequest.builder()
                .topicArn(snsTopicArn)
                .subject("Orden Procesada -- " + n.getOrderId())
                .message(cuerpo)
                .build());

        log.info("SNS Publish exitoso [orderId={}] [snsMessageId={}]",
                n.getOrderId(), response.messageId());
    }
}
