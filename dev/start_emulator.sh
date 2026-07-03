#!/usr/bin/env bash
set -euo pipefail

if [ "${EUID:-$(id -u)}" -eq 0 ]; then
  echo "Error: do not run this script as root." >&2
  exit 1
fi

if [ "$(id -un)" != "leonardo" ]; then
  echo "Error: run as user 'leonardo'." >&2
  exit 1
fi

SDK_ROOT="/home/leonardo/Android/Sdk"
AVD_NAME="AgendaFalante_API35"
EMU_BIN="$SDK_ROOT/emulator/emulator"

if [ ! -x "$EMU_BIN" ]; then
  echo "Error: emulator not found at $EMU_BIN" >&2
  exit 1
fi

if pgrep -u leonardo -f "emulator.*$AVD_NAME" >/dev/null 2>&1; then
  echo "Emulator already running: $AVD_NAME"
  exit 0
fi

export ANDROID_HOME="$SDK_ROOT"
export ANDROID_SDK_ROOT="$SDK_ROOT"
export PATH="$SDK_ROOT/platform-tools:$SDK_ROOT/emulator:$SDK_ROOT/cmdline-tools/latest/bin:$PATH"

nohup "$EMU_BIN" -avd "$AVD_NAME" -no-window -no-audio -no-snapshot -gpu swiftshader_indirect >/tmp/${AVD_NAME}.log 2>&1 &
echo "Started emulator: $AVD_NAME"
