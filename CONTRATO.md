# Contrato del sistema — CryptoSim

Todos los nombres de tópicos, grupos, colas y exchanges viven solo en el `application.yml` de cada servicio (vía `@ConfigurationProperties`). Este documento es su referencia.

## Puertos
| Componente | Puerto |
|---|---|
| Keycloak (realm `cloudnative`) | 8080 |
| ms-alertas | 8081 |
| ms-notificaciones | 8082 |
| ms-admin-rabbit | 8083 |
| ms-precios | 8084 |
| ms-admin-kafka | 8085 |
| ms-alertas (2.ª instancia, demo) | 8086 |
| Kafka-UI | 8090 |
| RabbitMQ (AMQP / consola) | 5672 / 15672 |
| Kafka brokers (externo) | 9092, 9093, 9094 (interno `kafkaN:29092`) |
| Zookeeper | 2181 |
| Frontend | 4200 |

## Seguridad
- Todas las APIs REST exigen `Authorization: Bearer <JWT>` emitido por `http://localhost:8080/realms/cloudnative`.
- El usuario se toma siempre del claim `sub` del JWT, nunca del cuerpo.
- CORS permitido solo para `http://localhost:4200`.

## Catálogo (fijo, en `app.catalogo` de ms-precios y ms-alertas)
| Símbolo | Nombre | Par Binance |
|---|---|---|
| BTC | Bitcoin | BTCUSDT |
| ETH | Ethereum | ETHUSDT |
| BNB | BNB | BNBUSDT |
| SOL | Solana | SOLUSDT |
| XRP | XRP | XRPUSDT |
| ADA | Cardano | ADAUSDT |
| DOGE | Dogecoin | DOGEUSDT |
| NEAR | NEAR Protocol | NEARUSDT |
| RENDER | Render | RENDERUSDT |
| THETA | Theta Network | THETAUSDT |

Precios en USDT. Fuente: `https://data-api.binance.vision` (`/api/v3/ticker/24hr`), con respaldo simulado.

## Kafka

### Tópicos
| Tópico | Productor (declara) | Clave | Particiones | RF | min.insync | retention.ms | cleanup.policy |
|---|---|---|---|---|---|---|---|
| `precios.actualizados` | ms-precios | `simbolo` | 3 | 3 | 2 | 86400000 (24 h) | delete |
| `alertas.disparadas` | ms-alertas | `usuarioId` | 2 | 3 | 2 | 604800000 (7 d) | delete |
| `precios.actualizados.DLT` | error handler de ms-alertas | clave original | 1 | 3 | 2 | 604800000 | delete |
| `alertas.disparadas.DLT` | error handler de ms-notificaciones | clave original | 1 | 3 | 2 | 604800000 | delete |

### Consumer groups
| Grupo | Servicio | Tópico | auto-offset-reset |
|---|---|---|---|
| `alertas-group` | ms-alertas | `precios.actualizados` | latest |
| `alertas-dlt-group` | ms-alertas | `precios.actualizados.DLT` | earliest |
| `notificaciones-group` | ms-notificaciones | `alertas.disparadas` | earliest |
| `notificaciones-dlt-group` | ms-notificaciones | `alertas.disparadas.DLT` | earliest |

### Cliente
- Productor: `acks=all`, `enable.idempotence=true`, key `StringSerializer`, value `JacksonJsonSerializer` (sin type headers).
- Consumidor: `ErrorHandlingDeserializer` → `JacksonJsonDeserializer`, trusted packages = paquete `dto` del servicio, `enable-auto-commit=false`, `ack-mode=record`.
- Errores: `DefaultErrorHandler` con `ExponentialBackOffWithMaxRetries` (3 reintentos: 1 s, 2 s, 4 s) y `DeadLetterPublishingRecoverer` → `<tópico>.DLT` (partición elegida por el productor, headers `kafka_dlt-exception-*`). No reintentables: deserialización fallida y `PayloadInvalidoException`.

### Eventos (JSON)
```java
record PrecioEvent(String simbolo, BigDecimal precio, BigDecimal variacion24hPct,
                   BigDecimal maximo24h, BigDecimal minimo24h, BigDecimal volumen24h,
                   Instant timestamp, String fuente /* BINANCE | SIMULADO */) {}

record AlertaDisparadaEvent(Long alertaId, String usuarioId, String simbolo, String tipo,
                            BigDecimal valorObjetivo, BigDecimal precioDisparo,
                            Instant timestamp, String mensaje) {}
```
Validación al consumir `PrecioEvent`: `simbolo` no vacío y dentro del catálogo, `precio` > 0, `timestamp` presente.
Validación al consumir `AlertaDisparadaEvent`: `alertaId`, `usuarioId`, `simbolo` y `timestamp` presentes, `precioDisparo` > 0.

## RabbitMQ (heredado, sin cambios de topología)
| Elemento | Nombre |
|---|---|
| Exchange principal (direct) | `tareas.exchange` |
| Cola | `notificaciones.queue` |
| Routing key | `notificacion.enviar` |
| Exchange de fallos (direct) | `tareas.dlx` |
| DLQ | `notificaciones.dlq` |
| Routing key DLQ | `notificacion.fallida` |

Productor: ms-alertas, al crear una alerta. Consumidor: ms-notificaciones, ACK manual; `MensajeInvalido` → DLQ directo; `FalloTemporal` (mensaje que contiene `FALLO_TEMPORAL`) → hasta 3 intentos y luego DLQ.

```java
record NotificacionMensaje(Long referenciaId /* id de la alerta */, String usuario /* sub del JWT */, String mensaje) {}
```

## API REST

### ms-precios (8084)
| Método | Ruta | Respuesta |
|---|---|---|
| GET | `/api/catalogo` | `[{simbolo, nombre}]` |
| GET | `/api/precios` | `[PrecioEvent]` — último precio de cada cripto |
| GET | `/api/precios/{simbolo}/historial?minutos=60` (1–1440) | `[{timestamp, precio}]`; 404 si el símbolo no existe |

### ms-alertas (8081)
| Método | Ruta | Cuerpo / respuesta |
|---|---|---|
| POST | `/api/alertas` | `AlertaRequest` → 201 `AlertaResponse` |
| GET | `/api/alertas?estado=ACTIVA\|DISPARADA\|CANCELADA` | `[AlertaResponse]` del usuario |
| DELETE | `/api/alertas/{id}` | 204; 404 si no es del usuario; 409 si no está ACTIVA |
| GET | `/api/seguimiento` | `{simbolos: [..]}` |
| PUT | `/api/seguimiento` | `{simbolos: [..]}` → reemplaza la lista |

```java
record AlertaRequest(@NotBlank String simbolo, @NotNull TipoAlerta tipo, @NotNull @Positive BigDecimal valor) {}
record AlertaResponse(Long id, String simbolo, TipoAlerta tipo, BigDecimal valor, EstadoAlerta estado,
                      Instant creadaEn, Instant disparadaEn, BigDecimal precioDisparo) {}
enum TipoAlerta  { PRECIO_MAYOR_IGUAL, PRECIO_MENOR_IGUAL }
enum EstadoAlerta { ACTIVA, DISPARADA, CANCELADA }
```
Errores: 400 con detalle de validación (incluye símbolo fuera del catálogo).

### ms-admin-kafka (8085)
| Método | Ruta | Cuerpo / respuesta |
|---|---|---|
| GET | `/api/topics` | `[{nombre, particiones, replicas, configs}]` |
| GET | `/api/topics/{name}` | `{nombre, particiones, replicas, configs, detalleParticiones: [{particion, lider, replicas, isr}]}` |
| POST | `/api/topics` | `{name, particiones, replicas, configs?}` → 201 |
| PUT | `/api/topics/{name}/config` | `{configs: {"retention.ms": "..."}}` |
| PUT | `/api/topics/{name}/partitions` | `{particiones: n}` (solo aumentar) |
| DELETE | `/api/topics/{name}` | 204 |
| GET | `/api/consumer-groups` | `[{groupId, estado, tipo}]` |
| GET | `/api/consumer-groups/{id}` | `{groupId, estado, coordinador, miembros, offsets, lagTotal}` |

Errores: 404 tópico/grupo inexistente, 409 tópico ya existe, 400 petición inválida, 503 Kafka no disponible, 502 otro error del clúster.

### ms-admin-rabbit (8083)
- `POST /api/queues`, `DELETE /api/queues/{name}`
- `POST /api/exchanges`, `DELETE /api/exchanges/{name}`
- `POST /api/bindings`, `DELETE /api/bindings`
