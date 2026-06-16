#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
TAR_PATH="$ROOT_DIR/security/reports/notes-security-sca.tar"

mkdir -p security/reports
mkdir -p security/.cache/trivy

cleanup() {
  rm -f "$TAR_PATH"
}

trap cleanup EXIT

docker build -t notes-security-sca .
docker save notes-security-sca:latest -o "$TAR_PATH"

docker run --rm \
  -v "$ROOT_DIR:/workspace" \
  -v "$ROOT_DIR/security/.cache/trivy:/root/.cache/trivy" \
  aquasec/trivy:latest \
  image \
  --input /workspace/security/reports/notes-security-sca.tar \
  --scanners vuln \
  --severity MEDIUM,HIGH,CRITICAL \
  --format json \
  --output /workspace/security/reports/trivy-sca.json \
  --no-progress

docker run --rm \
  -v "$ROOT_DIR:/workspace" \
  -v "$ROOT_DIR/security/.cache/trivy:/root/.cache/trivy" \
  aquasec/trivy:latest \
  image \
  --input /workspace/security/reports/notes-security-sca.tar \
  --scanners vuln \
  --severity HIGH,CRITICAL \
  --exit-code 1 \
  --no-progress
