#!/usr/bin/env bash

set -euo pipefail

MODE="${1:-}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ACTIVE_MAPPING_DIR="$REPO_ROOT/wiremock/active-mappings"
SOURCE_MAPPING_DIR="$REPO_ROOT/wiremock/mappings"

if [[ -z "$MODE" ]]; then
  echo "Usage: $0 <201|400|500|500-then-201>"
  exit 1
fi

case "$MODE" in
  201|400|500|500-then-201) ;;
  *)
    echo "Invalid mode: $MODE"
    echo "Usage: $0 <201|400|500|500-then-201>"
    exit 1
    ;;
esac

mkdir -p "$ACTIVE_MAPPING_DIR"
rm -f "$ACTIVE_MAPPING_DIR"/*.json

if [[ "$MODE" == "500-then-201" ]]; then
  cp "$SOURCE_MAPPING_DIR/notify-send-email-500-then-201-first.json" "$ACTIVE_MAPPING_DIR/notify-send-email-500-then-201-first.json"
  cp "$SOURCE_MAPPING_DIR/notify-send-email-500-then-201-second.json" "$ACTIVE_MAPPING_DIR/notify-send-email-500-then-201-second.json"
else
  cp "$SOURCE_MAPPING_DIR/notify-send-email-$MODE.json" "$ACTIVE_MAPPING_DIR/notify-send-email.json"
fi

docker compose up -d notify-stub >/dev/null
docker compose restart notify-stub >/dev/null

for _ in {1..30}; do
  if curl -fsS http://localhost:8093/__admin/mappings >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

if ! curl -fsS http://localhost:8093/__admin/mappings >/dev/null 2>&1; then
  echo "notify-stub did not become ready on http://localhost:8093"
  exit 1
fi

echo "notify-stub mode set to $MODE"



