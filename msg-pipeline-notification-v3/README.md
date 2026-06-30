# msg-pipeline-notification-v3

Lambda suscriptor de la regla de EventBridge. Recibe el evento `OrderProcessed`
y publica un email de notificación en el topic SNS.

## Arquitectura
```
NotificationHandler (EventBridge consumer)
  -> NotifyOrderUseCase
       -> NotificationPort -> SnsNotificationAdapter (SNS Publish)
```

## Handler
```
com.msgpipeline.notification.NotificationHandler::handleRequest
```

## Variables de entorno
| Variable | Valor de ejemplo |
|---|---|
| `SNS_TOPIC_ARN` | `arn:aws:sns:us-east-1:<ACCOUNT_ID>:msg-pipeline-notifications-v3` |
| `AWS_REGION_NAME` | `us-east-1` |

## Compilar y empaquetar
```bash
./gradlew clean lambdaJar
# -> build/distributions/msg-pipeline-notification-v3-lambda.zip
```

## Permisos IAM del rol (mínimos)
- `sns:Publish` sobre `msg-pipeline-notifications-v3`
- `logs:CreateLogGroup`, `logs:CreateLogStream`, `logs:PutLogEvents`
