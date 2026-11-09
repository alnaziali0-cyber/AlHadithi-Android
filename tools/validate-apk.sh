#!/usr/bin/env bash
set -Eeuo pipefail

APK="${1:-app/build/outputs/apk/release/app-release.apk}"
fail() { printf 'ERROR: %s\n' "$*" >&2; exit 2; }
[[ -f "$APK" ]] || fail "APK file not found: $APK"
command -v unzip >/dev/null 2>&1 || fail "unzip is required"

printf 'APK: %s\n' "$APK"
sha256sum "$APK"
unzip -t "$APK" >/dev/null || fail "ZIP container validation failed"

entries="$(unzip -Z1 "$APK")"
grep -qx 'AndroidManifest.xml' <<<"$entries" || fail "AndroidManifest.xml is missing"
grep -qx 'resources.arsc' <<<"$entries" || fail "resources.arsc is missing"
grep -Eq '^classes([0-9]+)?\.dex$' <<<"$entries" || fail "classes.dex is missing"

SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
find_tool() {
  local name="$1"
  if command -v "$name" >/dev/null 2>&1; then command -v "$name"; return 0; fi
  if [[ -n "$SDK_ROOT" && -d "$SDK_ROOT/build-tools" ]]; then
    local found
    found="$(find "$SDK_ROOT/build-tools" -type f -name "$name" -print 2>/dev/null | sort -V | tail -n1)"
    if [[ -n "$found" ]]; then printf '%s\n' "$found"; return 0; fi
  fi
  if [[ "$name" == "apkanalyzer" && -n "$SDK_ROOT" && -d "$SDK_ROOT/cmdline-tools" ]]; then
    local found
    found="$(find "$SDK_ROOT/cmdline-tools" -type f -name "$name" -path '*/bin/*' -print 2>/dev/null | sort -V | tail -n1)"
    if [[ -n "$found" ]]; then printf '%s\n' "$found"; return 0; fi
  fi
  return 1
}

APKSIGNER="$(find_tool apksigner || true)"
ZIPALIGN="$(find_tool zipalign || true)"
AAPT="$(find_tool aapt || true)"
APKANALYZER="$(find_tool apkanalyzer || true)"

[[ -n "$APKSIGNER" ]] || fail "apksigner is unavailable; install Android SDK Build Tools"
[[ -n "$ZIPALIGN" ]] || fail "zipalign is unavailable; install Android SDK Build Tools"

printf '\n== Signature verification ==\n'
"$APKSIGNER" verify --verbose --print-certs "$APK"
printf '\n== Zip alignment ==\n'
"$ZIPALIGN" -c -v 4 "$APK"
printf '\n== Package metadata ==\n'
if [[ -n "$APKANALYZER" ]]; then
  PACKAGE_ID="$("$APKANALYZER" manifest application-id "$APK" | tr -d '\r')"
  MIN_SDK="$("$APKANALYZER" manifest min-sdk "$APK" | tr -d '\r')"
  TARGET_SDK="$("$APKANALYZER" manifest target-sdk "$APK" | tr -d '\r')"
  MANIFEST_TEXT="$("$APKANALYZER" manifest print "$APK")"
elif [[ -n "$AAPT" ]]; then
  BADGING="$("$AAPT" dump badging "$APK")"
  PACKAGE_LINE="$(grep -m1 '^package:' <<<"$BADGING" || true)"
  PACKAGE_ID="$(sed -n "s/.*name='\([^']*\)'.*/\1/p" <<<"$PACKAGE_LINE")"
  MIN_SDK="$(sed -n "s/.*sdkVersion:'\([^']*\)'.*/\1/p" <<<"$BADGING" | head -n1)"
  TARGET_SDK="$(sed -n "s/.*targetSdkVersion:'\([^']*\)'.*/\1/p" <<<"$BADGING" | head -n1)"
  MANIFEST_TEXT="$("$AAPT" dump xmltree "$APK" AndroidManifest.xml)"
else
  fail "apkanalyzer or aapt is required to read APK manifest metadata"
fi
printf 'applicationId: %s\nminSdk: %s\ntargetSdk: %s\n' "$PACKAGE_ID" "$MIN_SDK" "$TARGET_SDK"
[[ "$PACKAGE_ID" == "com.aistudio.alhadithi.iqshia" ]] || fail "Unexpected applicationId: $PACKAGE_ID"
if grep -Eiq '(^|[[:space:]])split[[:space:]]*=|configForSplit[[:space:]]*=|isFeatureSplit[[:space:]]*=[[:space:]]*(true|0xffffffff)' <<<"$MANIFEST_TEXT"; then
  fail "The APK manifest indicates a split/feature APK; a standalone installable APK is required."
fi
if grep -Eiq "testOnly[[:space:]]*=[[:space:]]*['\"]?true|testOnly.*0xffffffff" <<<"$MANIFEST_TEXT"; then
  fail "Manifest contains testOnly; do not distribute a test-only APK."
fi
if grep -Eiq "debuggable[[:space:]]*=[[:space:]]*['\"]?true|debuggable.*0xffffffff" <<<"$MANIFEST_TEXT"; then
  fail "Release APK is debuggable; refuse distribution."
fi

printf '\nAPK structural, signature, alignment, package identity and release-manifest checks passed.\n'
