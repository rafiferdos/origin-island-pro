#!/usr/bin/env bash
# ==============================================================================
# raiden-bridge.sh
# Route B Companion Bridge Script for Origin Island Pro Raiden Card
#
# Dispatches secured live state broadcasts directly to PlaygroundService:
# - States: working, typing, coding, reading Gmail, searching, waiting, done, error
# - Dispatched via local Termux shell or host ADB connection.
# - Protected by permission: com.originisle.android.permission.RAIDEN_STATE
#
# Usage:
#   ./raiden-bridge.sh <state> [task_description] [file_name] [source_pkg]
#
# Examples:
#   ./raiden-bridge.sh working "Analyzing repository metrics"
#   ./raiden-bridge.sh coding "Refactoring priority arbitration" "PlaygroundService.kt"
#   ./raiden-bridge.sh "reading Gmail" "Checking server alerts"
#   ./raiden-bridge.sh waiting "Awaiting user approval"
#   ./raiden-bridge.sh done "Task completed successfully"
#   ./raiden-bridge.sh error "Failed to connect to backend"
# ==============================================================================

set -euo pipefail

STATE="${1:-working}"
TASK="${2:-}"
FILE="${3:-}"
PKG="${4:-ai.muse}"

ACTION="com.originisle.android.action.RAIDEN_STATE"
PERMISSION="com.originisle.android.permission.RAIDEN_STATE"

# Build am broadcast argument list
ARGS=(
  -a "$ACTION"
  --receiver-permission "$PERMISSION"
  --es state "$STATE"
)

if [[ -n "$TASK" ]]; then
  ARGS+=(--es task "$TASK")
fi

if [[ -n "$FILE" ]]; then
  ARGS+=(--es file "$FILE")
fi

if [[ -n "$PKG" ]]; then
  ARGS+=(--es source_pkg "$PKG")
fi

echo "[Raiden Bridge] Dispatching state: '$STATE' | task: '$TASK' | file: '$FILE' | pkg: '$PKG'"

# Detect whether we are running on device (Termux/Android shell) or desktop host (ADB)
if command -v adb >/dev/null 2>&1 && ! [[ -d /system && -d /data ]]; then
  echo "[Raiden Bridge] Host environment detected. Using 'adb shell am broadcast'..."
  adb shell am broadcast "${ARGS[@]}"
elif command -v am >/dev/null 2>&1; then
  echo "[Raiden Bridge] Android / Termux environment detected. Using 'am broadcast'..."
  am broadcast "${ARGS[@]}"
else
  echo "Error: Neither 'adb' nor Android 'am' command was found." >&2
  exit 1
fi

echo "[Raiden Bridge] Broadcast sent successfully."
