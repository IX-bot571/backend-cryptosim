#!/usr/bin/env bash
# Levanta CryptoSim completo: infraestructura (Docker), los 5 microservicios y el frontend.
#
# Uso:  ./levantar.sh [--simulado] [--sin-frontend] [--sin-compilar]
#   --simulado      ms-precios en modo simulado (sin Binance; alertas se disparan rápido en la demo)
#   --sin-frontend  no arranca ng serve
#   --sin-compilar  usa los JAR ya compilados en target/ (más rápido)
#
# Logs en logs/<servicio>.log · PIDs en .run/ · Para apagar todo: ./detener.sh
set -euo pipefail

RAIZ="$(cd "$(dirname "$0")" && pwd)"
FRONTEND_DIR="${FRONTEND_DIR:-$RAIZ/../frontend-cryptosim}"
LOGS="$RAIZ/logs"
RUN="$RAIZ/.run"
SIMULADO=false; FRONTEND=true; COMPILAR=true
for a in "$@"; do
  case "$a" in
    --simulado) SIMULADO=true ;;
    --sin-frontend) FRONTEND=false ;;
    --sin-compilar) COMPILAR=false ;;
    -h|--help) sed -n '2,10p' "$0"; exit 0 ;;
    *) echo "Opción desconocida: $a (usa --help)"; exit 1 ;;
  esac
done
mkdir -p "$LOGS" "$RUN"

paso() { printf '\n\033[1;34m==> %s\033[0m\n' "$*"; }
ok()   { printf '    \033[32m✔\033[0m %s\n' "$*"; }
falla(){ printf '    \033[31m✘ %s\033[0m\n' "$*"; exit 1; }

# Espera hasta que "$2" (comando) tenga éxito, con un límite de $1 segundos.
esperar() {
  local limite=$1; shift
  for ((i = 0; i < limite; i += 2)); do
    if eval "$@" >/dev/null 2>&1; then return 0; fi
    sleep 2
  done
  return 1
}
puerto_abierto() { (exec 3<>"/dev/tcp/localhost/$1") 2>/dev/null; }

# Arranca un proceso en segundo plano en su propia sesión (setsid) y guarda en .run/<nombre>.pid el id de su
# grupo de procesos, para que detener.sh pueda terminarlo junto con sus hijos (npx → node → ng).
# El proceso escribe su propio $$ antes de hacer exec, así el PID guardado es siempre el correcto.
lanzar() {
  local nombre=$1 dir=$2; shift 2
  # "cd || exit;" y no "cd &&": con && el "&" mandaría al fondo toda la lista como un subshell intermedio,
  # que se quedaría con la salida de este script abierta.
  (cd "$dir" || exit 1; setsid bash -c 'echo $$ > "$0"; exec "$@"' "$RUN/$nombre.pid" "$@" \
     > "$LOGS/$nombre.log" 2>&1 < /dev/null &)
}

# ---------------------------------------------------------------- requisitos
paso "Comprobando requisitos"
command -v docker >/dev/null || falla "Falta Docker"
command -v java >/dev/null || falla "Falta Java 21"
java -version 2>&1 | grep -qE 'version "21' || echo "    (aviso) se recomienda Java 21: $(java -version 2>&1 | head -1)"
[ -f "$RAIZ/ms-alertas/src/main/resources/application-local.yml" ] \
  || falla "Falta ms-alertas/src/main/resources/application-local.yml con la contraseña de Neon (ver README, sección 2)"
if $FRONTEND; then
  command -v npx >/dev/null || falla "Falta Node/npm (o usa --sin-frontend)"
  [ -d "$FRONTEND_DIR" ] || falla "No encuentro el frontend en $FRONTEND_DIR (define FRONTEND_DIR o usa --sin-frontend)"
fi
for p in 8081 8082 8083 8084 8085 4200; do
  puerto_abierto $p && falla "El puerto $p ya está en uso. ¿Quedó algo corriendo? Prueba ./detener.sh"
done
ok "Docker, Java y archivo de secretos presentes; puertos libres"

# ---------------------------------------------------------------- infraestructura
paso "Infraestructura (Docker)"
docker network inspect cryptosim-net >/dev/null 2>&1 || docker network create cryptosim-net >/dev/null
for c in kafka kafka-ui rabbitmq keycloak; do
  docker compose -f "$RAIZ/infra/$c/docker-compose.yml" up -d --quiet-pull 2>&1 | grep -vE 'Running|Started|Healthy|Waiting|Created' || true
  ok "$c"
done
esperar 180 '[ "$(docker exec kafka1 kafka-broker-api-versions --bootstrap-server kafka1:29092 2>/dev/null | grep -c "id:")" = 3 ]' \
  || falla "Kafka no tiene los 3 brokers arriba (docker compose -f infra/kafka/docker-compose.yml logs)"
ok "Kafka con 3 brokers"
esperar 180 'curl -sf localhost:8080/realms/cloudnative/.well-known/openid-configuration' \
  || falla "Keycloak no respondió (docker compose -f infra/keycloak/docker-compose.yml logs)"
ok "Keycloak con el realm cloudnative"
esperar 120 'curl -sf -u guest:guest localhost:15672/api/overview' || falla "RabbitMQ no respondió"
ok "RabbitMQ"

# ---------------------------------------------------------------- microservicios
# Orden: ms-precios crea precios.actualizados; ms-alertas crea alertas.disparadas y precios.actualizados.DLT.
arrancar() {
  local svc=$1 puerto=$2; shift 2
  local jar="$RAIZ/$svc/target/$svc-0.0.1-SNAPSHOT.jar"
  if $COMPILAR || [ ! -f "$jar" ]; then
    (cd "$RAIZ/$svc" && ./mvnw -q -DskipTests package) > "$LOGS/$svc-build.log" 2>&1 \
      || falla "$svc no compiló (ver logs/$svc-build.log)"
  fi
  lanzar "$svc" "$RAIZ/$svc" java -jar "$jar" "$@"
  if esperar 120 "puerto_abierto $puerto"; then
    ok "$svc en :$puerto (log: logs/$svc.log)"
  else
    falla "$svc no abrió el puerto $puerto (ver logs/$svc.log)"
  fi
}

paso "Microservicios (compilar + arrancar)"
if $SIMULADO; then
  arrancar ms-precios 8084 --app.precios.modo=simulado --app.precios.volatilidad-pct=0.5
else
  arrancar ms-precios 8084
fi
arrancar ms-alertas 8081 --spring.profiles.active=local
arrancar ms-notificaciones 8082
arrancar ms-admin-kafka 8085
arrancar ms-admin-rabbit 8083

# ---------------------------------------------------------------- frontend
if $FRONTEND; then
  paso "Frontend (Angular)"
  if [ ! -d "$FRONTEND_DIR/node_modules" ]; then
    (cd "$FRONTEND_DIR" && npm ci --silent) > "$LOGS/frontend-npm.log" 2>&1 || falla "npm ci falló (logs/frontend-npm.log)"
  fi
  lanzar frontend "$FRONTEND_DIR" npx ng serve --port 4200
  esperar 180 'curl -sf localhost:4200' || falla "ng serve no respondió (ver logs/frontend.log)"
  ok "Frontend en :4200 (log: logs/frontend.log)"
fi

# ---------------------------------------------------------------- resumen
paso "Listo"
cat <<EOF
    Frontend      http://localhost:4200   (usuario demo / demo123)
    Kafka-UI      http://localhost:8090
    RabbitMQ      http://localhost:15672  (guest / guest)
    Keycloak      http://localhost:8080   (admin / admin)
    Precios       modo $($SIMULADO && echo simulado || echo "binance-con-respaldo (real)")
    Logs          $LOGS
    Apagar todo   ./detener.sh
EOF
