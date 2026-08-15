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
export HR_WEB_PUSH_ENABLED="${HR_WEB_PUSH_ENABLED:-true}"
export HR_WEB_PUSH_PUBLIC_KEY="${HR_WEB_PUSH_PUBLIC_KEY:-BGMHGzdrTGM-gl0Mz9N3Vxa4ikgTuVpBkuRtliKK135FS-TYaCGmUzMSSpfgt4womxH-In5uo9dhg8NowHPBp9c}"
export HR_WEB_PUSH_PRIVATE_KEY="${HR_WEB_PUSH_PRIVATE_KEY:-jdl2o2sjy6-fXiu4RqynsTTdnfjwbFYbQibGokehV10}"
export HR_WEB_PUSH_SUBJECT="${HR_WEB_PUSH_SUBJECT:-mailto:admin@bemo-erp.local}"
export HR_WEB_PUSH_TTL_SECONDS="${HR_WEB_PUSH_TTL_SECONDS:-86400}"
echo "[INFO] Entitlement defaults: ALL IMPLEMENTED FEATURES ENABLED"
"$JAVA_HOME/bin/java" -version
cd "$SCRIPT_DIR/be"
./gradlew "-Dorg.gradle.java.home=$JAVA_HOME" bootJar
exec "$JAVA_HOME/bin/java" -Xms512m -Xmx2g -jar build/libs/bemo-erp-0.0.1-SNAPSHOT.jar "$@"
