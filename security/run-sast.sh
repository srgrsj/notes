#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPORT_DIR="$ROOT_DIR/security/reports"
mkdir -p "$REPORT_DIR"

docker run --rm \
  -v "$ROOT_DIR:/src" \
  returntocorp/semgrep:latest \
  semgrep \
  --config /src/security/semgrep/rules.yml \
  --config p/owasp-top-ten \
  --config p/secrets \
  --json \
  --output /src/security/reports/semgrep.json \
  /src/src /src/compose.yml /src/Dockerfile

docker run --rm \
  -v "$ROOT_DIR:/src" \
  returntocorp/semgrep:latest \
  semgrep \
  --config /src/security/semgrep/rules.yml \
  --config p/owasp-top-ten \
  --config p/secrets \
  --json \
  --output /src/security/reports/semgrep-demo.json \
  /src/security/fixtures
