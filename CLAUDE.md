# CryptoSim — instrucciones para Claude Code

Monitor de criptomonedas con alertas (proyecto académico, EP5 DSY1107). Entrega: **jueves 26-nov-2026, 08:30**. Trabajo individual.

**Lee primero `docs/ESPECIFICACION.md`.** Es la fuente de verdad (alcance, servicios, tópicos, DTOs, etapas y criterios de aceptación). Si algo no está claro o hay que desviarse de ella, pregunta antes de hacerlo.

## Stack
Java 21, Spring Boot 4.1.1 (Maven wrapper), Angular 22, Keycloak 26 (JWT), PostgreSQL en Neon, RabbitMQ 3, Kafka (Zookeeper + 3 brokers). Boot 4 usa Jackson 3.

## Cómo trabajar
- **Una etapa a la vez**, en el orden de la sección 12 de la especificación. No adelantes etapas.
- Al terminar una etapa: compilar (`./mvnw -q -DskipTests package` o `ng build`), comprobar el criterio de aceptación **ejecutándolo**, y recién ahí hacer commit.
- Si algo falla, lee el error real (logs, `docker compose logs`) antes de cambiar código. Arregla la causa, no el síntoma.
- Antes de usar una API de Spring Boot 4 o Spring Kafka 4 de la que no estés seguro, comprueba que existe en el JAR resuelto. Los nombres cambiaron respecto a Boot 3.
- Cambios pequeños y revisables. No reescribas archivos que ya funcionan.
- Explica en pocas líneas qué hiciste y por qué en cada etapa: el autor debe poder **defender el código** en una presentación.

## Reglas del proyecto
- Nombres de tópicos, grupos, colas y exchanges **solo** en `application.yml` vía `@ConfigurationProperties`.
- DTOs como `record`. Precios y dinero con `BigDecimal`. Fechas con `Instant`.
- `@KafkaListener` solo recibe, valida y delega a un `@Service`. La lógica de negocio no conoce Kafka.
- El usuario sale siempre del JWT (`sub`), nunca del cuerpo de la petición.
- **No tocar `ms-admin-rabbit`. No romper el flujo RabbitMQ existente.**
- Logs con `@Slf4j`, contextuales (tópico, partición, offset, clave). Nada de `System.out`.
- Sin dependencias nuevas salvo necesidad real. El frontend usa SVG propio para gráficas.

## Seguridad y Git
- **Nunca** subir ni imprimir secretos. La contraseña de Neon vive solo en `application-local.yml` (ignorado por Git).
- Revisa `git status` antes de cada commit. Deben estar ignorados: `target/`, `node_modules/`, `dist/`, `.angular/`, `.idea/`, `.vscode/`, `application-local.yml`, `.env`.
- Mensajes de commit en español, una línea, por etapa.

## Comandos útiles
```bash
docker network create cryptosim-net
docker compose -f infra/kafka/docker-compose.yml up -d
docker compose -f infra/kafka-ui/docker-compose.yml up -d
docker compose -f infra/rabbitmq/docker-compose.yml up -d
docker compose -f infra/keycloak/docker-compose.yml up -d
./mvnw spring-boot:run                                   # desde la carpeta de cada servicio
./mvnw spring-boot:run -Dspring-boot.run.profiles=local  # servicios con BD (ms-alertas)
ng serve                                                 # frontend en :4200
```

## Puertos
Keycloak 8080 · ms-alertas 8081 · ms-notificaciones 8082 · ms-admin-rabbit 8083 · ms-precios 8084 · ms-admin-kafka 8085 · Kafka-UI 8090 · RabbitMQ 5672/15672 · Kafka 9092/9093/9094 · Zookeeper 2181 · Frontend 4200.

## Ahorro de uso
Pide y haz tareas acotadas. No releas archivos grandes sin necesidad ni pegues logs completos: cita solo las líneas relevantes.
