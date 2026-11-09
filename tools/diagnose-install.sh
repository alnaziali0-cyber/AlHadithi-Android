#!/usr/bin/env bash
set -Eeuo pipefail

APP_ID="com.aistudio.alhadithi.iqshia"
APK="${1:-app/build/outputs/apk/release/app-release.apk}"
ADB="${ADB:-adb}"
SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
fail() { printf 'ERROR: %s\n' "$*" >&2; exit 2; }
command -v "$ADB" >/dev/null 2>&1 || fail "adb is unavailable; install Android Platform Tools"
[[ -f "$APK" ]] || fail "APK file not found: $APK"

ADB_ARGS=()
if [[ -n "${ANDROID_SERIAL:-}" ]]; then ADB_ARGS=(-s "$ANDROID_SERIAL"); fi
DEVICE_LINES="$($ADB devices | awk 'NR>1 && $2=="device" {print $1}')"
if [[ -z "$DEVICE_LINES" ]]; then fail "No authorized Android device connected. Enable USB debugging and authorize this computer."; fi
if [[ -z "${ANDROID_SERIAL:-}" && "$(wc -l <<<"$DEVICE_LINES" | tr -d ' ')" != "1" ]]; then
  fail "More than one device is connected; set ANDROID_SERIAL to the intended device serial."
fi

run_adb() { "$ADB" "${ADB_ARGS[@]}" "$@"; }

printf 'Target package: %s\n' "$APP_ID"
printf 'APK: %s\n' "$APK"
sha256sum "$APK"
printf '\n== Device ==\n'
printf 'Android release: '; run_adb shell getprop ro.build.version.release | tr -d '\r'
printf 'SDK level: '; run_adb shell getprop ro.build.version.sdk | tr -d '\r'
printf 'Supported ABIs: '; run_adb shell getprop ro.product.cpu.abilist | tr -d '\r'
printf '\n== Incoming APK certificate (read-only) ==\n'
APKSIGNER=""
if [[ -n "$SDK_ROOT" && -d "$SDK_ROOT/build-tools" ]]; then
  APKSIGNER="$(find "$SDK_ROOT/build-tools" -type f -name apksigner -print 2>/dev/null | sort -V | tail -n1)"
fi
if [[ -z "$APKSIGNER" ]] && command -v apksigner >/dev/null 2>&1; then APKSIGNER="$(command -v apksigner)"; fi
INCOMING_CERT=""
if [[ -n "$APKSIGNER" ]]; then
  INCOMING_CERT="$("$APKSIGNER" verify --print-certs "$APK" 2>&1 | grep -m1 'certificate SHA-256 digest:' || true)"
  printf '%s\n' "${INCOMING_CERT:-Unable to read incoming APK signer certificate}"
else
  printf 'apksigner unavailable; certificate comparison skipped.\n'
fi

printf '\n== Existing installation (read-only) ==\n'
EXISTING_PATHS="$(run_adb shell pm path "$APP_ID" 2>/dev/null | tr -d '\r' || true)"
if [[ -n "$EXISTING_PATHS" ]]; then
  run_adb shell dumpsys package "$APP_ID" | grep -E 'versionCode=|versionName=|firstInstallTime|lastUpdateTime|signatures=|SigningInfo' | head -30 || true
  BASE_DEVICE_PATH="$(head -n1 <<<"$EXISTING_PATHS" | sed 's/^package://')"
  if [[ -n "$BASE_DEVICE_PATH" && -n "$APKSIGNER" ]]; then
    TEMP_DIR="$(mktemp -d)"
    trap 'rm -rf "$TEMP_DIR"' EXIT
    if run_adb pull "$BASE_DEVICE_PATH" "$TEMP_DIR/installed-base.apk" >/dev/null 2>&1; then
      INSTALLED_CERT="$("$APKSIGNER" verify --print-certs "$TEMP_DIR/installed-base.apk" 2>&1 | grep -m1 'certificate SHA-256 digest:' || true)"
      printf 'Installed APK: %s\n' "${INSTALLED_CERT:-Unable to read installed APK signer certificate}"
      if [[ -n "$INCOMING_CERT" && -n "$INSTALLED_CERT" ]]; then
        if [[ "$INCOMING_CERT" == "$INSTALLED_CERT" ]]; then
          printf 'SIGNATURE RESULT: certificates match.\n'
        else
          printf 'SIGNATURE RESULT: CERTIFICATES DIFFER — update will normally be rejected.\n' >&2
        fi
      fi
    else
      printf 'Could not pull the installed base APK; no signature comparison was possible.\n'
    fi
  fi
else
  printf 'No existing install of %s was found.\n' "$APP_ID"
fi

printf '\n== Update install test (does not uninstall or clear app data) ==\n'
set +e
INSTALL_OUTPUT="$(run_adb install -r "$APK" 2>&1)"
INSTALL_STATUS=$?
set -e
printf '%s\n' "$INSTALL_OUTPUT"
printf '\n== Relevant Package Manager log lines ==\n'
run_adb logcat -d -t 800 -v brief 2>/dev/null | grep -E 'PackageManager|PackageInstaller|INSTALL_FAILED|INSTALL_PARSE_FAILED|PackageInstallerSession' | tail -80 || true

if [[ "$INSTALL_STATUS" -ne 0 ]]; then
  printf '\nInstallation failed with exit status %s. Preserve the exact INSTALL_FAILED_* code above for diagnosis.\n' "$INSTALL_STATUS" >&2
  exit "$INSTALL_STATUS"
fi
printf '\nADB install -r reported success. App data was not cleared by this command.\n'
