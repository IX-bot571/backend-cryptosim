#!/usr/bin/env bash
# Detiene lo que levantó ./levantar.sh: frontend, microservicios y (salvo --dejar-infra) los contenedores.
#
# Uso:  ./detener.sh [--dejar-infra]
#   --dejar-infra   detiene solo frontend y servicios; Kafka, Keycloak, RabbitMQ y Kafka-UI siguen arriba
#
# Ojo: Kafka no usa volúmenes, así que "down" borra tópicos y mensajes (los servicios los recrean al arrancar).
# Las alertas no se pierden: viven en Neon.
set -uo pipefail

RAIZ="$(cd "$(dirname "$0")" && pwd)"
RUN="$RAIZ/.run"
INFRA=true
for a in "$@"; do
  case "$a" in
    --dejar-infra) INFRA=false ;;
    -h|--help) sed -n '2,8p' "$0"; exit 0 ;;
    *) echo "Opción desconocida: $a (usa --help)"; exit 1 ;;
  esac
done

paso() { printf '\n\033[1;34m==> %s\033[0m\n' "$*"; }
ok()   { printf '    \033[32m✔\033[0m %s\n' "$*"; }

paso "Frontend y microservicios"
shopt -s nullglob
pids=("$RUN"/*.pid)
if [ ${#pids[@]} -eq 0 ]; then
  ok "no hay procesos registrados en .run/"
fi
for f in "${pids[@]}"; do
  nombre=$(basename "$f" .pid)
  pid=$(cat "$f")
  if kill -0 "$pid" 2>/dev/null; then
    kill -TERM -- "-$pid" 2>/dev/null || kill -TERM "$pid" 2>/dev/null
    for _ in $(seq 1 15); do kill -0 "$pid" 2>/dev/null || break; sleep 1; done
    if kill -0 "$pid" 2>/dev/null; then
      kill -KILL -- "-$pid" 2>/dev/null || kill -KILL "$pid" 2>/dev/null
      ok "$nombre detenido (forzado)"
    else
      ok "$nombre detenido"
    fi
  else
    ok "$nombre ya no estaba corriendo"
  fi
  rm -f "$f"
done

# Aviso si algo sigue ocupando los puertos (por ejemplo, un servicio arrancado a mano con mvnw).
for p in 8081 8082 8083 8084 8085 8086 4200; do
  if (exec 3<>"/dev/tcp/localhost/$p") 2>/dev/null; then
    echo "    (aviso) el puerto $p sigue ocupado por un proceso no iniciado con levantar.sh"
  fi
done

if $INFRA; then
  paso "Infraestructura (docker compose down)"
  for c in keycloak rabbitmq kafka-ui kafka; do
    docker compose -f "$RAIZ/infra/$c/docker-compose.yml" down 2>&1 | grep -vE 'Stopping|Stopped|Removing|Removed' || true
    ok "$c"
  done
fi

paso "Listo"
