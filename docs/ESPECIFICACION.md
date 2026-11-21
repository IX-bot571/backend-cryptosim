# CryptoSim — Especificación (EP5 DSY1107)

Monitor de criptomonedas con alertas personalizadas. Proyecto académico: **solo lee precios y avisa; no opera ni usa dinero real.**

Plazo de entrega: **jueves 26-nov-2026, 08:30**. Trabajo individual. El último día real de desarrollo es el miércoles 25.

---

## 1. Objetivo y qué se evalúa

El enunciado evalúa la integración de **Kafka** a una arquitectura que ya usa **RabbitMQ**, sin romper lo anterior. Pesos de la rúbrica (todo es Kafka):

| Indicador | Peso |
|---|---|
| Config. del cliente Kafka (bootstrap, serializers, groups, props avanzadas) | 12 |
| Productores y consumidores con DTO tipados | 12 |
| Productores correctos (DTO + serializers) | 14 |
| Consumidores con `@KafkaListener`, lógica separada de la infraestructura | 14 |
| Manejo de errores (logging, validación, reintentos) | 12 |
| Parámetros de tópicos (retención, cleanup.policy, particiones, replicación) | 12 |
| Consumer groups (balanceo / resiliencia) | 12 |
| Microservicio admin Kafka: crear, listar, administrar tópicos y configs | 6 |
| Admin: operaciones sobre tópicos y consumer groups | 6 |

Instrucciones del enunciado que también deben cumplirse: JWT validado en backend (IDaaS), login con IDaaS en el frontend, BD cloud con entidades y repositorios, RabbitMQ con productores/consumidores/DLQ y microservicio admin, `docker-compose` de RabbitMQ, de Kafka (Zookeeper + brokers) y de Kafka-UI, `.gitignore` correcto, código que compile, front modular sin errores.

**Prioridad absoluta: que lo anterior funcione y se pueda demostrar.** Lo visual va después.

---

## 2. Stack (heredado de la entrega anterior, no cambiar)

- Java 21, Spring Boot 4.1.1 (Maven wrapper, starters modulares de Boot 4), Lombok.
- Angular 22, `keycloak-js` (login PKCE S256), Node 20.19+.
- IDaaS: **Keycloak 26** (realm `cloudnative`, cliente público `angular-app`). Backend valida JWT con `oauth2ResourceServer`.
- BD cloud: **PostgreSQL en Neon**. La contraseña va solo en `application-local.yml` (ignorado por Git).
- RabbitMQ 3 (nodo único). Kafka con Zookeeper y 3 brokers (imágenes Confluent 7.x).
- Boot 4 usa **Jackson 3**: en RabbitMQ ya se usa `JacksonJsonMessageConverter`. Para Kafka usar los serializers JSON de Jackson 3 de Spring Kafka 4 (`JacksonJsonSerializer` / `JacksonJsonDeserializer`). Si el nombre no existe en la versión resuelta, comprobarlo en los JAR antes de improvisar. Usar `spring-boot-starter-kafka` si está disponible, o `spring-kafka` con la versión que gestiona el BOM.

---

## 3. Alcance

### MVP (obligatorio)
1. Precios reales de 10 criptos desde Binance, con **modo simulado de respaldo**.
2. Cada usuario elige qué criptos **sigue** (lista de seguimiento).
3. **Alertas simples**: precio ≥ valor, o precio ≤ valor. Se disparan una sola vez.
4. Notificaciones de alertas disparadas (historial en pantalla).
5. Admin de Kafka y admin de RabbitMQ.
6. Frontend con 4 pantallas: Mercado, Seguimiento, Mis alertas, Notificaciones.

### Extras (solo si el MVP está completo y probado; en este orden)
1. Alerta **VARIACION_VENTANA** (ver sección 11).
2. Panel admin en el frontend (tópicos y consumer groups).
3. Comparador base 100 y resumen de alertas.

### Fuera de alcance
Órdenes, saldos, trading real, velas, múltiples monedas fiat, email o push reales, clúster RabbitMQ (el enunciado habla de "clúster"; se deja nodo único y se pregunta al docente si basta; si lo exige, se agrega al final).

---

## 4. Catálogo de criptos (fijo, configurable por propiedad)

| simbolo | nombre | par Binance |
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

Se define en `application.yml` (`app.catalogo`) de **ms-precios** y de **ms-alertas** (misma lista; documentarlo en `CONTRATO.md`). Al arrancar, ms-precios comprueba que cada par exista; si alguno no existe, lo registra en el log, lo omite y sigue con los demás. En el evento y en toda la API el identificador es `simbolo` (por ejemplo `BTC`), no el par.

---

## 5. Arquitectura

```
Angular (4200) ──JWT──► ms-alertas (8081) ─────► Neon (PostgreSQL)
        │                    ▲  │  │
        │                    │  │  └─(Rabbit) tareas.exchange ─► notificaciones.queue ─► ms-notificaciones (8082)
        │                    │  │                                     └─ DLQ: notificaciones.dlq
        │        precios.actualizados (Kafka)                    ▲
        │                    │  └─ alertas.disparadas (Kafka) ───┘  (consumo en ms-notificaciones)
        └──JWT──► ms-precios (8084) ── Binance / modo simulado
        └──JWT──► ms-admin-kafka (8085)     ms-admin-rabbit (8083, sin cambios)
```

| Servicio | Puerto | Origen | Rol |
|---|---|---|---|
| ms-alertas | 8081 | **renombrar y adaptar `ms-pedidos`** | REST + BD, consume precios, publica alertas disparadas, envía aviso por RabbitMQ |
| ms-notificaciones | 8082 | adaptar existente | consumidor RabbitMQ (con DLQ) + consumidor Kafka |
| ms-admin-rabbit | 8083 | **sin cambios** | administración RabbitMQ |
| ms-precios | 8084 | **nuevo** | obtiene precios y los publica en Kafka |
| ms-admin-kafka | 8085 | **nuevo** | administración de Kafka |

Otros puertos: Keycloak 8080, Kafka-UI 8090, RabbitMQ 5672/15672, Zookeeper 2181, brokers externos 9092/9093/9094.

---

## 6. Kafka

### Tópicos

| Tópico | Productor | Clave | Particiones | Replicación | min.insync | retention.ms | cleanup.policy |
|---|---|---|---|---|---|---|---|
| `precios.actualizados` | ms-precios | `simbolo` | 3 | 3 | 2 | 86400000 (24 h) | delete |
| `alertas.disparadas` | ms-alertas | `usuarioId` | 2 | 3 | 2 | 604800000 (7 d) | delete |
| `precios.actualizados.DLT` | (error handler) | — | 1 | 3 | 2 | 604800000 | delete |
| `alertas.disparadas.DLT` | (error handler) | — | 1 | 3 | 2 | 604800000 | delete |

- La retención de 24 h de `precios.actualizados` permite reconstruir ventanas de tiempo (extra). Documentar este razonamiento en el informe.
- Cada servicio **dueño** de un tópico lo declara con `TopicBuilder`/`NewTopic` en una clase `KafkaTopicConfig`.
- Los DLT se declaran donde se configura el error handler. Ojo: `DeadLetterPublishingRecoverer` por defecto usa la **misma partición** del registro original; como los DLT tienen 1 partición, configurar el resolver de destino con partición `-1` (decide el productor) o crearlos con las mismas particiones.
- Con RF=3 hacen falta los 3 brokers arriba. Verificar con `kafka-topics --describe`.

### Eventos (DTO como `record`, JSON)

```java
record PrecioEvent(String simbolo, BigDecimal precio, BigDecimal variacion24hPct,
                   BigDecimal maximo24h, BigDecimal minimo24h, BigDecimal volumen24h,
                   Instant timestamp, String fuente /* BINANCE | SIMULADO */) {}

record AlertaDisparadaEvent(Long alertaId, String usuarioId, String simbolo, String tipo,
                            BigDecimal valorObjetivo, BigDecimal precioDisparo,
                            Instant timestamp, String mensaje) {}
```

Validación del payload al consumir (si falla, es **no reintentable** y va al DLT): `simbolo` no vacío y dentro del catálogo, `precio` > 0, `timestamp` presente.

### Configuración del cliente (centralizada, sin literales dispersos)

- Un `@ConfigurationProperties(prefix = "app.kafka")` con nombres de tópicos y grupos. Ninguna clase escribe nombres a mano.
- Productor: `acks=all`, `enable.idempotence=true`, key `StringSerializer`, value JSON (Jackson 3). Bootstrap `localhost:9092,localhost:9093,localhost:9094`.
- Consumidor: `ErrorHandlingDeserializer` envolviendo el deserializador JSON, `trusted packages` restringidos al paquete de los DTO, `enable-auto-commit=false`, `ack-mode=record`.
- `auto-offset-reset`: `latest` para `alertas-group` (no evaluar precios viejos), `earliest` para `notificaciones-group`.

### Consumer groups

| Grupo | Servicio | Tópico |
|---|---|---|
| `alertas-group` | ms-alertas | `precios.actualizados` |
| `notificaciones-group` | ms-notificaciones | `alertas.disparadas` |

Demostración de balanceo: levantar **2 instancias de ms-alertas** (la segunda con `--server.port=8086`) y ver en Kafka-UI cómo se reparten las 3 particiones. Como la clave es el símbolo, cada cripto cae siempre en la misma partición y mantiene su orden.

### Manejo de errores (productores y consumidores)

- `DefaultErrorHandler` con `ExponentialBackOffWithMaxRetries` (3 reintentos; 1 s, 2 s, 4 s).
- `DeadLetterPublishingRecoverer` hacia `<tópico>.DLT`, conservando los headers de excepción.
- Excepciones **no reintentables** (`addNotRetryableExceptions`): deserialización fallida, `PayloadInvalidoException`.
- Excepciones **reintentables**: fallos transitorios (BD no disponible, etc.).
- Logging contextual en todo error: tópico, partición, offset, clave y causa.
- Un consumidor `@KafkaListener` adicional sobre cada DLT que **registra el mensaje fallido en el log** (como ya se hace con `notificaciones.dlq`).
- Productor: callback de `send(...)` que registra éxito (partición/offset) y error.

### Separación de responsabilidades (rúbrica)

El `@KafkaListener` solo recibe el DTO, valida y delega a un `@Service` de dominio (por ejemplo `AlertaEvaluadorService`). La lógica de negocio no conoce Kafka.

---

## 7. Servicios en detalle

### 7.1 ms-precios (nuevo, 8084)

- `BinanceClient` (Spring `RestClient`): `GET https://data-api.binance.vision/api/v3/ticker/24hr?symbols=[...]`. Campos usados: `symbol`, `lastPrice`, `priceChangePercent`, `highPrice`, `lowPrice`, `volume`. Sin API key.
- `SimuladorPrecios`: caminata aleatoria con `BigDecimal` a partir de un precio base por cripto; la volatilidad es configurable para forzar cruces en la demo.
- Propiedades: `app.precios.modo` (`binance` | `simulado` | `binance-con-respaldo`, por defecto la última), `app.precios.intervalo-ms` (5000), `app.catalogo`, `app.historial.max-puntos` (1440).
- `@Scheduled` que obtiene los precios (si Binance falla en modo con respaldo, usa el simulador y marca `fuente=SIMULADO`) y publica **un evento por cripto** en `precios.actualizados` con clave `simbolo`.
- Historial en memoria por cripto (buffer circular).
- REST (JWT):
  - `GET /api/catalogo`
  - `GET /api/precios` → último `PrecioEvent` de cada cripto
  - `GET /api/precios/{simbolo}/historial?minutos=60` → lista de `{timestamp, precio}`
- CORS para `http://localhost:4200`.

### 7.2 ms-alertas (renombrar `ms-pedidos`, 8081)

Renombrar paquete a `com.proyecto.ms_alertas`, reemplazar la entidad y los DTO de pedidos. Mantener `SecurityConfig` (JWT + CORS), conexión a Neon y estructura por capas.

**Entidades**
- `Alerta`: `id`, `usuarioId` (claim `sub` del JWT), `simbolo`, `tipo` (`PRECIO_MAYOR_IGUAL` | `PRECIO_MENOR_IGUAL`), `valor` (BigDecimal), `estado` (`ACTIVA` | `DISPARADA` | `CANCELADA`), `creadaEn`, `disparadaEn`, `precioDisparo`.
- `Seguimiento`: `id`, `usuarioId`, `simbolo`; único `(usuarioId, simbolo)`.

**REST (JWT; el usuario sale del token, nunca del cuerpo)**
- `POST /api/alertas` `{simbolo, tipo, valor}` → 201. Valida: símbolo en catálogo, valor > 0, tipo válido.
- `GET /api/alertas` → alertas del usuario (filtro opcional `?estado=`)
- `DELETE /api/alertas/{id}` → pasa a `CANCELADA` (solo si es del usuario y está ACTIVA)
- `GET /api/seguimiento` → lista de símbolos seguidos
- `PUT /api/seguimiento` `{simbolos:[...]}` → reemplaza la lista (valida contra el catálogo)

Si al crear una alerta la condición ya se cumple, se permite: se dispara con el siguiente precio (útil en la demo).

**Consumo y evaluación**
- `@KafkaListener(topics = "${app.kafka.topics.precios}", groupId = "alertas-group")` recibe `PrecioEvent`, valida y llama a `AlertaEvaluadorService.evaluar(precio)`.
- El servicio busca alertas `ACTIVA` del símbolo y delega en una interfaz `ReglaAlerta { boolean cumple(Alerta a, PrecioEvent p); }` con una implementación por tipo (patrón estrategia, para poder agregar `VARIACION_VENTANA`).
- Disparo **idempotente**: pasar a `DISPARADA` con un `UPDATE ... WHERE id=? AND estado='ACTIVA'` (o `@Version`); solo si afectó 1 fila se publica el evento. Así dos instancias o un reintento no duplican la alerta.
- Al disparar: guardar `precioDisparo` y `disparadaEn`, publicar `AlertaDisparadaEvent` en `alertas.disparadas` (clave `usuarioId`).
- Al **crear** una alerta se envía por RabbitMQ un `NotificacionMensaje` de confirmación (flujo RabbitMQ existente, con su DLQ intacta).

### 7.3 ms-notificaciones (8082)

- Mantener el consumidor RabbitMQ actual (ACK manual, reintentos, DLQ, `NotificacionDlqConsumer`). Solo cambia el DTO: `NotificacionMensaje(Long referenciaId, String usuario, String mensaje)`. Actualizar el contrato en **ambos** servicios y en `CONTRATO.md`.
- Nuevo `@KafkaListener` sobre `alertas.disparadas` (`notificaciones-group`): valida, delega a un `@Service` que “envía” la notificación (simulado: log estructurado). Mismo manejo de errores y DLT.
- Mantener el gancho de prueba existente que fuerza un fallo temporal.

### 7.4 ms-admin-kafka (nuevo, 8085)

`AdminClient` de Kafka (bean único, bootstrap por propiedad), con `SecurityConfig` JWT como `ms-admin-rabbit`. Estructura: `controller` → `service` → `AdminClient`, DTOs `record`, `GlobalExceptionHandler`.

| Método y ruta | Acción |
|---|---|
| `GET /api/topics` | lista tópicos con particiones, factor de replicación y configs |
| `GET /api/topics/{name}` | detalle de un tópico |
| `POST /api/topics` | crea `{name, particiones, replicas, configs:{...}}` |
| `PUT /api/topics/{name}/config` | modifica configs (`retention.ms`, `cleanup.policy`, `min.insync.replicas`…) con `incrementalAlterConfigs` |
| `PUT /api/topics/{name}/partitions` | aumenta particiones (`createPartitions`) |
| `DELETE /api/topics/{name}` | elimina |
| `GET /api/consumer-groups` | lista grupos y estado |
| `GET /api/consumer-groups/{id}` | miembros, offsets comprometidos y **lag** por partición |

Respuestas claras de error (404 si no existe, 409 si ya existe, 400 por validación).

### 7.5 ms-admin-rabbit (8083)

Sin cambios. Solo se reutiliza.

---

## 8. Infraestructura (`infra/` en el repo del backend)

Red Docker compartida `cryptosim-net` (créala con `docker network create cryptosim-net`; los compose la declaran como `external: true`).

```
infra/
  rabbitmq/docker-compose.yml     # rabbitmq:3-management, 5672 y 15672
  kafka/docker-compose.yml        # zookeeper + kafka1/2/3
  kafka-ui/docker-compose.yml     # Kafka-UI en 8090
  keycloak/docker-compose.yml     # Keycloak 26 en 8080, con --import-realm
  keycloak/cloudnative-realm.json # el existente
```

**Kafka:** Zookeeper (`cp-zookeeper` 7.x) + 3 brokers (`cp-kafka` 7.x), `KAFKA_BROKER_ID` 1-3, `KAFKA_HEAP_OPTS="-Xms256m -Xmx512m"`, dos listeners por broker: interno (`kafkaN:29092`, para Kafka-UI y entre contenedores) y externo (`localhost:9092/9093/9094`, para los servicios en el host). Configurar `KAFKA_ADVERTISED_LISTENERS`, `KAFKA_LISTENER_SECURITY_PROTOCOL_MAP`, `KAFKA_INTER_BROKER_LISTENER_NAME`, `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=3`, `KAFKA_MIN_INSYNC_REPLICAS=2`, `KAFKA_AUTO_CREATE_TOPICS_ENABLE=false`. Healthchecks y `depends_on`.

**Kafka-UI:** `provectuslabs/kafka-ui`, cluster `local` apuntando a `kafka1:29092,kafka2:29092,kafka3:29092`, y Zookeeper si la imagen lo requiere.

**Keycloak:** montar `cloudnative-realm.json` y arrancar con `start-dev --import-realm` para que el realm sea reproducible. Debe existir al menos un usuario de prueba (con email, nombre y apellido).

**Notas de máquina:** el desarrollo se hace en Linux (Nobara), donde Docker corre nativo. El laptop tiene 16 GB: no levantar más de lo necesario a la vez.

---

## 9. Frontend (Angular 22, repo aparte)

Reutilizar tal cual `AuthService` (Keycloak, PKCE) y `app.config.ts` (login antes de mostrar la app). Ampliar el `authInterceptor` para que añada el JWT a las llamadas a los puertos **8081, 8084 y 8085** (centralizar las URLs base en un archivo de entorno/constantes, no repartidas en servicios).

Componentes *standalone*, servicios por dominio (`PreciosService`, `AlertasService`, `SeguimientoService`), señales para el estado, rutas por pantalla.

| Ruta | Pantalla | Contenido |
|---|---|---|
| `/mercado` | Mercado | Tarjetas de las criptos **seguidas**: precio grande, variación 24 h (verde/rojo), máx/mín, mini gráfica SVG (con `historial`). Actualización cada 5 s; el precio parpadea al subir o bajar. |
| `/seguimiento` | Seguimiento | Las 10 criptos del catálogo con interruptor para seguir o dejar de seguir. |
| `/alertas` | Mis alertas | Formulario (cripto, tipo, valor) con un resumen en lenguaje natural (“Avísame si BTC ≥ 100.000”) y lista con estado y botón cancelar. |
| `/notificaciones` | Notificaciones | Alertas disparadas (cripto, condición, precio al disparo, hora). Refresco periódico. |

**Diseño:** tema oscuro tipo dashboard financiero, variables CSS (colores, espaciados, radios), tarjetas con borde sutil, tipografía legible, verde/rojo con buen contraste, barra superior con usuario y cierre de sesión, responsive. Gráficas con **SVG propio, sin librerías de gráficos**. Evitar dependencias nuevas salvo necesidad real.

Texto visible al usuario en español. Mensaje claro en pantalla cuando un servicio no responde (sin pantalla en blanco).

---

## 10. Reglas transversales

- Todo nombre (tópicos, grupos, colas, exchanges) sale de `application.yml` vía `@ConfigurationProperties`.
- DTOs como `record`; dinero y precios siempre `BigDecimal`; fechas con `Instant`.
- Validación con Bean Validation; errores REST con `@RestControllerAdvice`.
- CORS solo para `http://localhost:4200`.
- Secretos fuera de Git (`application-local.yml`, variables de entorno).
- `.gitignore` por repo: `target/`, `node_modules/`, `dist/`, `.angular/`, `.idea/`, `.vscode/`, `*.iml`, `application-local.yml`, `.env`.
- Pruebas mínimas: `contextLoads` por servicio y un test unitario por cada `ReglaAlerta` (casos: cumple, no cumple, borde exacto).
- Logs contextuales con SLF4J (`@Slf4j`), sin `System.out`.
- No tocar `ms-admin-rabbit`. No romper el flujo RabbitMQ existente.

---

## 11. Extra: alerta VARIACION_VENTANA (solo con el MVP terminado)

Ejemplo: “avísame si BTC **sube** 1 % en **120** minutos”.

- Campos nuevos en `Alerta`: `direccion` (`SUBE` | `BAJA` | `AMBAS`), `porcentaje`, `ventanaMinutos`.
- **Interpretación acordada (a):** se compara el precio actual contra el precio que tenía hace `ventanaMinutos`. `variacion = (actual − referencia) / referencia × 100`.
- **Regla por dirección:** `SUBE` se cumple si `variacion ≥ +porcentaje`; `BAJA` si `variacion ≤ −porcentaje`; `AMBAS` si `|variacion| ≥ porcentaje`. Con `AMBAS` se dispara una sola vez, con el primer movimiento que ocurra, y el mensaje indica si fue subida o bajada. El `porcentaje` es siempre positivo en la API.
- Estado en memoria en ms-alertas: por símbolo, una cola de `(timestamp, precio)` que descarta lo anterior a la ventana más larga activa. La referencia es el primer punto con `timestamp ≥ ahora − ventana`.
- Se implementa como una nueva `ReglaAlerta` sin tocar las demás.
- Al reiniciar el servicio se pierde la ventana. Mitigación: al arrancar, releer `precios.actualizados` desde `ahora − ventana` (por `offsetsForTimes`) antes de empezar a evaluar.
- Para la demo, `ventanaMinutos` puede ser 1-2 min con el modo simulado y un porcentaje bajo (por ejemplo 0,05 %).

---

## 12. Plan por etapas con criterios de aceptación

Trabajar **una etapa a la vez**, con commit al terminar cada una.

| # | Etapa | Está lista cuando |
|---|---|---|
| 1 | `infra/`: Kafka (ZK + 3 brokers), Kafka-UI, Keycloak (realm importado), RabbitMQ | Kafka-UI en `localhost:8090` muestra el clúster con 3 brokers; Keycloak permite login con el usuario de prueba; RabbitMQ en `15672` |
| 2 | ms-precios | Se ven mensajes de `precios.actualizados` en Kafka-UI cada 5 s, 3 particiones, RF=3; `GET /api/precios` responde con JWT; el modo simulado funciona si se corta la red |
| 3 | ms-alertas (renombrar y adaptar) | Crear alerta por API; con precio simulado que cruza el valor, pasa a DISPARADA y aparece un mensaje en `alertas.disparadas`; con 2 instancias se reparten particiones |
| 4 | Errores y DLT | Un mensaje malo publicado a mano en `precios.actualizados` termina en `precios.actualizados.DLT` tras los reintentos y queda en el log; el consumo de los demás mensajes no se interrumpe |
| 5 | ms-notificaciones | El evento Kafka y el mensaje RabbitMQ se procesan; el fallo temporal forzado termina en `notificaciones.dlq` |
| 6 | ms-admin-kafka | Se crea un tópico, se cambia su `retention.ms`, se aumentan particiones, se lista el lag de `alertas-group` y se elimina el tópico, todo vía REST con JWT |
| 7 | Frontend | Login Keycloak, las 4 pantallas funcionan con datos reales, sin errores de compilación |
| 8 | Prueba completa y documentación | Clon limpio de ambos repos → se levanta todo con el README → el guion de demo se cumple de punta a punta |

### Guion de demo (debe poder ejecutarse en ~8 minutos)
1. Mostrar Kafka-UI: clúster, tópicos, particiones y réplicas.
2. Login en el frontend; seguir 3 criptos; ver los precios moverse.
3. Crear una alerta que se cumple pronto; mostrar el disparo en pantalla y el evento en `alertas.disparadas`.
4. Publicar un mensaje inválido → mostrarlo en el DLT y en el log.
5. Levantar la 2.ª instancia de ms-alertas y ver el reparto de particiones.
6. Desde `ms-admin-kafka`: cambiar la retención de un tópico y consultar el lag de un grupo.
7. Mostrar que RabbitMQ sigue funcionando (confirmación de alerta creada y un caso de DLQ).

---

## 13. Estructura de repos

- **Backend** (`backend-cryptosim`): `infra/`, `ms-precios/`, `ms-alertas/`, `ms-notificaciones/`, `ms-admin-kafka/`, `ms-admin-rabbit/`, `CONTRATO.md`, `README.md`, `.gitignore`.
- **Frontend** (`frontend-cryptosim`): proyecto Angular con su `README.md` y `.gitignore`.

`README.md` del backend: requisitos, cómo levantar la infraestructura, cómo crear `application-local.yml`, cómo arrancar cada servicio y cómo ejecutar el guion de demo.
`CONTRATO.md`: puertos, tópicos (con particiones, RF y retención), DTOs, colas RabbitMQ, catálogo y endpoints.
