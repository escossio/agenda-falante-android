#!/usr/bin/env bash
set -euo pipefail

SDK_ROOT="/home/leonardo/Android/Sdk"
PACKAGE_NAME="com.escossio.agendafalante"

if [ "${EUID:-$(id -u)}" -eq 0 ]; then
  echo "Error: do not run this script as root." >&2
  exit 1
fi

export ANDROID_HOME="$SDK_ROOT"
export ANDROID_SDK_ROOT="$SDK_ROOT"
export PATH="$SDK_ROOT/platform-tools:$SDK_ROOT/emulator:$SDK_ROOT/cmdline-tools/latest/bin:$PATH"

adb logcat --pid="$(adb shell pidof "$PACKAGE_NAME" | tr -d '\r')" 2>/dev/null || adb logcat | grep --line-buffered "$PACKAGE_NAME"
