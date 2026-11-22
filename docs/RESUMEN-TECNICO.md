# CryptoSim — Resumen técnico (estado real del código)

Fecha: **domingo 22-nov-2026**. Redactado leyendo los archivos de los dos repositorios (`backend-cryptosim` y `frontend-cryptosim`), no la especificación. Cuando algo difiere de `docs/ESPECIFICACION.md` se indica con **⚠ Diferencia**.
Las rutas son relativas a la raíz de cada repositorio. Abreviaturas de paquetes: `…/ms_alertas/` = `ms-alertas/src/main/java/com/proyecto/ms_alertas/` (igual para los demás servicios).
No se incluyen contraseñas: la de Neon vive solo en `ms-alertas/src/main/resources/application-local.yml`, ignorado por Git.

---

## 1. Inventario

"Probado" significa que se ejecutó contra la infraestructura real y se observó el resultado (detalle en `docs/ESTADO.md`, sección 2). La última verificación completa es del 22-nov-2026, con `./levantar.sh`.

| Etapa / extra | ¿Implementado? | ¿Probado ejecutándolo? | Archivos principales |
|---|---|---|---|
| 1. Infraestructura | Sí | Sí: 3 brokers (`kafka-broker-api-versions`), login en Keycloak, consola de RabbitMQ | `infra/kafka/docker-compose.yml`, `infra/kafka-ui/docker-compose.yml`, `infra/rabbitmq/docker-compose.yml`, `infra/keycloak/docker-compose.yml`, `infra/keycloak/cloudnative-realm.json` |
| 2. ms-precios | Sí | Sí: mensajes cada 5 s, 3 particiones con RF 3, `GET /api/precios` con JWT, respaldo simulado con Binance caído (servidor falso que devolvía error) | `ms-precios/src/main/resources/application.yml`, `…/ms_precios/client/BinanceClient.java`, `service/ObtencionPreciosService.java`, `service/SimuladorPrecios.java`, `service/HistorialPrecios.java`, `scheduler/PreciosScheduler.java`, `producer/PrecioPublisher.java`, `controller/PreciosController.java`, `config/KafkaTopicConfig.java` |
| 3. ms-alertas | Sí | Sí: alerta creada por API pasa a DISPARADA y aparece en `alertas.disparadas`; con 2 instancias las particiones se reparten 2 + 1 | `…/ms_alertas/controller/*`, `service/AlertaService.java`, `service/AlertaEvaluadorService.java`, `regla/*`, `repository/AlertaRepository.java`, `consumer/PrecioConsumer.java`, `producer/AlertaDisparadaPublisher.java` |
| 4. Errores y DLT | Sí | Sí: un mensaje inválido llega a `precios.actualizados.DLT` sin reintentos; los fallos transitorios se reintentan 1-2-4 s (con el gancho de prueba) | `…/ms_alertas/config/KafkaErrorConfig.java`, `consumer/PrecioEventValidator.java`, `consumer/PrecioDltConsumer.java`, `service/FalloTransitorioSimulado.java` |
| 5. ms-notificaciones | Sí | Sí: el evento Kafka se registra en el log; el mensaje RabbitMQ con `FALLO_TEMPORAL` hace 3 intentos y termina en `notificaciones.dlq` | `…/ms_notificaciones/consumer/AlertaDisparadaConsumer.java`, `AlertaDisparadaValidator.java`, `AlertaDltConsumer.java`, `NotificacionConsumer.java`, `config/KafkaErrorConfig.java`, `service/NotificacionAlertaService.java` |
| 6. ms-admin-kafka | Sí | Sí: crear tópico, cambiar `retention.ms`, aumentar particiones, ver lag y eliminar, todo por REST con JWT | `…/ms_admin_kafka/service/KafkaAdminService.java`, `controller/TopicController.java`, `controller/ConsumerGroupController.java`, `config/AdminClientConfig.java` |
| 7. Frontend (4 pantallas) | Sí | Sí: login real, las 4 pantallas con datos reales (prueba del usuario en su navegador); `ng build` sin avisos | `frontend-cryptosim/src/app/**` |
| 8. Prueba completa y documentación | Sí | Sí: clon limpio de ambos repos y guion de demo de punta a punta | `README.md`, `CONTRATO.md`, `frontend-cryptosim/README.md`, `docs/ESTADO.md`, `levantar.sh`, `detener.sh` |
| Extra: alerta `VARIACION_VENTANA` | Sí | Sí: alerta "±0,05 % en 2 min" disparada; al reiniciar se releyeron 390 precios; 9 tests unitarios | `…/ms_alertas/regla/VariacionVentanaRegla.java`, `service/VentanaPrecios.java`, `consumer/VentanaReplay.java`, `src/main/resources/db/ajustes-esquema.sql`, `src/test/.../VariacionVentanaReglaTest.java`; frontend `src/app/pages/alertas/*` |
| Extra: panel Admin Kafka | Sí | Sí: cada operación probada por API con el CORS de :4200; captura de pantalla con Chrome headless | `frontend-cryptosim/src/app/pages/admin/admin.ts`, `admin.html`, `src/app/core/admin-kafka.service.ts` |
| Extra: comparador base 100 | Sí | Sí: capturas con Chrome headless, incluido el tooltip al pasar el cursor (sin errores en consola) | `frontend-cryptosim/src/app/shared/comparador.ts`, `src/app/pages/mercado/*` |
| Extra: resumen de alertas | Sí | Sí: captura con Chrome headless | `frontend-cryptosim/src/app/pages/alertas/alertas.ts` (`resumenAlertas`), `alertas.html` |

Pruebas automáticas (`./mvnw test`): 20 tests, 0 fallos. Hay un `contextLoads` en cada servicio, más 15 tests de reglas en `ms-alertas/src/test/java/com/proyecto/ms_alertas/regla/`. El frontend no tiene tests: no hay archivos `*.spec.ts`.

---

## 2. Versiones y herramientas

| Herramienta | Versión | Fuente |
|---|---|---|
| Java | 21 (`java.version` en cada `pom.xml`); JDK local OpenJDK 21.0.2 | `ms-*/pom.xml` |
| Spring Boot | 4.1.1 (parent) | `ms-*/pom.xml` |
| Spring Kafka | 4.1.1 (vía `spring-boot-starter-kafka`) | dependencia resuelta |
| kafka-clients | 4.2.1 | dependencia resuelta |
| Jackson | 3 (Boot 4): `JacksonJsonSerializer` / `JacksonJsonDeserializer` / `JacksonJsonMessageConverter` | `application.yml`, `RabbitConfig.java` |
| Maven | 3.10.0 vía wrapper 3.3.4 | `ms-*/.mvn/wrapper/maven-wrapper.properties` |
| Angular | 22.2.3 (CLI 22.2.4) | `frontend-cryptosim/package.json` |
| keycloak-js | ^26.2.4 | `package.json` |
| TypeScript / RxJS | ~6.0.2 / ~7.8.0 | `package.json` |
| Node | 24.22.0 (local) | `node -v` |

Imágenes Docker:

| Imagen | Tag en el compose | Versión real en ejecución |
|---|---|---|
| `confluentinc/cp-zookeeper` | `7.7.1` | 7.7.1 |
| `confluentinc/cp-kafka` | `7.7.1` | 7.7.1-ccs (Kafka 3.7) |
| `provectuslabs/kafka-ui` | `latest` | imagen del 2024-04-10 (sin versión fijada) |
| `rabbitmq` | `3-management` | 3.13.7 |
| `quay.io/keycloak/keycloak` | `26.0` | 26.0.8 |

⚠ **Diferencia o riesgo:** `kafka-ui:latest` y `rabbitmq:3-management` son tags flotantes. Un `docker pull` futuro puede traer otra versión.

---

## 3. Infraestructura

Todos los compose usan la red externa **`cryptosim-net`** (`networks: cryptosim-net: external: true`), que se crea una sola vez con `docker network create cryptosim-net`.

| Compose | Contenedor | Puertos host | Notas |
|---|---|---|---|
| `infra/kafka/docker-compose.yml` | `cryptosim-zookeeper` | 2181 | healthcheck `echo srvr \| nc localhost 2181 \| grep -q Mode`; heap 128–256 MB |
| | `kafka1`, `kafka2`, `kafka3` | 9092, 9093, 9094 | `KAFKA_BROKER_ID` 1–3; `depends_on` ZK healthy; healthcheck `kafka-broker-api-versions`; heap 256–512 MB |
| `infra/kafka-ui/docker-compose.yml` | `cryptosim-kafka-ui` | 8090 → 8080 | clúster `local` en `kafka1:29092,kafka2:29092,kafka3:29092`; `DYNAMIC_CONFIG_ENABLED=true` |
| `infra/rabbitmq/docker-compose.yml` | `rabbitmq` | 5672, 15672 | healthcheck `rabbitmq-diagnostics -q ping`; usuario por defecto `guest` |
| `infra/keycloak/docker-compose.yml` | `keycloak` | 8080 | `start-dev --import-realm`; monta `cloudnative-realm.json` (`:ro,Z`) |

**Listeners de cada broker** (ejemplo de `kafka1`; los otros cambian solo el puerto externo y el host):

```yaml
KAFKA_LISTENERS: INTERNAL://0.0.0.0:29092,EXTERNAL://0.0.0.0:9092
KAFKA_ADVERTISED_LISTENERS: INTERNAL://kafka1:29092,EXTERNAL://localhost:9092
KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: INTERNAL:PLAINTEXT,EXTERNAL:PLAINTEXT
KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL
```
- **INTERNAL** (`kafkaN:29092`) se usa entre brokers y para Kafka-UI dentro de la red Docker.
- **EXTERNAL** (`localhost:909x`) se usa para los servicios Spring que corren en el host.

Valores por defecto del clúster:
- `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=3`, `KAFKA_DEFAULT_REPLICATION_FACTOR=3`, `KAFKA_MIN_INSYNC_REPLICAS=2`.
- `KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=3` y `…_MIN_ISR=2`.
- `KAFKA_AUTO_CREATE_TOPICS_ENABLE=false`: los tópicos solo los crean los servicios.

**Realm de Keycloak** (`infra/keycloak/cloudnative-realm.json`):
- Realm `cloudnative`; `accessTokenLifespan` de 300 s.
- Cliente público `angular-app`: flujo estándar con PKCE S256, redirect `http://localhost:4200/*`, web origin `http://localhost:4200`. Tiene `directAccessGrantsEnabled=true`, que se usa para obtener tokens con `curl` en la demo.
- Usuario `demo` con email, nombre y apellido, e **id fijo** para que su `sub` no cambie al recrear el contenedor.

**Sin volúmenes:** ni Kafka ni Keycloak persisten datos. Un `down` borra tópicos y mensajes, y el realm se reimporta al arrancar.

---

## 4. Kafka

### 4.1 Tópicos

| Tópico | Part. | RF | retention.ms | cleanup.policy | min.insync | Clave | Declara / produce | Consume |
|---|---|---|---|---|---|---|---|---|
| `precios.actualizados` | 3 | 3 | 86400000 (24 h) | delete | 2 | `simbolo` | ms-precios (`…/ms_precios/config/KafkaTopicConfig.java`, `producer/PrecioPublisher.java`) | ms-alertas, `alertas-group` (`consumer/PrecioConsumer.java`); también `VentanaReplay` sin grupo |
| `alertas.disparadas` | 2 | 3 | 604800000 (7 d) | delete | 2 | `usuarioId` | ms-alertas (`…/ms_alertas/config/KafkaTopicConfig.java`, `producer/AlertaDisparadaPublisher.java`) | ms-notificaciones, `notificaciones-group` |
| `precios.actualizados.DLT` | 1 | 3 | 604800000 | delete | 2 | la del original | declarado en `…/ms_alertas/config/KafkaErrorConfig.java`; produce el `DeadLetterPublishingRecoverer` | ms-alertas, `alertas-dlt-group` (`PrecioDltConsumer`) |
| `alertas.disparadas.DLT` | 1 | 3 | 604800000 | delete | 2 | la del original | declarado en `…/ms_notificaciones/config/KafkaErrorConfig.java`; produce el recoverer | ms-notificaciones, `notificaciones-dlt-group` (`AlertaDltConsumer`) |

Los valores salen de `app.kafka.topics.*` en el `application.yml` de cada servicio dueño, y se aplican con `TopicBuilder`. `spring.kafka.admin.modify-topic-configs: true` alinea las configs si el tópico ya existe. Estado verificado con `kafka-topics --describe` el 22-nov.

### 4.2 Productor (ms-precios; ms-alertas usa los mismos valores con `linger.ms: 5`)

`ms-precios/src/main/resources/application.yml`:
```yaml
spring.kafka:
  bootstrap-servers: localhost:9092,localhost:9093,localhost:9094
  producer:
    acks: all
    key-serializer: org.apache.kafka.common.serialization.StringSerializer
    value-serializer: org.springframework.kafka.support.serializer.JacksonJsonSerializer
    compression-type: lz4                     # solo ms-precios
    properties:
      enable.idempotence: true
      max.in.flight.requests.per.connection: 5
      linger.ms: 10
      request.timeout.ms: 10000
      delivery.timeout.ms: 30000
      max.block.ms: 5000
      spring.json.add.type.headers: false     # el consumidor usa su propio record
```
- **Callback:** `PrecioPublisher` y `AlertaDisparadaPublisher` registran el resultado del envío en `whenComplete`: éxito con tópico, partición, offset y clave, o error con su causa.
- **Productor de ms-notificaciones** (`ms-notificaciones/src/main/resources/application.yml`): solo lo usa el recoverer del DLT. Tiene `acks: all`, idempotencia y `max.block.ms: 5000`.

### 4.3 Consumidor (ms-alertas; ms-notificaciones es igual salvo lo indicado)

`ms-alertas/src/main/resources/application.yml`:
```yaml
spring.kafka:
  consumer:
    auto-offset-reset: latest               # ms-notificaciones: earliest
    enable-auto-commit: false
    key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    value-deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
    properties:
      spring.deserializer.value.delegate.class: org.springframework.kafka.support.serializer.JacksonJsonDeserializer
      spring.json.trusted.packages: com.proyecto.ms_alertas.dto      # ms-notificaciones: com.proyecto.ms_notificaciones.dto
      spring.json.use.type.headers: false
      spring.json.value.default.type: com.proyecto.ms_alertas.dto.PrecioEvent   # ms-notificaciones: ...AlertaDisparadaEvent
  listener:
    ack-mode: record
```
- **Listeners de los DLT** (`PrecioDltConsumer`, `AlertaDltConsumer`): sobrescriben `value.deserializer=StringDeserializer` y `auto.offset.reset=earliest` en el propio `@KafkaListener`, porque el contenido puede ser JSON inválido.
- **Nombres de tópicos y grupos:** salen de `app.kafka.*`, leído con `@ConfigurationProperties`: `…/ms_alertas/config/AppProperties.java`, `…/ms_precios/config/AppProperties.java` y `…/ms_notificaciones/config/KafkaNames.java`. Ninguna clase los escribe en el código.

---

## 5. DTOs y eventos

Todos son `record`; los precios y montos son `BigDecimal` y las fechas `Instant`.

**`PrecioEvent`** (`…/ms_precios/dto/PrecioEvent.java`; copia en `…/ms_alertas/dto/PrecioEvent.java`), en `precios.actualizados`:

| Campo | Tipo | Nota |
|---|---|---|
| `simbolo` | String | clave del mensaje |
| `precio`, `variacion24hPct`, `maximo24h`, `minimo24h`, `volumen24h` | BigDecimal | desde Binance o el simulador |
| `timestamp` | Instant | momento del ciclo |
| `fuente` | String | `BINANCE` o `SIMULADO` |

**`AlertaDisparadaEvent`** (`…/ms_alertas/dto/` y `…/ms_notificaciones/dto/`), en `alertas.disparadas`:

| Campo | Tipo | Nota |
|---|---|---|
| `alertaId` | Long | |
| `usuarioId` | String | `sub` del JWT; clave del mensaje |
| `simbolo`, `tipo` | String | `tipo` = nombre del enum `TipoAlerta` |
| `valorObjetivo` | BigDecimal | precio objetivo; en `VARIACION_VENTANA` es el **porcentaje** (⚠ ampliación) |
| `precioDisparo` | BigDecimal | |
| `timestamp` | Instant | |
| `mensaje` | String | por ejemplo "BTC ≥ 60000 · precio 60010" o "ETH se mueve ± 0.05 % en 2 min · subió 0.28 % (de A a B)" |

**`NotificacionMensaje`** (RabbitMQ; `…/ms_alertas/dto/NotificacionMensaje.java` y `…/ms_notificaciones/dto/NotificacionMensaje.java`): `Long referenciaId` (id de la alerta), `String usuario` (`sub`), `String mensaje` ("Alerta creada: …"). Se serializa a JSON con `JacksonJsonMessageConverter`.

---

## 6. Consumer groups

| Grupo | Servicio | Tópico | auto-offset-reset | Concurrencia |
|---|---|---|---|---|
| `alertas-group` | ms-alertas | `precios.actualizados` (3 part.) | latest | 1 por instancia (no se configura `concurrency`) |
| `alertas-dlt-group` | ms-alertas | `precios.actualizados.DLT` | earliest | 1 |
| `notificaciones-group` | ms-notificaciones | `alertas.disparadas` (2 part.) | earliest | 1 |
| `notificaciones-dlt-group` | ms-notificaciones | `alertas.disparadas.DLT` | earliest | 1 |

**Con 2 instancias de ms-alertas** (la segunda con `--server.port=8086`):
- Se usa la estrategia de asignación por defecto de kafka-clients 4.2 (`RangeAssignor` y `CooperativeStickyAssignor`, visible en el log del consumidor).
- Las 3 particiones de `precios.actualizados` se reparten **2 + 1** (comprobado con `GET /api/consumer-groups/alertas-group` y en Kafka-UI). Si una instancia cae, la otra toma las 3.
- Como la clave es el símbolo, cada cripto cae siempre en la misma partición, así que la evalúa una sola instancia y en orden.
- `alertas-dlt-group` también se reparte, pero su tópico tiene 1 partición, así que solo una instancia lo consume.

---

## 7. Manejo de errores (Kafka)

`…/ms_alertas/config/KafkaErrorConfig.java` y `…/ms_notificaciones/config/KafkaErrorConfig.java` comparten el mismo esquema. Boot inyecta el `DefaultErrorHandler` en la factory de listeners por defecto.

```java
ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(r.maxReintentos()); // 3
backOff.setInitialInterval(r.intervaloInicialMs());  // 1000
backOff.setMultiplier(r.multiplicador());            // 2.0
backOff.setMaxInterval(r.intervaloMaximoMs());       // 4000  → esperas 1 s, 2 s, 4 s
DefaultErrorHandler handler = new DefaultErrorHandler(recovererConLog, backOff);
handler.addNotRetryableExceptions(DeserializationException.class, PayloadInvalidoException.class);
```
```java
// Destino: <tópico>.DLT, partición -1 (decide el productor: el DLT tiene 1 partición).
DeadLetterPublishingRecoverer dlt = new DeadLetterPublishingRecoverer(templates,
        (registro, ex) -> new TopicPartition(dltPara(dltPorTopico, registro), -1));
// Se publica la causa real, no el ListenerExecutionFailedException que la envuelve.
dlt.accept(registro, ex instanceof ListenerExecutionFailedException lefe && lefe.getCause() instanceof Exception c ? c : ex);
```
- `templates` usa `byte[]` → `ByteArraySerializer` cuando falló la deserialización, y así reenvía los bytes originales. Para el resto usa el `KafkaTemplate` JSON.
- Los reintentos van por `app.kafka.reintentos.*` en `application.yml`.

| Caso | ¿Reintenta? | Destino |
|---|---|---|
| JSON ilegible (`DeserializationException`, vía `ErrorHandlingDeserializer`) | No | DLT de su tópico, con los bytes originales |
| `PayloadInvalidoException` (falla la validación) | No | DLT |
| Cualquier otra excepción: `FalloTransitorioException` del gancho de demo, BD caída (`DataAccessException`), etc. | Sí: 3 reintentos (1 s, 2 s, 4 s) | DLT al agotarse |

**Qué se registra en el log:**
- Cada reintento: `WARN Fallo procesando registro (intento n) topico= particion= offset= clave= causa=`.
- Al enviar al DLT: `ERROR Enviando a DLT destino= topico= particion= offset= clave= causa=`.
- El consumidor del DLT: `ERROR Mensaje en DLT topico= particion= offset= clave= | origen=topico-part@offset | excepcion=<fqcn> | mensaje= | valor=`, leyendo los headers `kafka_dlt-*`.

**Validación del payload:**
- `…/ms_alertas/consumer/PrecioEventValidator.java`: evento no nulo, `simbolo` no vacío y dentro del catálogo, `precio > 0` y `timestamp` presente.
- `…/ms_notificaciones/consumer/AlertaDisparadaValidator.java`: `alertaId`, `usuarioId`, `simbolo` y `timestamp` presentes, y `precioDisparo > 0`. ⚠ La especificación solo exige validar `PrecioEvent`; esta validación es una ampliación.

**Gancho de demo** (`…/ms_alertas/service/FalloTransitorioSimulado.java`): con `app.demo.fallo-transitorio.simbolo=BTC` y `veces=N`, los N primeros precios de BTC lanzan `FalloTransitorioException`.
- Con `veces=2` se recupera al tercer intento.
- Con `veces=4` agota los 3 reintentos y el registro va al DLT.
- Está desactivado por defecto.

Los productores registran sus errores en el callback de `send`. Además, `max.block.ms: 5000` evita que el scheduler se bloquee si Kafka no responde.

---

## 8. Microservicios

Seguridad común (`…/config/SecurityConfig.java` de ms-precios, ms-alertas y ms-admin-kafka):
- Resource server OAuth2 con `issuer-uri: http://localhost:8080/realms/cloudnative`. Spring obtiene las claves públicas (JWKS) del issuer y valida la firma, el `iss` y la expiración.
- `anyRequest().authenticated()`. Quedan abiertos `OPTIONS /**` (preflight) y `/actuator/health`.
- CSRF desactivado (API sin cookies). CORS solo para `http://localhost:4200`, con métodos GET/POST/PUT/DELETE/OPTIONS y headers `Authorization` y `Content-Type`.
- No se validan audiencia ni roles: cualquier usuario autenticado del realm puede usar todos los endpoints, incluidos los de administración.

### ms-precios (8084)
**Responsabilidad:** cada 5 s (`@Scheduled(fixedRateString = "${app.precios.intervalo-ms}")`) obtiene los precios, los guarda en el historial y publica un `PrecioEvent` por cripto. No usa base de datos.

**Modos de obtención** (`ObtencionPreciosService`):
- `binance`: si Binance falla, omite el ciclo.
- `simulado`: solo usa el simulador.
- `binance-con-respaldo` (el modo por defecto): si Binance falla, pasa al simulador, que sigue desde el último precio real (`SimuladorPrecios.sincronizar`). Vuelve a Binance cuando responde.

**Otros componentes:**
- `BinanceClient`: un solo request a `/api/v3/ticker/24hr?symbols=[...]`, con timeouts de conexión de 2 s y de lectura de 3 s.
- `CatalogoService`: al arrancar valida cada par con `/api/v3/exchangeInfo` y omite los que no existen.
- Historial: buffer circular de `app.historial.max-puntos` = 1440 puntos por cripto.

| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/api/catalogo` | `[{simbolo, nombre}]` de las criptos activas |
| GET | `/api/precios` | último `PrecioEvent` de cada cripto |
| GET | `/api/precios/{simbolo}/historial?minutos=60` (1–1440) | `[{timestamp, precio}]`; 404 si el símbolo no existe |

### ms-alertas (8081)
**Responsabilidad:** CRUD de alertas y de la lista de seguimiento en Neon; consumo de precios y evaluación; publicación de `alertas.disparadas` (Kafka) y de la confirmación de alerta creada (RabbitMQ). El usuario sale siempre de `jwt.getSubject()` en los controladores.

| Método | Ruta | Qué hace |
|---|---|---|
| POST | `/api/alertas` | crea una alerta (201). Valida el símbolo en el catálogo y los campos según el tipo (400). Envía un `NotificacionMensaje` por RabbitMQ |
| GET | `/api/alertas?estado=` | alertas del usuario, de la más nueva a la más vieja, con filtro opcional |
| DELETE | `/api/alertas/{id}` | pasa a CANCELADA solo si es del usuario y está ACTIVA (204; 404 si no es suya; 409 si no está ACTIVA) |
| GET | `/api/seguimiento` | `{simbolos:[...]}` |
| PUT | `/api/seguimiento` | reemplaza la lista. Valida todo antes de borrar, en una transacción |

**Entidades y tablas** (Hibernate con `ddl-auto: update` sobre PostgreSQL en Neon):

| Entidad | Tabla | Columnas |
|---|---|---|
| `Alerta` (`entity/Alerta.java`) | `alertas` | `id` (identity), `usuario_id`, `simbolo`, `tipo`, `valor` (numeric 24,8, **nullable**), `estado`, `creada_en`, `disparada_en`, `precio_disparo`, `direccion`, `porcentaje` (numeric 10,4), `ventana_minutos`. Índices `(simbolo, estado)` y `(usuario_id)` |
| `Seguimiento` (`entity/Seguimiento.java`) | `seguimientos` | `id`, `usuario_id`, `simbolo`; único `(usuario_id, simbolo)` |

**Script de esquema** `ms-alertas/src/main/resources/db/ajustes-esquema.sql`:
- Corre en cada arranque, después de Hibernate (`spring.sql.init.mode: always`, `spring.jpa.defer-datasource-initialization: true`, separador `@@`).
- Es idempotente: rehace el `CHECK` de `tipo` para que incluya `VARIACION_VENTANA` y quita el `NOT NULL` de `valor`.

### ms-notificaciones (8082)
**Responsabilidad:** no expone endpoints REST propios y no valida JWT (no tiene `spring-boot-starter-security`).
- **Kafka:** `AlertaDisparadaConsumer` → `AlertaDisparadaValidator` → `NotificacionAlertaService`, que "envía" la notificación con un log estructurado `NOTIFICACION ALERTA usuario= alertaId= …`. `AlertaDltConsumer` registra lo que llega al DLT.
- **RabbitMQ:** ver la sección 10.

### ms-admin-kafka (8085)
**Responsabilidad:** administra el clúster con un único `AdminClient` construido desde la configuración de `KafkaAdmin` de Boot (`config/AdminClientConfig.java`). Cada operación espera como máximo `app.admin.timeout-segundos` = 15 s.

| Método | Ruta | Qué hace (API de `AdminClient`) |
|---|---|---|
| GET | `/api/topics` | `listTopics` + `describeTopics` + `describeConfigs` (solo las configs definidas en el tópico) |
| GET | `/api/topics/{name}` | detalle con líder, réplicas e ISR por partición |
| POST | `/api/topics` | `createTopics` con `{name, particiones, replicas, configs}` → 201 |
| PUT | `/api/topics/{name}/config` | `incrementalAlterConfigs` (SET; un valor `null` en el JSON hace DELETE y vuelve al valor por defecto) |
| PUT | `/api/topics/{name}/partitions` | `createPartitions(increaseTo)`; espera hasta 2 s a que la metadata refleje el cambio |
| DELETE | `/api/topics/{name}` | `deleteTopics`; rechaza tópicos internos (`__*`) |
| GET | `/api/consumer-groups` | `listGroups(ListGroupsOptions.forConsumerGroups())` con su estado |
| GET | `/api/consumer-groups/{id}` | miembros y asignación, offsets confirmados, fin de log (`listOffsets(OffsetSpec.latest())`) y lag por partición y total |

**Errores** (`exception/GlobalExceptionHandler.java`):

| Código | Cuándo |
|---|---|
| 404 | tópico o grupo inexistente |
| 409 | el tópico ya existe |
| 400 | validación, particiones o RF inválidos, o operación no permitida |
| 503 | timeout o Kafka no disponible |
| 502 | otro error de Kafka |

### ms-admin-rabbit (8083, sin cambios respecto a la entrega anterior)
**Responsabilidad:** usa `AmqpAdmin`/`RabbitAdmin` (`service/RabbitAdminService.java`).
- Endpoints: `POST /api/queues` (con `durable`, `ttlMs` y dead-letter opcionales), `DELETE /api/queues/{name}`, `POST /api/exchanges` (DIRECT, TOPIC, FANOUT o HEADERS), `DELETE /api/exchanges/{name}`, `POST /api/bindings` y `DELETE /api/bindings` (con el cuerpo `{exchange, queue, routingKey}`).
- Errores: 404 si el recurso no existe y 409 ante un `AmqpException`.
- Seguridad: JWT con el mismo issuer, **sin configuración CORS** (no lo usa el frontend).
- Probado el 22-nov: 201/204 y 401 sin token.

---

## 9. Alertas

**Tipos** (`entity/TipoAlerta.java`): `PRECIO_MAYOR_IGUAL`, `PRECIO_MENOR_IGUAL` y `VARIACION_VENTANA`.
**Estados:** `ACTIVA`, `DISPARADA` y `CANCELADA`. Una alerta se dispara una sola vez.

**Reglas exactas** (patrón estrategia: `regla/ReglaAlerta.java`, una clase por tipo, elegidas con un `EnumMap` en `AlertaEvaluadorService`):

| Tipo | Se cumple si | Clase |
|---|---|---|
| `PRECIO_MAYOR_IGUAL` | `precio.compareTo(valor) >= 0` | `PrecioMayorIgualRegla` |
| `PRECIO_MENOR_IGUAL` | `precio.compareTo(valor) <= 0` | `PrecioMenorIgualRegla` |
| `VARIACION_VENTANA` | `v = (actual − ref) / ref × 100` (8 decimales, HALF_UP). Con `SUBE`: `v ≥ +p`; con `BAJA`: `v ≤ −p`; con `AMBAS`: `|v| ≥ p`. Sin referencia no se cumple | `VariacionVentanaRegla` |

- `compareTo` hace que 100 y 100.00000000 cuenten como el mismo precio.
- Si la condición ya se cumple al crear la alerta, se dispara con el siguiente precio.

**Disparo idempotente** (`AlertaRepository.marcarDisparada`):
```sql
update Alerta a set a.estado = :disparada, a.disparadaEn = :ahora, a.precioDisparo = :precio
where a.id = :id and a.estado = :activa
```
- Si afecta 0 filas, el disparo ya lo hizo otra instancia o un reintento, y **no se publica** el evento.
- El evento se publica después de confirmar el UPDATE.
- La cancelación usa el mismo patrón (`marcarCancelada` con `usuarioId` y `estado = ACTIVA`).

**Ventana de precios** (`VARIACION_VENTANA`):
- `service/VentanaPrecios.java` guarda, por símbolo, una cola en memoria de `(timestamp, precio)`. Usa el timestamp del evento, no el reloj local.
- En cada precio, `AlertaEvaluadorService` lo **registra antes de evaluar** y descarta los puntos anteriores a `ahora − ventana más larga activa de ese símbolo`. Si el símbolo no tiene alertas de variación, conserva solo el último punto.
- La referencia es el primer punto con `timestamp ≥ ahora − ventanaMinutos`.
- `ventanaMinutos` tiene un máximo de `app.ventana.max-minutos` = 1440, igual a la retención del tópico.

**Al reiniciar** (`consumer/VentanaReplay.java`):
1. El listener de precios tiene `autoStartup="false"` e `id = "precios-listener"`, con `idIsGroup=false`.
2. En `ApplicationReadyEvent`, si hay alertas `VARIACION_VENTANA` activas, crea un consumidor **sin grupo**. Ese consumidor hace `assign` de todas las particiones, `offsetsForTimes(ahora − ventana más larga)`, `seek`, y lee hasta los `endOffsets`, con un límite de 30 s.
3. Solo alimenta la ventana (valida cada evento y no evalúa alertas) y no confirma offsets de `alertas-group`.
4. Al final, en un `finally`, arranca el listener normal.

Se probó con 390 precios releídos. ⚠ Ni el `id` ni el `autoStartup` del listener aparecen en la especificación: son la forma de garantizar que la relectura termine antes de evaluar.

---

## 10. RabbitMQ

Topología declarada en `…/ms_notificaciones/config/RabbitConfig.java` y `RabbitConfigAmpliacion.java`. Los nombres salen de `app.rabbit.*` vía `RabbitNames`.

| Exchange | Tipo | Routing key / patrón | Cola | Dead letter de la cola |
|---|---|---|---|---|
| `tareas.exchange` | direct | `notificacion.enviar` | `notificaciones.queue` | `tareas.dlx` / `notificacion.fallida` |
| `tareas.dlx` | direct | `notificacion.fallida` | `notificaciones.dlq` | — |
| `tareas.exchange` | direct | `ticket.generar` | `tickets.queue` | `tareas.dlx` / `ticket.fallido` |
| `tareas.dlx` | direct | `ticket.fallido` | `tickets.dlq` | — |
| `eventos.exchange` | topic | `documento.*` | `documentos.queue` | `tareas.dlx` / `documento.fallido` |
| `tareas.dlx` | direct | `documento.fallido` | `documentos.dlq` | — |

Todas las colas son durables. Los casos de **tickets y documentos vienen de la entrega anterior** y no participan en CryptoSim (no se tocaron).

**Conexión con ms-alertas:**
- `…/ms_alertas/config/RabbitConfig.java` declara `tareas.exchange` y el convertidor JSON.
- `producer/NotificacionProducer.java` publica `NotificacionMensaje` con la routing key `notificacion.enviar` al crear cada alerta (`AlertaService.crear`).

**Consumo en ms-notificaciones** (`consumer/NotificacionConsumer.java`, ACK manual; `acknowledge-mode: manual` y `default-requeue-rejected: false`):

| Caso | Qué hace |
|---|---|
| Éxito | `basicAck` |
| Mensaje inválido (sin `referenciaId` o `usuario`) | `basicNack` sin reencolar → DLQ |
| `FalloTemporalException` (el mensaje contiene `FALLO_TEMPORAL`, gancho de prueba) | se republica con el header `x-intentos+1`; al tercer intento, `basicNack` → DLQ |
| Otro error | `basicNack` → DLQ |

`NotificacionDlqConsumer` consume `notificaciones.dlq` y registra `[DLQ] Mensaje no entregado: … | cabeceras=` (incluye `x-death`). Por eso la DLQ se ve vacía en la consola: el mensaje se consume en cuanto llega.

⚠ **Diferencia:** RabbitMQ es de **un solo nodo**; el enunciado menciona "clúster" (ver sección 13).

---

## 11. Frontend (`frontend-cryptosim`)

**Login:**
- `src/app/auth.service.ts` usa `keycloak-js` (`url: http://localhost:8080`, realm `cloudnative`, cliente `angular-app`) con `init({ onLoad: 'login-required', pkceMethod: 'S256' })`.
- `src/app/app.config.ts` lo ejecuta con `provideAppInitializer`, así la app no se muestra sin sesión.
- `getToken()` llama a `updateToken(30)`, que renueva el token si le quedan menos de 30 s.
- El logout vuelve al origen.

**JWT en las llamadas:**
- `src/app/auth.interceptor.ts` (funcional) agrega `Authorization: Bearer <token>` solo si la URL empieza por uno de los orígenes de `ORIGENES_CON_JWT`.
- Esos orígenes se derivan de `API` en `src/app/core/api.config.ts`: `http://localhost:8081`, `:8084` y `:8085`.
- Es el único lugar donde están escritas las URLs.

**Arquitectura:**
- Angular 22 con componentes standalone, `ChangeDetectionStrategy.OnPush`, señales y sin zone.js.
- Rutas diferidas (`src/app/app.routes.ts`), cada pantalla en su propio chunk.
- Los refrescos usan `timer` + `switchMap` + `takeUntilDestroyed`.

| Ruta | Componente | Qué muestra |
|---|---|---|
| `/mercado` | `pages/mercado/mercado.ts` | Tarjetas de las criptos seguidas: precio (parpadea verde o rojo al cambiar), variación 24 h, máximo, mínimo, volumen, fuente (BINANCE/SIMULADO) y sparkline SVG. Debajo, el **comparador base 100** (`shared/comparador.ts`) de la última hora, con tooltip. Refresco cada 5 s; el historial se carga una vez (60 min) y se extiende en memoria |
| `/seguimiento` | `pages/seguimiento/seguimiento.ts` | Las 10 criptos con un interruptor. El cambio se ve al instante y se deshace si el backend falla |
| `/alertas` | `pages/alertas/alertas.ts` | **Resumen** (activas, disparadas y cuántas en 24 h, canceladas). Formulario reactivo por tipo (precio ≥/≤ con botón "usar precio actual", o variación con dirección, % y minutos) y resumen en lenguaje natural. Lista filtrable con botón cancelar; refresco cada 10 s |
| `/notificaciones` | `pages/notificaciones/notificaciones.ts` | Alertas DISPARADA (cripto, condición, precio al disparo, hora); refresco cada 5 s y resaltado de las nuevas |
| `/admin` | `pages/admin/admin.ts` | Tópicos (crear, detalle con líder/réplicas/ISR, cambiar retención, aumentar particiones, eliminar con confirmación) y consumer groups (miembros, offsets, lag; refresco cada 5 s) |

**Servicios** (`src/app/core/`):
- `PreciosService` (8084), `AlertasService` y `SeguimientoService` (8081), `AdminKafkaService` (8085).
- Modelos en `models.ts`.
- `errores.ts` (`mensajeError`) arma el aviso en pantalla cuando un servicio no responde, en lugar de dejar la pantalla en blanco.

**Utilidades compartidas** (`src/app/shared/`):
- `formato.ts` y `formato.pipes.ts`: números con `Intl.NumberFormat('es-CL')` y textos de condición.
- `sparkline.ts` y `comparador.ts`: gráficas en SVG propio, sin librerías.
- El comparador usa 8 colores fijos por posición en el catálogo (`--serie-1…8` en `src/styles.css`), validados para daltonismo sobre el fondo oscuro. La 9.ª y 10.ª cripto usan línea discontinua.

---

## 12. Cómo ejecutar

### 12.1 Con scripts (desde `backend-cryptosim`; el frontend debe estar en `../frontend-cryptosim`)
Requisito previo: crear `ms-alertas/src/main/resources/application-local.yml` con `spring.datasource.password` (no está en Git).
```bash
./levantar.sh                  # red, 4 compose, espera 3 brokers/Keycloak/RabbitMQ, compila y arranca los 5 servicios y ng serve
./levantar.sh --simulado       # precios simulados (alertas se disparan rápido)
./levantar.sh --sin-compilar   # usa los JAR existentes (~50 s en vez de ~10 min)
./detener.sh                   # detiene frontend y servicios y hace docker compose down de toda la infra
./detener.sh --dejar-infra
```
Los logs quedan en `logs/<servicio>.log` y los PID en `.run/` (ambos ignorados por Git).

### 12.2 A mano, en orden
```bash
docker network create cryptosim-net                         # una sola vez
docker compose -f infra/kafka/docker-compose.yml up -d
docker compose -f infra/kafka-ui/docker-compose.yml up -d
docker compose -f infra/rabbitmq/docker-compose.yml up -d
docker compose -f infra/keycloak/docker-compose.yml up -d
cd ms-precios        && ./mvnw spring-boot:run                                   # 1.º: crea precios.actualizados
cd ms-alertas        && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local  # crea alertas.disparadas y precios.actualizados.DLT
cd ms-notificaciones && ./mvnw spring-boot:run
cd ms-admin-kafka    && ./mvnw spring-boot:run
cd ms-admin-rabbit   && ./mvnw spring-boot:run
cd ../frontend-cryptosim && npm ci && npx ng serve          # http://localhost:4200, demo / demo123
```

### 12.3 Guion de demo (~8 min; detalle y comandos en `README.md`, sección 5)
1. **Kafka-UI** (http://localhost:8090): 3 brokers, 4 tópicos con particiones, RF y retención.
2. **Login** en el frontend, seguir 3 criptos en *Seguimiento* y ver los precios moverse en *Mercado*.
3. **Alerta que se cumple pronto:** el botón "usar" pone el precio actual, que luego se ajusta. Mostrar el disparo en *Notificaciones* y el evento en `alertas.disparadas`.
4. **Mensaje inválido:** publicarlo con `kafka-console-producer` y mostrar `precios.actualizados.DLT` y el log de `PrecioDltConsumer`.
5. **Segunda instancia** de ms-alertas en el puerto 8086: reparto 2 + 1 en `alertas-group` (Kafka-UI o panel *Admin Kafka*).
6. **ms-admin-kafka:** cambiar `retention.ms` y consultar el lag, con `curl` o desde el panel.
7. **RabbitMQ:** la confirmación "Alerta creada" en el log de ms-notificaciones, y un `FALLO_TEMPORAL` publicado a mano que hace 3 intentos y pasa a la DLQ.

Opcional: alerta de variación "sube o baja 0,05 % en 2 min" y el reinicio de ms-alertas mostrando `Ventana reconstruida … precios=N`.

---

## 13. Limitaciones, decisiones de diseño y diferencias con la especificación

### Decisiones de diseño (para el informe)
- **Clave = `simbolo`** en `precios.actualizados`: cada cripto va siempre a la misma partición, así se conserva su orden y la evalúa una sola instancia del grupo.
- **Clave = `usuarioId`** en `alertas.disparadas`: las notificaciones de un usuario llegan en orden.
- **Retención de 24 h** en `precios.actualizados`: permite reconstruir la ventana de precios al reiniciar (`offsetsForTimes`). Por eso `ventanaMinutos ≤ 1440`.
- **RF 3 con `min.insync.replicas` 2 y `acks=all` + idempotencia:** se tolera la caída de un broker sin perder escrituras confirmadas ni duplicar por reintentos del productor.
- **DLT con 1 partición y destino de partición −1:** evita el fallo del recoverer, que por defecto usa la partición del original.
- **No reintentar lo irrecuperable** (JSON ilegible, payload inválido) y sí lo transitorio, con backoff exponencial 1-2-4 s.
- **Disparo idempotente** con un UPDATE condicional en lugar de `@Version`: un reintento o una segunda instancia no duplican el evento.
- **`@KafkaListener` solo recibe, valida y delega:** los servicios de dominio (`AlertaEvaluadorService`, `NotificacionAlertaService`) no conocen Kafka.
- **`auto-offset-reset`:** `latest` en `alertas-group` (no evaluar precios viejos) y `earliest` en `notificaciones-group` (no perder alertas disparadas).

### Diferencias con la especificación
| Tema | Especificación | Implementación |
|---|---|---|
| Criterio de la etapa 4 | "termina en el DLT **tras los reintentos**" | Se siguió el §6 (el payload inválido no se reintenta). Para mostrar reintentos se agregó el gancho `app.demo.fallo-transitorio` |
| Listener de precios | `@KafkaListener(topics, groupId)` | Además `id="precios-listener"`, `idIsGroup=false` y `autoStartup="false"`; lo arranca `VentanaReplay` |
| Contrato de alertas | `AlertaRequest(simbolo, tipo, valor)` | Agrega `direccion`, `porcentaje` y `ventanaMinutos`; `valor` pasa a ser opcional según el tipo. `AlertaResponse` también se amplía |
| `AlertaDisparadaEvent.valorObjetivo` | precio objetivo | En `VARIACION_VENTANA` contiene el porcentaje |
| Esquema de BD | solo `ddl-auto` | Se agrega `db/ajustes-esquema.sql`, porque `update` no cambia el `CHECK` ni el `NOT NULL` existentes |
| Validación en ms-notificaciones | no se exige | `AlertaDisparadaValidator` (no reintentable → DLT) |
| Ubicación de los extras | no se especifica | Comparador en *Mercado* y resumen en *Mis alertas*; panel en la ruta `/admin` |
| RabbitMQ "clúster" | se deja nodo único y se pregunta al docente | Nodo único. **Pendiente de confirmar con el docente** |
| Tickets y documentos (RabbitMQ) | no se mencionan | Siguen en ms-notificaciones desde la entrega anterior, sin cambios |

### Limitaciones conocidas
- **Evento perdido** si la publicación a Kafka falla después del UPDATE: la alerta queda DISPARADA sin evento. Se resolvería con el patrón *outbox*.
- **Ventana en memoria con 2 instancias:** tras un rebalanceo, la ventana de un símbolo recién asignado puede tener huecos; la referencia es el primer precio disponible dentro de la ventana.
- **Historial de ms-precios en memoria:** se pierde al reiniciar. Con `max-puntos` = 1440 y un ciclo de 5 s cubre **unas 2 h**, aunque el endpoint acepte `minutos` hasta 1440.
- **Sin persistencia en Docker:** `down` borra los datos de Kafka; Keycloak se reimporta. Las alertas no se pierden porque viven en Neon.
- **Seguridad mínima:** no hay roles; cualquier usuario autenticado puede usar ms-admin-kafka y ms-admin-rabbit. `directAccessGrantsEnabled=true` en `angular-app`, solo para obtener tokens en la demo. Las credenciales `guest` de RabbitMQ y `admin` de Keycloak son las de desarrollo.
- **Imágenes con tag flotante:** `kafka-ui:latest` y `rabbitmq:3-management`.
- **Pruebas mínimas:** `contextLoads` y los tests de reglas que pide el §10. No hay tests de integración con Kafka embebido ni tests del frontend. El `contextLoads` de ms-alertas requiere el perfil `local` (conecta a Neon).
- **Detalles menores en el código** (no afectan el funcionamiento):
  - El comentario de `…/ms_alertas/config/RabbitConfig.java` todavía dice "ms-pedidos".
  - Los consumidores de los DLT usan el mismo error handler: si fallaran, `dltPara` lanzaría `IllegalStateException` porque no hay DLT del DLT. Solo registran en el log, así que en la práctica no ocurre.
