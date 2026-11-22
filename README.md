# CryptoSim — backend

Monitor de criptomonedas con alertas (EP5 DSY1107). Solo lee precios y avisa; no opera ni usa dinero real.
Microservicios Spring Boot 4.1 (Java 21) con **Kafka** (Zookeeper + 3 brokers), **RabbitMQ**, JWT de **Keycloak** y PostgreSQL en **Neon**.
El frontend Angular está en el repo `frontend-cryptosim`.

```
ms-precios ──precios.actualizados──▶ ms-alertas ──alertas.disparadas──▶ ms-notificaciones
 (Binance/simulado)                  (BD Neon)  ──RabbitMQ tareas.exchange──▶ (log)
                                         ▲
               frontend :4200 ──JWT──────┘ (también ms-precios y ms-admin-kafka)
```

| Servicio | Puerto | Rol |
|---|---|---|
| ms-alertas | 8081 | Alertas y seguimiento (BD). Consume precios, publica alertas disparadas (Kafka) y confirmaciones (RabbitMQ) |
| ms-notificaciones | 8082 | Consume `alertas.disparadas` (Kafka) y `notificaciones.queue` (RabbitMQ), con reintentos, DLT y DLQ |
| ms-admin-rabbit | 8083 | Administración de RabbitMQ (entrega anterior, sin cambios) |
| ms-precios | 8084 | Obtiene precios de Binance cada 5 s (o los simula) y los publica |
| ms-admin-kafka | 8085 | Administración de tópicos, configs, particiones y consumer groups |

Contrato completo (tópicos, DTOs, colas, endpoints): [CONTRATO.md](CONTRATO.md). Especificación: [docs/ESPECIFICACION.md](docs/ESPECIFICACION.md).

## Requisitos
- Java 21 (los servicios traen Maven wrapper, no hace falta instalar Maven).
- Docker con Compose v2. Se recomiendan ~6 GB de RAM libres para la infraestructura y los servicios.
- Node 22+ y npm (para el frontend).
- La contraseña de la base de datos Neon (la entrega el autor; no está en Git).

## 1. Infraestructura
```bash
docker network create cryptosim-net        # una sola vez
docker compose -f infra/kafka/docker-compose.yml up -d
docker compose -f infra/kafka-ui/docker-compose.yml up -d
docker compose -f infra/rabbitmq/docker-compose.yml up -d
docker compose -f infra/keycloak/docker-compose.yml up -d
```

| Componente | URL | Credenciales |
|---|---|---|
| Kafka (3 brokers) | `localhost:9092,9093,9094` | — |
| Kafka-UI | http://localhost:8090 | — |
| RabbitMQ | http://localhost:15672 | guest / guest |
| Keycloak | http://localhost:8080 | admin / admin |

Keycloak importa el realm `cloudnative` (`infra/keycloak/cloudnative-realm.json`) al arrancar: cliente público `angular-app` con PKCE y el usuario de prueba **demo / demo123**.

Comprobar que los 3 brokers están arriba (los tópicos con RF=3 los necesitan):
```bash
docker exec kafka1 kafka-broker-api-versions --bootstrap-server kafka1:29092 | grep -c 'id:'   # → 3
```

## 2. Secreto de la base de datos
Crear `ms-alertas/src/main/resources/application-local.yml` (está en `.gitignore`, **no se sube**):
```yaml
spring:
  datasource:
    password: TU_PASSWORD_DE_NEON
```
La URL y el usuario de Neon ya están en `application.yml`. Las tablas se crean solas (`ddl-auto: update`).

## 3. Arrancar los servicios
Cada uno en su terminal, desde su carpeta. Orden recomendado (ms-precios crea `precios.actualizados`; ms-alertas crea `alertas.disparadas` y `precios.actualizados.DLT`):
```bash
cd ms-precios        && ./mvnw spring-boot:run
cd ms-alertas        && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
cd ms-notificaciones && ./mvnw spring-boot:run
cd ms-admin-kafka    && ./mvnw spring-boot:run
cd ms-admin-rabbit   && ./mvnw spring-boot:run     # opcional, para el punto 7 de la demo
```
Frontend (repo `frontend-cryptosim`): `npm ci && npx ng serve` → http://localhost:4200.

Modo de precios (`app.precios.modo` en `ms-precios/src/main/resources/application.yml`):
- `binance-con-respaldo` (por defecto): Binance; si no responde, simula desde el último precio real.
- `simulado`: paseo aleatorio, sin red. Útil para que una alerta se dispare rápido:
  ```bash
  ./mvnw spring-boot:run -Dspring-boot.run.arguments="--app.precios.modo=simulado --app.precios.volatilidad-pct=0.5"
  ```

Pruebas: `./mvnw test` en cada servicio (en ms-alertas, `./mvnw test -Dspring.profiles.active=local`, porque `contextLoads` conecta a Neon).

## 4. Token para llamar a la API desde la terminal
```bash
TOKEN=$(curl -s -X POST localhost:8080/realms/cloudnative/protocol/openid-connect/token \
  -d grant_type=password -d client_id=angular-app -d username=demo -d password=demo123 \
  | python3 -c 'import sys,json; print(json.load(sys.stdin)["access_token"])')
curl -s -H "Authorization: Bearer $TOKEN" localhost:8084/api/precios
```
El token dura 5 minutos; si una llamada responde 401, vuelve a pedirlo.

## 5. Guion de demo (~8 min)
Con la infraestructura, los 4 servicios principales y el frontend arriba. Para el paso 3, conviene arrancar ms-precios en modo `simulado` (ver arriba).

**1. Kafka-UI** — http://localhost:8090 → *Brokers*: 3 brokers. *Topics*: `precios.actualizados` (3 particiones, RF 3, 24 h), `alertas.disparadas` (2 particiones, 7 d) y los dos `.DLT`. Mostrar los mensajes que llegan cada 5 s, con clave = símbolo.

**2. Frontend** — http://localhost:4200, login `demo / demo123`. En *Seguimiento* activar BTC, ETH y SOL. En *Mercado* los precios se mueven cada 5 s y parpadean.

**3. Alerta que se dispara** — En *Mis alertas* elegir BTC, tipo "≥", pulsar **usar** (pone el precio actual) y subirlo un poco (o elegir "≤" y bajarlo un poco). Al cruzar, la alerta pasa a DISPARADA y aparece en *Notificaciones*. En Kafka-UI, el evento está en `alertas.disparadas`. En el log de ms-notificaciones aparece la notificación.

*Extra — alerta por variación en una ventana:* en *Mis alertas* elegir "Variación % en una ventana de tiempo", ETH, "Sube o baja", 0,05 % y 2 min. Con el modo simulado se dispara en segundos y el mensaje dice si subió o bajó y desde qué precio. Si se reinicia ms-alertas con una alerta así activa, el log muestra `Ventana reconstruida … precios=N`: relee `precios.actualizados` desde (ahora − ventana) antes de arrancar el listener.

**4. Mensaje inválido → DLT**
```bash
docker exec -i kafka1 kafka-console-producer --bootstrap-server kafka1:29092 \
  --topic precios.actualizados --property parse.key=true --property key.separator='|' <<'EOF'
BTC|{"simbolo":"XXX","precio":-1}
EOF
```
El log de ms-alertas muestra el payload rechazado (no reintentable) y `PrecioDltConsumer` lo registra con tópico, partición, offset y causa. En Kafka-UI aparece en `precios.actualizados.DLT` con los headers `kafka_dlt-exception-*`. Los demás precios se siguen procesando.

*Opcional (reintentos con backoff):* arrancar ms-alertas con `--app.demo.fallo-transitorio.simbolo=BTC --app.demo.fallo-transitorio.veces=2` y ver en el log los reintentos a 1 s y 2 s antes de procesar bien; con `veces=4` se agotan los 3 reintentos y el registro va al DLT.

**5. Segunda instancia y reparto de particiones**
```bash
cd ms-alertas && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.arguments=--server.port=8086
```
En Kafka-UI → *Consumers* → `alertas-group`: 2 miembros, uno con 2 particiones y otro con 1. Al detener una instancia, la otra recupera las 3.

**6. ms-admin-kafka** (con `$TOKEN` del punto 4)
```bash
curl -s -H "Authorization: Bearer $TOKEN" localhost:8085/api/topics
curl -s -X PUT -H "Authorization: Bearer $TOKEN" -H 'content-type: application/json' \
  localhost:8085/api/topics/alertas.disparadas/config -d '{"configs":{"retention.ms":"259200000"}}'
curl -s -H "Authorization: Bearer $TOKEN" localhost:8085/api/consumer-groups/alertas-group   # miembros, offsets y lag
```
(Devolver la retención a 7 días con `"604800000"`.)

**7. RabbitMQ sigue funcionando** — Cada alerta creada en el punto 3 envió una confirmación por `tareas.exchange` → `notificaciones.queue`: aparece en el log de ms-notificaciones ("Alerta creada: …"). Para el caso de DLQ, publicar un mensaje con fallo temporal forzado:
```bash
curl -s -u guest:guest -H 'content-type: application/json' \
  -X POST localhost:15672/api/exchanges/%2F/tareas.exchange/publish \
  -d '{"properties":{"content_type":"application/json"},"routing_key":"notificacion.enviar","payload":"{\"referenciaId\":1,\"usuario\":\"demo\",\"mensaje\":\"FALLO_TEMPORAL\"}","payload_encoding":"string"}'
```
El log muestra "Fallo temporal, reintento 1/3", "2/3", luego "Reintentos agotados (3), va a DLQ" y finalmente `NotificacionDlqConsumer` registra `[DLQ] Mensaje no entregado: …` con las cabeceras `x-death`. (Ese consumidor vacía la DLQ al registrarla, por eso en la consola de RabbitMQ `notificaciones.dlq` queda en 0; se ve el paso del mensaje en su gráfica de *message rates*.)

## 6. Apagar
```bash
docker compose -f infra/keycloak/docker-compose.yml down
docker compose -f infra/rabbitmq/docker-compose.yml down
docker compose -f infra/kafka-ui/docker-compose.yml down
docker compose -f infra/kafka/docker-compose.yml down
```

## Decisiones y limitaciones conocidas
- **Clave = símbolo** en `precios.actualizados`: cada cripto cae siempre en la misma partición y mantiene su orden aunque haya varias instancias de ms-alertas.
- **Retención 24 h** en `precios.actualizados`: permite reconstruir ventanas de tiempo releyendo el tópico (base del extra `VARIACION_VENTANA`).
- **Disparo idempotente**: `UPDATE … WHERE estado='ACTIVA'`; el evento se publica solo si cambió exactamente una fila, así un precio reprocesado no duplica el aviso.
- **Payload inválido no se reintenta** (va directo al DLT, §6 de la especificación); los fallos transitorios sí (3 reintentos, 1-2-4 s).
- **VARIACION_VENTANA**: la ventana de precios vive en memoria (por símbolo, solo lo que cubre la ventana más larga activa). Al arrancar, ms-alertas la reconstruye releyendo `precios.actualizados` con `offsetsForTimes`, con un consumidor sin grupo que no toca los offsets de `alertas-group`; por eso `ventanaMinutos` tiene tope 1440 (retención de 24 h). Con 2 instancias, cada una evalúa los símbolos de sus particiones; tras un rebalanceo, la ventana de un símbolo recién recibido puede tener un hueco y la referencia sería el primer precio disponible dentro de la ventana.
- **Ajustes de esquema**: `ddl-auto: update` no modifica restricciones existentes. `ms-alertas/src/main/resources/db/ajustes-esquema.sql` (idempotente, corre en cada arranque después de Hibernate) amplía el `CHECK` de `tipo` y quita el `NOT NULL` de `valor`.
- Si la publicación a Kafka falla *después* de guardar la alerta como DISPARADA, ese evento se pierde. Se resolvería con un patrón *outbox*; queda fuera del alcance.
