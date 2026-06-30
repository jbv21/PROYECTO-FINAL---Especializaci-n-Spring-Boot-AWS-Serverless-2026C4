# msg-pipeline-orchestrator-v3

Lambda de entrada del pipeline serverless v3. Recibe `POST /orders-v3` desde API
Gateway (sin autenticación), invoca **sincrónicamente** al Lambda Validator v3,
persiste la orden en DynamoDB y publica el evento `OrderProcessed` en EventBridge.

## Arquitectura
Hexagonal (Ports & Adapters) + Clean Architecture.

```
OrchestratorHandler (API Gateway Proxy)
  -> ProcessOrderUseCase
       -> ValidatorPort        -> LambdaValidatorAdapter   (invoke sincrono Validator)
       -> OrderRepository      -> DynamoOrderRepository     (PutItem)
       -> EventPublisherPort   -> EventBridgeAdapter        (PutEvents OrderProcessed)
```

## Handler
```
com.msgpipeline.orchestrator.OrchestratorHandler::handleRequest
```

## Variables de entorno
| Variable | Valor de ejemplo |
|---|---|
| `DYNAMODB_TABLE_NAME` | `msg-pipeline-orders-v3` |
| `EVENT_BUS_NAME` | `msg-pipeline-bus-v3` |
| `VALIDATOR_FUNCTION_NAME` | `msg-pipeline-validator-v3` |
| `AWS_REGION_NAME` | `us-east-1` |

## Compilar y empaquetar
```bash
./gradlew clean lambdaJar
# -> build/distributions/msg-pipeline-orchestrator-v3-lambda.zip
```

## Permisos IAM del rol (mínimos)
- `lambda:InvokeFunction` sobre `msg-pipeline-validator-v3`
- `dynamodb:PutItem` sobre la tabla `msg-pipeline-orders-v3`
- `events:PutEvents` sobre el bus `msg-pipeline-bus-v3`
- `logs:CreateLogGroup`, `logs:CreateLogStream`, `logs:PutLogEvents`

## Ejecución local (perfil local, sin AWS)
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```
