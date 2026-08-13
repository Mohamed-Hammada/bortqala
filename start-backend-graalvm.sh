#!/usr/bin/env sh
set -eu

if [ -z "${GRAALVM_HOME:-}" ]; then
  echo "[ERROR] Set GRAALVM_HOME to a GraalVM JDK 21 or newer." >&2
  exit 1
fi
if [ ! -x "$GRAALVM_HOME/bin/java" ]; then
  echo "[ERROR] $GRAALVM_HOME/bin/java was not found or is not executable." >&2
  exit 1
fi

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
export JAVA_HOME="$GRAALVM_HOME"
export PATH="$JAVA_HOME/bin:$PATH"
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"

"$JAVA_HOME/bin/java" -version
cd "$SCRIPT_DIR/be"
./gradlew bootJar
exec "$JAVA_HOME/bin/java" -Xms512m -Xmx2g -jar build/libs/bemo-erp-0.0.1-SNAPSHOT.jar "$@"
