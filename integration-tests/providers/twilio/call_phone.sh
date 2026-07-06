#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/twilio.env"

usage() {
  cat <<'EOF'
Usage: call_phone.sh [--to PHONE] [--from PHONE] [--message TEXT]
EOF
}

require_bin() {
  local bin_name="$1"
  if ! command -v "$bin_name" >/dev/null 2>&1; then
    echo "Error: required command '$bin_name' was not found." >&2
    exit 1
  fi
}

trim() {
  local value="${1:-}"
  value="${value#${value%%[![:space:]]*}}"
  value="${value%${value##*[![:space:]]}}"
  printf '%s' "$value"
}

if [ ! -f "$ENV_FILE" ]; then
  echo "Error: missing $ENV_FILE. Copy twilio.example.env to twilio.env and fill the values." >&2
  exit 1
fi

require_bin curl

set -a
# shellcheck disable=SC1090
. "$ENV_FILE"
set +a

TO="${AGENDA_FALANTE_TEST_PHONE_NUMBER:-}"
FROM="${TWILIO_FROM_NUMBER:-}"
MESSAGE="Teste do Agenda Falante."

while [ $# -gt 0 ]; do
  case "$1" in
    --to)
      shift
      TO="${1:-}"
      ;;
    --from)
      shift
      FROM="${1:-}"
      ;;
    --message)
      shift
      MESSAGE="${1:-}"
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

missing=0
for var_name in TWILIO_ACCOUNT_SID TWILIO_AUTH_TOKEN TWILIO_FROM_NUMBER AGENDA_FALANTE_TEST_PHONE_NUMBER; do
  if [ -z "${!var_name:-}" ]; then
    echo "Error: missing required variable $var_name in $ENV_FILE." >&2
    missing=1
  fi
done

if [ "$missing" -ne 0 ]; then
  exit 1
fi

if [ -z "${TO:-}" ]; then
  echo "Error: missing destination number. Set AGENDA_FALANTE_TEST_PHONE_NUMBER or pass --to." >&2
  exit 1
fi

if [ -z "${FROM:-}" ]; then
  echo "Error: missing source number. Set TWILIO_FROM_NUMBER or pass --from." >&2
  exit 1
fi

response_file="$(mktemp)"
cleanup() {
  rm -f "$response_file"
}
trap cleanup EXIT

if ! curl -sS -u "${TWILIO_ACCOUNT_SID}:${TWILIO_AUTH_TOKEN}" \
  -X POST "https://api.twilio.com/2010-04-01/Accounts/${TWILIO_ACCOUNT_SID}/Calls.json" \
  --data-urlencode "To=${TO}" \
  --data-urlencode "From=${FROM}" \
  --data-urlencode "Twiml=<Response><Say>${MESSAGE}</Say></Response>" \
  -o "$response_file"; then
  echo "Error: failed to send Twilio call request." >&2
  exit 1
fi

echo "Call request sent."

if command -v python3 >/dev/null 2>&1; then
  call_sid="$(python3 - <<'PY' "$response_file"
import json
import sys
from pathlib import Path

payload = Path(sys.argv[1]).read_text(encoding="utf-8")
try:
    data = json.loads(payload)
except Exception:
    sys.exit(0)
print(data.get("sid", ""))
PY
)"
  if [ -n "$call_sid" ]; then
    echo "Call SID: $call_sid"
  fi
fi
