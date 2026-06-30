# msg-pipeline-validator-v3

Lambda de validación. Es invocado **sincrónicamente** por el Orchestrator v3.
Recibe el payload de la orden y devuelve `{ "valida": boolean, "motivo": string }`.
No consume servicios AWS de salida (solo aplica reglas de negocio).

## Reglas de validación
- `customerId`: obligatorio, no vacío
- `productId`: obligatorio, no vacío
- `quantity`: obligatorio, mayor a 0
- `amount`: obligatorio, mayor a 0

## Handler
```
com.msgpipeline.validator.ValidatorHandler::handleRequest
```

## Contrato
Entrada:
```json
{ "orderId":"uuid", "customerId":"CUST-001", "productId":"PROD-ABC", "quantity":2, "amount":150.00 }
```
Salida:
```json
{ "valida": true, "motivo": "Orden valida -- customerId: CUST-001" }
```

## Variables de entorno
| Variable | Valor |
|---|---|
| `AWS_REGION_NAME` | `us-east-1` |

## Compilar y empaquetar
```bash
./gradlew clean lambdaJar
# -> build/distributions/msg-pipeline-validator-v3-lambda.zip
```

## Permisos IAM del rol (mínimos)
- `logs:CreateLogGroup`, `logs:CreateLogStream`, `logs:PutLogEvents`
