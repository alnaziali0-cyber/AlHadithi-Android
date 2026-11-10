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
require("versionCode = 20" in build and 'versionName = "1.0.18"' in build, "version is 1.0.18 (20)")
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
require('SettingsSection(title = "اللغة والخطوط وحجم النص")' in settings, "language, font and text scale settings are grouped in one card")
require('onChangeDailyGoals(' in settings and 'DailyGoal.entries.forEach' in settings, "daily goals are adjustable from settings")
require('cachedAyahCount' in settings and 'SettingsSection(title = "التنزيلات والتخزين")' in settings, "storage panel displays live local Quran cache count")
require('onChangeHijriOffset' in settings, "Hijri date correction is adjustable from settings")
require('showClearCacheDialog' in settings, "cache cleanup requires explicit confirmation")
require('لا نعرض مفاتيح تشغيل شكلية' in settings, "unsupported daily content pushes are not presented as working controls")
require('ActivityResultContracts.OpenDocument()' in settings and 'Intent.EXTRA_STREAM' in settings, "support reports can attach an image through Android document picker")
dao = (ROOT / "app/src/main/java/com/example/data/local/AlHadithiDao.kt").read_text(encoding="utf-8")
view_model = (ROOT / "app/src/main/java/com/example/ui/viewmodel/AlHadithiViewModel.kt").read_text(encoding="utf-8")
shell = (ROOT / "app/src/main/java/com/example/ui/AlHadithiAppShell.kt").read_text(encoding="utf-8")
theme = (ROOT / "app/src/main/java/com/example/ui/theme/Theme.kt").read_text(encoding="utf-8")
colors = (ROOT / "app/src/main/java/com/example/ui/theme/Color.kt").read_text(encoding="utf-8")
repo_tests = (ROOT / "app/src/test/java/com/example/data/local/UserPreferencesRepositoryTest.kt").read_text(encoding="utf-8")
require("observeCachedAyahCount(): Flow<Int>" in dao, "Room exposes a live local Quran cache count")
require("dao.observeCachedAyahCount()" in view_model and "cachedAyahCount = cachedAyahCount" in shell, "cached Quran count reaches SettingsScreen through ViewModel")
require("ThemeMode.SYSTEM -> darkTheme" in theme and "darkTheme = isSystemInDarkTheme()" in (ROOT / "app/src/main/java/com/example/ui/AlHadithiAppShell.kt").read_text(encoding="utf-8"), "system theme follows actual Android system mode")
require("darkMode = LocalIsDarkThemeActive.current" in shell and "darkMode = settings.darkMode" not in shell, "Quran reader uses resolved theme state rather than legacy dark flag")
require("LuminousCyanAccentPalette" in colors and "NightSanctuaryBg" in colors, "dark navy and luminous cyan design tokens exist")
require("font reset preserves daily goals" in repo_tests and "resetting all preferences preserves user progress" in repo_tests, "preference reset regression tests are present")
require("onChangeHijriOffset" in shell and "onChangeHijriOffset" in settings, "Hijri offset setting is connected from UI to ViewModel")
require("onChangeHomeLayout" not in settings, "fixed home layout exposes no inert reorder callback")
require('painterResource(id = R.drawable.img_alhadithi_logo_white)' not in settings and 'painterResource(R.drawable.img_alhadithi_logo)' in settings, "About screen uses the current bundled brand mark (not the obsolete white variant)")

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
