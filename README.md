# msg-pipeline v3 — Arquitectura Serverless en AWS (Java 17 + Spring Boot 3.5)

Implementación independiente de la versión **v3** del pipeline serverless,
construida con arquitectura Hexagonal (Ports & Adapters) + Clean Architecture.

Contiene tres proyectos Spring Boot, cada uno en su propia carpeta con su
`build.gradle` y task `lambdaJar`:

| Proyecto | Rol | Handler |
|---|---|---|
| `msg-pipeline-orchestrator-v3` | Entrada HTTP, valida, persiste y publica evento | `com.msgpipeline.orchestrator.OrchestratorHandler::handleRequest` |
| `msg-pipeline-validator-v3` | Validación de la orden (invocación síncrona) | `com.msgpipeline.validator.ValidatorHandler::handleRequest` |
| `msg-pipeline-notification-v3` | Consume EventBridge y envía email por SNS | `com.msgpipeline.notification.NotificationHandler::handleRequest` |

---

## Flujo de la arquitectura v3

```
1. Cliente (Postman) --> API Gateway v3 : POST /orders-v3 (sin autenticacion)
2. API Gateway v3    --> Lambda Orchestrator v3 (invocacion sincrona)
3. Orchestrator v3   --> Lambda Validator v3 (validacion del payload)
4. Validator v3      --> Respuesta OK/Error al Orchestrator
5. Orchestrator v3   --> DynamoDB : registro de la orden procesada
6. Orchestrator v3   --> EventBridge : publicacion del evento OrderProcessed
7. EventBridge       --> Lambda Notificacion v3 (suscriptor de la regla)
8. Notificacion v3   --> SNS Topic : envio de email de notificacion
```

Si el Validator responde inválido, el Orchestrator devuelve **HTTP 400** y NO
persiste en DynamoDB ni publica el evento.

---

## Payload de ejemplo

```json
{
  "customerId": "CUST-001",
  "productId": "PROD-ABC",
  "quantity": 2,
  "amount": 150.00
}
```

Respuesta exitosa (HTTP 201):

```json
{
  "orderId": "f1c2...",
  "status": "PROCESSED",
  "message": "Orden procesada correctamente",
  "timestamp": "2026-...",
  "requestId": "..."
}
```

Respuesta de validación fallida (HTTP 400):

```json
{
  "error": "Validacion fallida",
  "motivo": "El campo 'quantity' debe ser mayor a 0"
}
```

---

## 1. Compilar los tres proyectos

Requisitos: JDK 17 (Amazon Corretto 17 recomendado).

```bash
cd msg-pipeline-orchestrator-v3 && ./gradlew clean lambdaJar && cd ..
cd msg-pipeline-validator-v3    && ./gradlew clean lambdaJar && cd ..
cd msg-pipeline-notification-v3 && ./gradlew clean lambdaJar && cd ..
```

Cada uno genera su ZIP en `build/distributions/*-lambda.zip`.

---

## 2. Crear los recursos AWS (región us-east-1)

> Prefijo `msg-pipeline-` y sufijo `-v3` en todos los recursos.

### 2.1 DynamoDB
- Tabla: `msg-pipeline-orders-v3`
- Clave primaria (PK): `orderId` (String)
- Modo de capacidad: **On-demand** (para evitar cargos por capacidad)

### 2.2 SNS
- Topic: `msg-pipeline-notifications-v3`
- Crear una suscripción tipo **Email** con tu correo y **confirmar** desde el
  enlace que llega al buzón (la suscripción debe quedar en estado *Confirmed*).

### 2.3 EventBridge
- Bus de eventos personalizado: `msg-pipeline-bus-v3`
- Regla: `OrderProcessed` sobre ese bus, con patrón de evento:
  ```json
  {
    "source": ["com.msgpipeline.orchestrator"],
    "detail-type": ["OrderProcessed"]
  }
  ```
- Target de la regla: el Lambda `msg-pipeline-notification-v3`.

### 2.4 Los tres Lambdas
Para cada función, configurar:

| Parámetro | Valor |
|---|---|
| Runtime | `java17` (Amazon Corretto 17) |
| Arquitectura | `x86_64` (requerido para SnapStart) |
| Memoria | 512 MB (mínimo) |
| Timeout | 30 segundos |
| SnapStart | `ApplyOn: PublishedVersions` |

Subir el ZIP correspondiente y configurar el **handler**:

- `msg-pipeline-orchestrator-v3` → `com.msgpipeline.orchestrator.OrchestratorHandler::handleRequest`
- `msg-pipeline-validator-v3` → `com.msgpipeline.validator.ValidatorHandler::handleRequest`
- `msg-pipeline-notification-v3` → `com.msgpipeline.notification.NotificationHandler::handleRequest`

#### Variables de entorno
Orchestrator:
```
DYNAMODB_TABLE_NAME     = msg-pipeline-orders-v3
EVENT_BUS_NAME          = msg-pipeline-bus-v3
VALIDATOR_FUNCTION_NAME = msg-pipeline-validator-v3
AWS_REGION_NAME         = us-east-1
```
Notificación:
```
SNS_TOPIC_ARN   = arn:aws:sns:us-east-1:<ACCOUNT_ID>:msg-pipeline-notifications-v3
AWS_REGION_NAME = us-east-1
```
Validator:
```
AWS_REGION_NAME = us-east-1
```

#### Publicar versión y alias (los tres Lambdas)
1. Con SnapStart en `PublishedVersions`, **publicar una versión** (Actions → Publish new version).
2. Crear un **alias** llamado `prod` apuntando a la última versión publicada.
3. Verificar en la pestaña *Versions* que existe al menos una versión (no solo `$LATEST`)
   y que el alias `prod` apunta a ella.

### 2.5 Permisos IAM (rol de ejecución por Lambda)
- **Orchestrator**: `lambda:InvokeFunction` sobre `msg-pipeline-validator-v3`,
  `dynamodb:PutItem` sobre la tabla, `events:PutEvents` sobre el bus, y permisos de CloudWatch Logs.
- **Validator**: solo CloudWatch Logs.
- **Notificación**: `sns:Publish` sobre el topic, y CloudWatch Logs.
- En la regla de EventBridge, conceder permiso de invocación al Lambda de Notificación
  (la consola lo agrega automáticamente al asignar el target).

### 2.6 API Gateway (REST)
- API REST nueva.
- Recurso `/orders-v3`, método **POST**.
- Integración: **Lambda Proxy** apuntando al alias `prod` del Orchestrator
  (`msg-pipeline-orchestrator-v3:prod`).
- **Sin** Cognito Authorizer (autenticación `NONE`).
- Deploy a un stage (por ejemplo `prod`) y copiar la *Invoke URL*.

---

## 3. Probar end-to-end (Postman)

POST a `https://<api-id>.execute-api.us-east-1.amazonaws.com/<stage>/orders-v3`

Caso exitoso (201): enviar el payload de ejemplo de arriba.
Caso de validación fallida (400): enviar, por ejemplo, `"quantity": 0`.

El email de SNS debe llegar al correo suscrito con los datos de la orden.

---

## 4. Evidencias para la entrega (mapa a la rúbrica)

1. **Repositorio GitHub público** con los tres proyectos.
2. **Postman**: capturas del POST 201 (éxito) y POST 400 (validación), con body de
   request y response visibles.
3. **CloudWatch**: log groups de Orchestrator, Validator y Notificación con una
   ejecución exitosa en cada uno.
4. **DynamoDB**: captura del ítem creado (`orderId`, `customerId`, `productId`,
   `quantity`, `amount`, `status`, `timestamp`).
5. **Email SNS**: captura del correo recibido con el contenido del evento.
6. **Consola AWS**: API Gateway, cada Lambda (con SnapStart visible), EventBridge,
   DynamoDB y SNS con su configuración.
7. **Versiones y alias**: pestaña *Versions* de cada Lambda mostrando la versión
   publicada y el alias `prod`.

---

## Notas de diseño

- **Perfiles Spring**: el perfil `aws` activa los adaptadores reales (DynamoDB,
  EventBridge, Lambda invoke, SNS). El perfil `local` usa adaptadores en memoria
  para pruebas sin AWS.
- **SnapStart**: el contexto Spring se inicializa en el bloque `static` del handler,
  por lo que queda capturado en el snapshot y se elimina el cold start desde `prod`.
- **Región**: fija `us-east-1` en los clientes del AWS SDK v2.
