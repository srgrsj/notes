#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPORT_DIR="$ROOT_DIR/security/reports"
TARGET_URL="${DAST_TARGET:-http://host.docker.internal:8080}"
mkdir -p "$REPORT_DIR"

docker compose -f "$ROOT_DIR/compose.yml" up -d --build
trap 'docker compose -f "$ROOT_DIR/compose.yml" down' EXIT

for _ in {1..30}; do
  if curl -fsS http://127.0.0.1:8080 >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

DOCKER_ARGS=(
  --rm
  -v "$ROOT_DIR/security:/zap/sec"
  -v "$REPORT_DIR:/zap/wrk"
)

if [[ -n "${ZAP_DOCKER_NETWORK_MODE:-}" ]]; then
  DOCKER_ARGS+=(--network "$ZAP_DOCKER_NETWORK_MODE")
fi

if [[ -n "${ZAP_DOCKER_ADD_HOST:-}" ]]; then
  DOCKER_ARGS+=(--add-host "$ZAP_DOCKER_ADD_HOST")
fi

docker run "${DOCKER_ARGS[@]}" \
  ghcr.io/zaproxy/zaproxy:stable \
  zap-baseline.py \
  -t "$TARGET_URL" \
  -c /zap/sec/zap/zap-rules.tsv \
  -I \
  -J zap-report.json \
  -r zap-report.html \
  -w zap-report.md \
  -x zap-report.xml
