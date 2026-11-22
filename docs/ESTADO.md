# Estado del proyecto — CryptoSim

Actualizado: **domingo 22-nov-2026**. Entrega: jueves 26-nov-2026, 08:30.

**Resumen:** el MVP (etapas 1 a 8) y los 3 extras de la especificación están completos y con commit en ambos repos.
Todo se probó ejecutándolo. Lo pendiente es menor y está en la sección 3.

---

## 1. Etapas

| # | Etapa | Estado | Commit (repo) |
|---|---|---|---|
| 1 | Infra: Kafka (ZK + 3 brokers), Kafka-UI, Keycloak, RabbitMQ | ✅ | `0b3d2b8` (backend) |
| 2 | ms-precios | ✅ | `2cd82d4` |
| 3 | ms-alertas (desde ms-pedidos) | ✅ | `f7b08db` |
| 4 | Errores, reintentos y DLT | ✅ | `338838e`, `727d1b6` |
| 5 | ms-notificaciones (Kafka + RabbitMQ) | ✅ | `404db4e` |
| 6 | ms-admin-kafka | ✅ | `5dc9a60` |
| 7 | Frontend (4 pantallas) | ✅ | `c79646f` (frontend) |
| 8 | Prueba completa y documentación | ✅ | `7c52306` (backend), `beb8ed1` (frontend) |
| Extra 1 | Alerta `VARIACION_VENTANA` | ✅ | `c8a2d54` (backend), `2ab92f0` (frontend) |
| Extra 2 | Panel Admin Kafka en el frontend | ✅ | `703eb9e` (frontend), `bb3bce6` (backend) |
| Extra 3 | Comparador base 100 y resumen de alertas | ✅ | `3f82168` (frontend) |
| — | Id fijo del usuario demo en el realm | ✅ | `8eaea82` |
| — | Scripts `levantar.sh` / `detener.sh` y este documento | ✅ | (este commit) |

---

## 2. Qué se verificó y cómo

Todo se comprobó **ejecutándolo** contra la infraestructura real (no solo compilando).

| Qué | Cómo se verificó | Resultado |
|---|---|---|
| Clúster Kafka | `kafka-broker-api-versions` y `kafka-topics --describe` dentro de `kafka1` | 3 brokers; 4 tópicos con particiones, RF=3, min.insync=2 y retención según contrato |
| Login Keycloak | Token `password grant` con `demo/demo123` y login real en el navegador | OK; el `sub` es fijo aunque se recree el contenedor |
| ms-precios | `GET /api/precios` con JWT; mensajes en `precios.actualizados` cada 5 s | 10 criptos con fuente `BINANCE`; modo simulado y respaldo probados cortando Binance (etapa 2) |
| Alertas simples | Crear alerta ≥ / ≤ cerca del precio por API | Pasa a `DISPARADA`, evento en `alertas.disparadas`, log en ms-notificaciones |
| Disparo idempotente | Dos instancias de ms-alertas (8081 y 8086) | `alertas-group` reparte 2 + 1 particiones; sin eventos duplicados |
| Payload inválido → DLT | Mensaje `{"simbolo":"XXX","precio":-1}` con `kafka-console-producer` | No se reintenta, llega a `precios.actualizados.DLT`, `PrecioDltConsumer` lo registra; el consumo sigue |
| Reintentos con backoff | Gancho `app.demo.fallo-transitorio` (`veces=2` y `veces=4`) | Se recupera al 3.er intento / agota 3 reintentos (1-2-4 s) y va al DLT |
| RabbitMQ intacto | Confirmación "Alerta creada" al crear alertas; mensaje `FALLO_TEMPORAL` publicado a mano | Llega a `notificaciones.queue`; el fallo hace 3 intentos y pasa por `notificaciones.dlq` (lo registra `NotificacionDlqConsumer`) |
| ms-admin-kafka | curl con JWT y desde el panel (con preflight CORS de :4200) | Crear tópico, cambiar `retention.ms`, aumentar particiones, 400 al reducir, eliminar, lag de grupos |
| `VARIACION_VENTANA` | Alerta "ETH ± 0,05 % en 2 min" en modo simulado; reinicio de ms-alertas con una alerta activa | Se dispara con mensaje "subió X % (de A a B)"; al reiniciar relee 390 precios del tópico antes de evaluar |
| Validación por tipo | POST con campos faltantes, ventana > 1440, precio sin valor | 400 con mensaje claro |
| Esquema en Neon | `psql` (contenedor postgres) contra Neon | `CHECK` de `tipo` con los 3 tipos; `valor` admite null |
| Frontend | `ng build` sin errores ni avisos; capturas con Chrome headless (login real) de Mercado, Mis alertas y Admin; prueba del usuario en su navegador (etapa 7) | Sin errores en consola; tooltip del comparador funciona |
| Pruebas automáticas | `./mvnw test` en los 5 servicios (ms-alertas con `-Dspring.profiles.active=local`) | 20 tests, 0 fallos (16 en ms-alertas: 15 de reglas + contextLoads) |
| Clon limpio (etapa 8) | Clonar ambos repos en una carpeta nueva y seguir solo el README | Se levanta todo y el guion de demo completo funciona |
| ms-admin-rabbit (22-nov) | Con JWT: crear cola, exchange y binding, ver el binding en la API de RabbitMQ, y borrar los tres; sin token | 201/201/201, 204/204/204; 401 sin token. Sin cambios de código (regla del proyecto) |
| Retomada (22-nov) | `./levantar.sh` compilando todo, luego alerta real, mensaje inválido → DLT, `FALLO_TEMPORAL` → DLQ, lag, ms-admin-rabbit | Todo OK; los únicos ERROR en logs son los provocados por esas pruebas |
| Scripts | `./detener.sh` → `./levantar.sh` desde cero → smoke test → `./detener.sh` | Infra + 5 servicios + frontend en ~50 s (sin compilar) o ~10 min (compilando todo); todo se apaga sin procesos huérfanos |

---

## 3. Qué falla o falta

No hay fallos conocidos en lo implementado. Pendientes y limitaciones:

- **Clúster RabbitMQ:** es un solo nodo. El enunciado habla de "clúster"; la especificación (§3) dice que hay que preguntarle al docente si basta. **Pendiente de preguntar.**
- **Kafka sin volúmenes:** `docker compose down` borra tópicos y mensajes. Los servicios recrean los tópicos al arrancar y las alertas viven en Neon, así que no se pierde nada importante, pero Kafka-UI y el lag arrancan desde cero.
- **Historial de precios en memoria (ms-precios):** se pierde al reiniciar. Las mini gráficas y el comparador tardan unos 10 min en llenarse. **Arrancar ms-precios ~15 min antes de presentar.**
- **Evento perdido si Kafka falla justo después del UPDATE** (alerta queda DISPARADA sin evento). Se resolvería con patrón *outbox*; fuera de alcance, documentado en el README.
- **`VARIACION_VENTANA` con 2 instancias:** tras un rebalanceo, la ventana de un símbolo recién asignado puede tener un hueco (la referencia es el primer precio disponible dentro de la ventana). Documentado.
- **Con precios reales las alertas de variación tardan** (1 % en 120 min puede no ocurrir). Para la demo: `--simulado`, o usar 0,05–0,1 % en 5 min.
- **Pruebas mínimas:** solo `contextLoads` y los tests de reglas que pide la especificación (§10). El frontend no tiene tests unitarios.
- **Informe:** la especificación pide documentar en el informe por qué `precios.actualizados` retiene 24 h (reconstruir ventanas). El razonamiento está en el README ("Decisiones"); falta pasarlo al informe si hay uno aparte.

---

## 4. Levantar todo desde cero

### Opción A — con los scripts (recomendado)

Desde la carpeta `backend-cryptosim` (el frontend debe estar en `../frontend-cryptosim`):

```bash
./levantar.sh               # infra + 5 servicios (compila) + frontend; precios reales de Binance
./levantar.sh --simulado    # igual, con precios simulados (alertas se disparan rápido)
./levantar.sh --sin-compilar --sin-frontend   # opciones combinables; --help las lista
./detener.sh                # detiene frontend y servicios, y hace "docker compose down" de toda la infra
./detener.sh --dejar-infra  # detiene solo frontend y servicios
```
Logs en `logs/<servicio>.log` (ignorados por Git). Si algo no arranca, el script se detiene y dice qué log mirar.

### Opción B — a mano, en orden

Requisitos: Docker con Compose v2, Java 21, Node 22+, y el archivo de secretos de la base de datos.

```bash
# 0. Una sola vez
cd backend-cryptosim
docker network create cryptosim-net
cat > ms-alertas/src/main/resources/application-local.yml <<'EOF'
spring:
  datasource:
    password: TU_PASSWORD_DE_NEON
EOF

# 1. Infraestructura (esperar ~1 min a que Kafka tenga 3 brokers y Keycloak importe el realm)
docker compose -f infra/kafka/docker-compose.yml up -d
docker compose -f infra/kafka-ui/docker-compose.yml up -d
docker compose -f infra/rabbitmq/docker-compose.yml up -d
docker compose -f infra/keycloak/docker-compose.yml up -d
docker exec kafka1 kafka-broker-api-versions --bootstrap-server kafka1:29092 | grep -c 'id:'   # debe dar 3

# 2. Servicios: una terminal por cada uno, en este orden
cd ms-precios        && ./mvnw spring-boot:run                                  # :8084
cd ms-alertas        && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local # :8081
cd ms-notificaciones && ./mvnw spring-boot:run                                  # :8082
cd ms-admin-kafka    && ./mvnw spring-boot:run                                  # :8085
cd ms-admin-rabbit   && ./mvnw spring-boot:run                                  # :8083 (opcional)
#   precios simulados: ./mvnw spring-boot:run -Dspring-boot.run.arguments="--app.precios.modo=simulado --app.precios.volatilidad-pct=0.5"

# 3. Frontend
cd ../frontend-cryptosim
npm ci
npx ng serve                                                                     # http://localhost:4200  (demo / demo123)

# 4. Apagar
#   Ctrl+C en cada terminal, y luego:
docker compose -f infra/keycloak/docker-compose.yml down
docker compose -f infra/rabbitmq/docker-compose.yml down
docker compose -f infra/kafka-ui/docker-compose.yml down
docker compose -f infra/kafka/docker-compose.yml down
```

URLs: frontend http://localhost:4200 · Kafka-UI http://localhost:8090 · RabbitMQ http://localhost:15672 (guest/guest) · Keycloak http://localhost:8080 (admin/admin).
Guion de demo paso a paso: README, sección 5.
