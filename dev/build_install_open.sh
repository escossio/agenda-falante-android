#!/usr/bin/env bash
set -euo pipefail

SDK_ROOT="/home/leonardo/Android/Sdk"
PACKAGE_NAME="com.escossio.agendafalante"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

if [ "${EUID:-$(id -u)}" -eq 0 ]; then
  echo "Error: do not run this script as root." >&2
  exit 1
fi

export ANDROID_HOME="$SDK_ROOT"
export ANDROID_SDK_ROOT="$SDK_ROOT"
export PATH="$SDK_ROOT/platform-tools:$SDK_ROOT/emulator:$SDK_ROOT/cmdline-tools/latest/bin:$PATH"

./gradlew assembleDebug

if ! adb devices | awk 'NR>1 && $2 == "device" {found=1} END {exit found ? 0 : 1}'; then
  echo "Error: no connected Android device/emulator found." >&2
  exit 1
fi

adb install -r "$APK_PATH"
adb shell monkey -p "$PACKAGE_NAME" 1
