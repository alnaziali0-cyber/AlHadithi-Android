#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

python3 scripts/audit_project.py
python3 -m compileall -q services/api content/resources/quran
if command -v node >/dev/null 2>&1; then
  node --check apps/admin/admin.js
else
  echo "WARN: Node.js غير متوفر؛ تم تجاوز فحص JavaScript." >&2
fi
pytest -q

if command -v flutter >/dev/null 2>&1; then
  (cd apps/mobile && flutter pub get && flutter analyze && flutter test)
else
  echo "WARN: Flutter SDK غير متوفر؛ لم يتم تشغيل flutter pub get أو flutter analyze أو flutter test أو بناء APK/IPA." >&2
fi
