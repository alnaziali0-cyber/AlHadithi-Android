#!/usr/bin/env python3
"""Static release preflight for AL-HADITHI without requiring an Android SDK."""
from __future__ import annotations

import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP_ID = "com.aistudio.alhadithi.iqshia"
ANDROID = "{http://schemas.android.com/apk/res/android}"
errors: list[str] = []
checks: list[str] = []

def ok(message: str) -> None:
    checks.append(message)

def require(condition: bool, message: str) -> None:
    if condition:
        ok(message)
    else:
        errors.append(message)

build = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
services = json.loads((ROOT / "app/google-services.json").read_text(encoding="utf-8"))

require(f'applicationId = "{APP_ID}"' in build, "applicationId preserved")
require('namespace = "com.example"' in build, "Kotlin namespace preserved")
require("versionCode = 19" in build and 'versionName = "1.0.17"' in build, "version is 1.0.17 (19)")
require("minSdk = 24" in build and "targetSdk = 36" in build, "SDK constraints remain explicit")
require("verifyReleaseSigningInputs" in build, "Release signing fail-fast guard exists")
require("isDebuggable = false" in build, "Release debuggable is explicitly false")
require("signingConfig = signingConfigs.getByName(\"release\")" in build, "Release uses supplied signing config")
require("abiFilters" not in build and "splits {" not in build, "no ABI split configuration (single APK output)")

app = manifest.find("application")
require(app is not None, "Manifest application element exists")
if app is not None:
    require(app.get(ANDROID + "icon") == "@mipmap/ic_launcher", "Manifest uses the launcher icon")
    require(app.get(ANDROID + "allowBackup") == "true", "existing backup behavior preserved")

client_packages = {
    c.get("client_info", {}).get("android_client_info", {}).get("package_name")
    for c in services.get("client", [])
}
require(APP_ID in client_packages, "Firebase Android package matches applicationId")
permissions = {node.get(ANDROID + "name") for node in manifest.findall("uses-permission")}
require("android.permission.INTERNET" in permissions, "Internet permission retained")
require("android.permission.ACCESS_FINE_LOCATION" in permissions, "Fine location permission retained")
require("android.permission.ACCESS_COARSE_LOCATION" in permissions, "Coarse location permission retained")
require("android.permission.ACCESS_BACKGROUND_LOCATION" not in permissions, "no background location tracking permission")

for f in [
    ROOT / "app/src/main/res/drawable/ic_launcher_background.xml",
    ROOT / "app/src/main/res/drawable/ic_launcher_foreground.xml",
    ROOT / "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml",
    ROOT / "app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml",
]:
    try:
        ET.parse(f)
        ok(f"XML parses: {f.relative_to(ROOT)}")
    except ET.ParseError as exc:
        errors.append(f"invalid XML {f.relative_to(ROOT)}: {exc}")

for f in [
    ROOT / "app/src/main/res/drawable-nodpi/alhadithi_launcher_mark.png",
    ROOT / "app/src/main/res/drawable-nodpi/alhadithi_launcher_monochrome.png",
    ROOT / "app/src/main/res/drawable/img_alhadithi_logo.png",
]:
    require(f.is_file() and f.stat().st_size > 1000, f"brand asset exists: {f.relative_to(ROOT)}")

settings = (ROOT / "app/src/main/java/com/example/ui/screens/SettingsScreen.kt").read_text(encoding="utf-8")
require('SettingsSection(title = "الخطوط وحجم النص")' in settings, "font settings are grouped in one card")
require('painterResource(id = R.drawable.img_alhadithi_logo_white)' not in settings, "About screen brand image removed")

auth = (ROOT / "app/src/main/java/com/example/data/remote/DeveloperAuthAndFirestoreRepository.kt").read_text(encoding="utf-8")
require("AUTHORIZED_DEVELOPER_EMAIL" in auth and "signInWithEmailAndPassword" in auth, "developer email/password authentication path exists")
# Reject obvious literal password assignments in source/build settings. Firebase client ids and API keys are not passwords.
secret_scan = "\n".join(
    p.read_text(encoding="utf-8", errors="ignore")
    for folder in (ROOT / "app/src/main/java", ROOT / "app/src/main/res/values")
    for p in folder.rglob("*") if p.is_file() and p.suffix in {".kt", ".xml"}
)
require(not re.search(r'(?i)(password|passwd)\s*=\s*"[^"\n]{8,}"', secret_scan), "no hard-coded password literal detected")

keystores = list(ROOT.rglob("*.jks")) + list(ROOT.rglob("*.keystore"))
require(not keystores, "no private signing keystore is bundled")

print(f"AL-HADITHI static preflight: {len(checks)} checks passed, {len(errors)} failed")
for item in checks:
    print(f"PASS  {item}")
for item in errors:
    print(f"FAIL  {item}", file=sys.stderr)
sys.exit(1 if errors else 0)
