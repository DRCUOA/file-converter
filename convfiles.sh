#!/usr/bin/env bash
set -euo pipefail

APP_DIR="/Volumes/SecureData/c26/active/file-converter"

cd "$APP_DIR"

if [[ "${1:-}" == "--rebuild" ]]; then
    echo "Running rebuild…"
    ./gradlew --stop
    ./gradlew clean run --rerun-tasks
else
    echo "Running normal start…"
    ./gradlew run
fi

