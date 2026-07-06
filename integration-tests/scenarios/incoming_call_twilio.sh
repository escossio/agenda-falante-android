#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
REPORT_ROOT="$ROOT_DIR/integration-tests/reports"
DEVICE_ID=""
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-45}"
TIMESTAMP="$(date '+%Y%m%d-%H%M%S')"
REPORT_DIR="$REPORT_ROOT/incoming-call-$TIMESTAMP"
APK_PATH="$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"
CALL_RESULT="NOT_SENT"
RESULT="INCONCLUSIVE"
LOGCAT_PID=""

usage() {
  cat <<EOF
Usage: incoming_call_twilio.sh [--timeout SECONDS]
EOF
}

require_bin() {
  local bin_name="$1"
  if ! command -v "$bin_name" >/dev/null 2>&1; then
    echo "Error: required command '$bin_name' was not found." >&2
    exit 1
  fi
}

detect_device() {
  local device physical emulator
  physical=""
  emulator=""
  while IFS=$'\t' read -r serial state; do
    [ -n "${serial:-}" ] || continue
    [ "$state" = "device" ] || continue
    case "$serial" in
      emulator-*) emulator="${emulator:-$serial}" ;;
      *) physical="${physical:-$serial}" ;;
    esac
  done < <(adb devices | sed -n '2,$p')

  if [ -n "$physical" ]; then
    printf '%s\n' "$physical"
    return 0
  fi

  if [ -n "$emulator" ]; then
    echo "Error: only emulator device detected. Connect a physical phone." >&2
    return 1
  fi

  echo "Error: no physical Android device detected." >&2
  return 1
}

cleanup() {
  if [ -n "$LOGCAT_PID" ]; then
    kill "$LOGCAT_PID" >/dev/null 2>&1 || true
    wait "$LOGCAT_PID" >/dev/null 2>&1 || true
  fi
}

trap cleanup EXIT

open_app() {
  adb -s "$DEVICE_ID" shell am start -n com.escossio.agendafalante/.MainActivity >/dev/null
}

collect_summary() {
  local logcat_result="$1"
  local model android_version head_value call_result_value
  model="$(adb -s "$DEVICE_ID" shell getprop ro.product.model | tr -d '\r')"
  android_version="$(adb -s "$DEVICE_ID" shell getprop ro.build.version.release | tr -d '\r')"
  head_value="$(git -C "$ROOT_DIR" rev-parse --short HEAD)"
  call_result_value="$CALL_RESULT"
  {
    echo "Data/hora: $(date '+%Y-%m-%d %H:%M:%S %Z')"
    echo "Device ID: $DEVICE_ID"
    echo "Modelo do aparelho: $model"
    echo "Android version: $android_version"
    echo "Commit HEAD: $head_value"
    echo "APK path: $APK_PATH"
    echo "Resultado Twilio: $call_result_value"
    echo "Resultado logcat: $logcat_result"
    echo "Caminho dos artefatos: $REPORT_DIR"
    echo "RESULT=$RESULT"
  } > "$REPORT_DIR/summary.txt"
}

if [ "$#" -gt 0 ]; then
  while [ "$#" -gt 0 ]; do
    case "$1" in
      --timeout)
        shift
        TIMEOUT_SECONDS="${1:-}"
        if [ -z "$TIMEOUT_SECONDS" ]; then
          echo "Error: --timeout requires a value." >&2
          exit 1
        fi
        ;;
      -h|--help)
        usage
        exit 0
        ;;
      *)
        echo "Error: unknown argument '$1'." >&2
        usage >&2
        exit 1
        ;;
    esac
    shift
  done
fi

require_bin adb
require_bin grep

DEVICE_ID="$(detect_device)"

mkdir -p "$REPORT_DIR"

echo "Building debug APK..."
(cd "$ROOT_DIR" && ./gradlew assembleDebug)

echo "Installing APK on $DEVICE_ID..."
adb -s "$DEVICE_ID" install --no-streaming -r -d "$APK_PATH"

echo "Opening app..."
open_app

LOGCAT_FILE="$REPORT_DIR/logcat.txt"
echo "Starting logcat capture..."
adb -s "$DEVICE_ID" logcat -v time -s com.escossio.agendafalante > "$LOGCAT_FILE" &
LOGCAT_PID=$!

if [ -x "$ROOT_DIR/integration-tests/providers/twilio/call_phone.sh" ]; then
  if "$ROOT_DIR/integration-tests/providers/twilio/call_phone.sh" > "$REPORT_DIR/twilio-call.txt" 2>&1; then
    CALL_RESULT="SENT"
  else
    CALL_RESULT="FAILED"
    collect_summary "no_match"
    echo "RESULT=$RESULT"
    echo "Report directory: $REPORT_DIR"
    exit 1
  fi
else
  echo "Error: integration-tests/providers/twilio/call_phone.sh is not executable." >&2
  exit 1
fi

sleep "$TIMEOUT_SECONDS"

kill "$LOGCAT_PID" >/dev/null 2>&1 || true
wait "$LOGCAT_PID" >/dev/null 2>&1 || true

adb -s "$DEVICE_ID" exec-out screencap -p > "$REPORT_DIR/screenshot.png"

logcat_result="no_match"
if grep -Eq 'incoming_call|Telephony event|IncomingCallEvent' "$LOGCAT_FILE"; then
  RESULT="PASS"
  logcat_result="matched"
else
  RESULT="INCONCLUSIVE"
fi

collect_summary "$logcat_result"
echo "RESULT=$RESULT"
echo "Report directory: $REPORT_DIR"
