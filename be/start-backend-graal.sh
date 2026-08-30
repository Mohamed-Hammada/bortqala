#!/usr/bin/env bash
# WP-18 T-1: GraalVM launcher script for Bemo ERP backend
# Probes GRAALVM_HOME env variable and common install paths.
# Sets JAVA_HOME and starts the application.

set -euo pipefail

echo "[GraalVM Launcher] Probing GraalVM installation..."

# Check GRAALVM_HOME environment variable
if [[ -n "${GRAALVM_HOME:-}" && -x "$GRAALVM_HOME/bin/java" ]]; then
    echo "[GraalVM Launcher] Found GRAALVM_HOME=$GRAALVM_HOME"
    export JAVA_HOME="$GRAALVM_HOME"
else
    # Check common Linux/Mac install paths
    GRAALVM_PATHS=(
        "$HOME/.sdkman/candidates/graalvm/current"
        "$HOME/graalvm"
        "/usr/local/graalvm"
        "/opt/graalvm"
        "$HOME/.graalvm"
    )

    FOUND=false
    for p in "${GRAALVM_PATHS[@]}"; do
        if [[ -x "$p/bin/java" ]]; then
            echo "[GraalVM Launcher] Found GraalVM at $p"
            export JAVA_HOME="$p"
            FOUND=true
            break
        fi
    done

    if [[ "$FOUND" == "false" ]]; then
        # Try SDKMAN
        if command -v sdk &>/dev/null; then
            SDK_GRAALVM=$(sdk home graalvm 2>/dev/null || true)
            if [[ -n "$SDK_GRAALVM" && -x "$SDK_GRAALVM/bin/java" ]]; then
                echo "[GraalVM Launcher] Found GraalVM via sdkman at $SDK_GRAALVM"
                export JAVA_HOME="$SDK_GRAALVM"
                FOUND=true
            fi
        fi
    fi

    if [[ "$FOUND" == "false" ]]; then
        echo ""
        echo "[GraalVM Launcher] ERROR: GraalVM not found."
        echo ""
        echo "To use this script, install GraalVM and set GRAALVM_HOME:"
        echo "  export GRAALVM_HOME=/path/to/graalvm"
        echo ""
        echo "Or install via SDKMAN (Linux/Mac/WSL):"
        echo "  sdk install java 21.0.2-graalce"
        echo ""
        echo "To build a native image (requires GraalVM):"
        echo "  cd be"
        echo "  ./gradlew nativeCompile"
        echo "  ./build/native/images/bemo-erp"
        echo ""
        echo "Starting with standard JDK instead..."
        unset JAVA_HOME
    fi
fi

echo "[GraalVM Launcher] JAVA_HOME=${JAVA_HOME:-<system default>}"
echo "[GraalVM Launcher] Starting Bemo ERP backend..."
cd "$(dirname "$0")"
./gradlew bootRun
