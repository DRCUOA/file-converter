#!/bin/zsh
# Launches Converto.app with the project root as working directory,
# so settings.json is resolved from this directory.
# Pass --rebuild to run ./gradlew jpackageApp before launching.
set -e

SCRIPT_DIR="${0:A:h}"
cd "$SCRIPT_DIR"

if [[ "$1" == --rebuild ]]; then
  echo "Running rebuild…"
  ./gradlew --stop
  ./gradlew jpackageApp
  shift
fi

APP_PATH="$SCRIPT_DIR/build/jpackage/Converto.app"
if [[ ! -d "$APP_PATH" ]]; then
  echo "Converto.app not found at $APP_PATH" >&2
  echo "Build it first: ./gradlew jpackageApp" >&2
  exit 1
fi

exec "$APP_PATH/Contents/MacOS/Converto"
