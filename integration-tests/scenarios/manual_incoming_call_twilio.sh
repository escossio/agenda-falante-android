#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
APK_PATH="$ROOT_DIR/release-local/agenda-falante-debug.apk"
REPORT_ROOT="$ROOT_DIR/integration-tests/reports"
TIMESTAMP="$(date '+%Y%m%d-%H%M%S')"
REPORT_DIR="$REPORT_ROOT/manual-incoming-call-$TIMESTAMP"
SUMMARY_FILE="$REPORT_DIR/summary.txt"
CONFIRMED="no"
TWILIO_RESULT="not-run"
CALL_OUTPUT_FILE="$REPORT_DIR/twilio-call.txt"

sha256_of() {
  sha256sum "$1" | awk '{print $1}'
}

if [ ! -f "$APK_PATH" ]; then
  echo "Error: missing APK at $APK_PATH." >&2
  exit 1
fi

APK_SHA="$(sha256_of "$APK_PATH")"
APK_SIZE="$(stat -c '%s' "$APK_PATH")"

echo "APK para instalação manual:"
echo "$APK_PATH"
echo "SHA-256: $APK_SHA"
echo "Tamanho: $APK_SIZE bytes"
echo
echo "Instale este APK manualmente no celular antes de continuar."
echo
echo 'Você já instalou este APK no celular? [y/N]'
read -r answer
if [ "${answer:-}" != "y" ]; then
  echo "Abortado pelo usuário."
  exit 1
fi
CONFIRMED="yes"

mkdir -p "$REPORT_DIR"
{
  echo "Data/hora: $(date '+%Y-%m-%d %H:%M:%S %Z')"
  echo "Commit HEAD: $(git -C "$ROOT_DIR" rev-parse --short HEAD)"
  echo "Caminho do APK: $APK_PATH"
  echo "SHA-256: $APK_SHA"
  echo "Confirmacao do usuario: $CONFIRMED"
  echo "Resultado Twilio: $TWILIO_RESULT"
  echo "Observacao: validacao manual pendente."
} > "$SUMMARY_FILE"

echo "Disparando chamada real via Twilio..."
if "$ROOT_DIR/integration-tests/providers/twilio/call_phone.sh" > "$CALL_OUTPUT_FILE" 2>&1; then
  TWILIO_RESULT="sent"
  echo "Call request sent."
else
  TWILIO_RESULT="failed"
  cat "$CALL_OUTPUT_FILE" >&2
  {
    echo "Data/hora: $(date '+%Y-%m-%d %H:%M:%S %Z')"
    echo "Commit HEAD: $(git -C "$ROOT_DIR" rev-parse --short HEAD)"
    echo "Caminho do APK: $APK_PATH"
    echo "SHA-256: $APK_SHA"
    echo "Confirmacao do usuario: $CONFIRMED"
    echo "Resultado Twilio: $TWILIO_RESULT"
    echo "Observacao: validacao manual pendente."
  } > "$SUMMARY_FILE"
  exit 1
fi

echo
echo "Observe no celular:"
echo "- Telephony Status"
echo "- Last Telephony Event"
echo "- nenhum áudio automático, se esse ainda for o comportamento esperado"

{
  echo "Data/hora: $(date '+%Y-%m-%d %H:%M:%S %Z')"
  echo "Commit HEAD: $(git -C "$ROOT_DIR" rev-parse --short HEAD)"
  echo "Caminho do APK: $APK_PATH"
  echo "SHA-256: $APK_SHA"
  echo "Confirmacao do usuario: $CONFIRMED"
  echo "Resultado Twilio: $TWILIO_RESULT"
  echo "Observacao: validacao manual pendente."
} > "$SUMMARY_FILE"

echo "Relatório: $REPORT_DIR"
