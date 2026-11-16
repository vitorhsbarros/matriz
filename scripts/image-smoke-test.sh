#!/usr/bin/env bash
# Smoke test of the application image, run through Docker Compose (docs/delivery-plan.md, section 5.4).
#
# Start the setup first, then run this from the project root:
#   docker compose --profile app up -d --wait
#   scripts/image-smoke-test.sh
#
# It checks the image (user, contents, file system, ports), the application (health, liveness and
# readiness, JSON logs, browser application, log-in, CSRF, creating and reading a task) and that the
# data survives recreating the containers. It stops at the first failed check and names it. The demo seed must be on.
#
# Settings: MATRIZ_APP_PORT (default 8080) and ENV_FILE (default .env), which holds the demo password.
#
# Against a deployed application, set BASE_URL (e.g. https://matriz.onrender.com) and give Ana's password in
# MATRIZ_DEMO_PASSWORD_ANA, without writing it on the command line (e.g. read -rs MATRIZ_DEMO_PASSWORD_ANA; export ...).
# The container, log and persistence checks are skipped; the session cookie must then be Secure.
#
# No secret is printed: the password goes to curl through a temporary file, removed at the end.
set -euo pipefail

REMOTE="${BASE_URL:+yes}"
BASE="${BASE_URL:-http://localhost:${MATRIZ_APP_PORT:-8080}}"
BASE="${BASE%/}"
ENV_FILE="${ENV_FILE:-.env}"
# A sleeping free web service takes about a minute to answer its first request.
TIMEOUT="${REMOTE:+120}"; TIMEOUT="${TIMEOUT:-10}"
COMPOSE=(docker compose --profile app)
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

pass() { echo "PASS  $1"; }
fail() { echo "FAIL  $1" >&2; exit 1; }

# Runs a request and keeps the body in $WORK/<name>. Prints the status code, or 000 when there is no
# answer, so the caller names the check that failed.
request() {
  local name="$1"; shift
  : > "$WORK/$name"
  curl --silent --max-time "$TIMEOUT" --output "$WORK/$name" --write-out '%{http_code}' \
    --cookie "$WORK/cookies" --cookie-jar "$WORK/cookies" "$@" || true
}

log_in() {
  local password="${MATRIZ_DEMO_PASSWORD_ANA:-}"
  if [ -z "$password" ]; then
    password="$(grep -E '^MATRIZ_DEMO_PASSWORD_ANA=' "$ENV_FILE" | cut -d= -f2-)" \
      || fail "MATRIZ_DEMO_PASSWORD_ANA is set, in the environment or in $ENV_FILE"
  fi
  jq -n --arg password "$password" '{email: "ana@example.com", password: $password}' > "$WORK/login.json"
  rm -f "$WORK/cookies"
  local status
  status="$(request session -H 'Content-Type: application/json' --data @"$WORK/login.json" "$BASE/api/session")"
  rm -f "$WORK/login.json"
  [ "$status" = 200 ] || fail "the demo account Ana logs in (got $status)"
  CSRF="$(jq -r .csrfToken "$WORK/session")"
  grep -q 'MATRIZ_SESSION' "$WORK/cookies" || fail "log-in sets the session cookie"
}

# --- The image -------------------------------------------------------------------------------------

if [ -n "$REMOTE" ]; then
  echo "Checking $BASE (container, log and persistence checks skipped)"
  case "$BASE" in https://*) ;; *) fail "BASE_URL uses HTTPS" ;; esac
else
APP="$("${COMPOSE[@]}" ps --quiet app)"
[ -n "$APP" ] || fail "the app service is running"

uid="$(docker exec "$APP" id -u)"
[ "$uid" != 0 ] || fail "the application doesn't run as root"
pass "the application runs as a non-root user (uid $uid)"

found="$(docker exec "$APP" sh -c 'find / -xdev \( -name .env -o -name "*.java" -o -name pom.xml \
  -o -name package.json -o -name node_modules -o -name node -o -name npm -o -name mvnw \) -print 2>/dev/null' || true)"
[ -z "$found" ] || fail "the image has no .env, source code or Node (found: $found)"
pass "the image has no .env, source code or Node"

[ "$(docker inspect --format '{{.HostConfig.ReadonlyRootfs}}' "$APP")" = true ] \
  || fail "the root file system is read-only"
if docker exec "$APP" touch /app/smoke-test 2>/dev/null; then fail "/app can't be written"; fi
pass "the root file system is read-only"

for service_port in app:8080 postgres:5432; do
  published="$("${COMPOSE[@]}" port "${service_port%%:*}" "${service_port##*:}")"
  case "$published" in
    127.0.0.1:*) ;;
    *) fail "${service_port%%:*} is published on 127.0.0.1 only (got $published)" ;;
  esac
done
pass "both ports are published on 127.0.0.1 only"
fi

# --- The application -------------------------------------------------------------------------------

for check in health health/liveness health/readiness; do
  status="$(request probe "$BASE/actuator/$check")"
  [ "$status" = 200 ] && [ "$(jq -r .status "$WORK/probe")" = UP ] || fail "/actuator/$check answers UP"
  [ "$(jq 'has("components") or has("details")' "$WORK/probe")" = false ] \
    || fail "/actuator/$check shows only its status"
done
pass "/actuator/health, its liveness and its readiness answer UP, with no details"

# Every line the application writes is a JSON object in the Elastic Common Schema.
if [ -z "$REMOTE" ]; then
docker logs "$APP" > "$WORK/logs" 2>&1
[ -s "$WORK/logs" ] || fail "the application writes logs"
not_ecs="$(jq -R -c 'fromjson? // null | select(type != "object" or ([."@timestamp", .log.level, .message,
  .service.name, .ecs.version] | any(. == null)))' "$WORK/logs" | wc -l | tr -d ' ')"
[ "$not_ecs" = 0 ] || fail "every log line is ECS JSON ($not_ecs of $(wc -l < "$WORK/logs" | tr -d ' ') are not)"
pass "every log line is ECS JSON ($(wc -l < "$WORK/logs" | tr -d ' ') lines)"
fi

status="$(request page "$BASE/")"
script="$(grep -o 'assets/index-[^"]*\.js' "$WORK/page" | head -1 || true)"
[ "$status" = 200 ] && [ -n "$script" ] || fail "/ serves the browser application"
[ "$(request script "$BASE/$script")" = 200 ] || fail "the browser application's script is served"
pass "/ and its script are served"

log_in
pass "the demo account Ana logs in"

if [ -n "$REMOTE" ]; then
  # Cookie jar columns: domain, subdomains, path, secure, expiry, name, value.
  [ "$(awk '$6 == "MATRIZ_SESSION" { print $4 }' "$WORK/cookies")" = TRUE ] || fail "the session cookie is Secure"
  grep -q '^#HttpOnly_' "$WORK/cookies" || fail "the session cookie is HttpOnly"
  pass "the session cookie is Secure and HttpOnly"
fi

[ "$(request projects "$BASE/api/projects")" = 200 ] || fail "Ana's projects are listed"
PROJECT="$(jq -r 'if type == "array" then .[0].id else (.items // .projects)[0].id end' "$WORK/projects")"
[ -n "$PROJECT" ] && [ "$PROJECT" != null ] || fail "Ana has a demo project"

TITLE="Smoke test $(date +%s)"
jq -n --arg title "$TITLE" '{title: $title}' > "$WORK/task.json"
status="$(request refused -H 'Content-Type: application/json' --data @"$WORK/task.json" \
  "$BASE/api/projects/$PROJECT/tasks")"
[ "$status" = 403 ] || fail "creating a task without the CSRF header is refused (got $status)"
pass "creating a task without the CSRF header is refused (403)"

status="$(request created -H 'Content-Type: application/json' -H "X-CSRF-Token: $CSRF" \
  --data @"$WORK/task.json" "$BASE/api/projects/$PROJECT/tasks")"
[ "$status" = 201 ] || fail "a task is created with the CSRF header (got $status)"
TASK="$(jq -r .id "$WORK/created")"
status="$(request read "$BASE/api/projects/$PROJECT/tasks/$TASK")"
[ "$status" = 200 ] && [ "$(jq -r .title "$WORK/read")" = "$TITLE" ] || fail "the new task is read back"
pass "a task is created (201) and read back"

# --- Persistence -----------------------------------------------------------------------------------

if [ -z "$REMOTE" ]; then
"${COMPOSE[@]}" down >/dev/null 2>&1
"${COMPOSE[@]}" up -d --wait >/dev/null 2>&1 || fail "the services start again after down"
log_in
status="$(request kept "$BASE/api/projects/$PROJECT/tasks/$TASK")"
[ "$status" = 200 ] && [ "$(jq -r .title "$WORK/kept")" = "$TITLE" ] \
  || fail "the task is kept after recreating the containers (got $status)"
pass "the task is kept after recreating the containers"
fi

status="$(request deleted -X DELETE -H "X-CSRF-Token: $CSRF" "$BASE/api/projects/$PROJECT/tasks/$TASK")"
[ "$status" = 204 ] || fail "the test task is deleted (got $status)"
request session-end -X DELETE -H "X-CSRF-Token: $CSRF" "$BASE/api/session" >/dev/null
pass "the test task is deleted"

echo "All checks passed."
