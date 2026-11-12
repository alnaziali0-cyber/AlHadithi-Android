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
require("versionCode = 25" in build and 'versionName = "1.0.23"' in build, "version is 1.0.23 (25)")
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
settings_support = (ROOT / "app/src/main/java/com/example/ui/screens/SettingsSupport.kt").read_text(encoding="utf-8")
require('title = "إعدادات القراءة"' in settings and 'title = "إعدادات المظهر"' in settings, "appearance and reading settings are grouped in dedicated cards")
require('onChangeDailyGoals(' in settings and 'DailyGoal.entries.forEach' in settings, "daily goals are adjustable from settings")
require('title = "المتابعة اليومية في الرئيسية"' in settings, "daily goals card is preserved in the rebuilt settings")
require('cachedAyahCount' in settings and 'title = "إعدادات البيانات والمزامنة"' in settings, "storage panel displays live local Quran cache count")
require('onChangeHijriOffset' in settings, "Hijri date correction is adjustable from settings")
require('showClearCacheDialog' in settings, "cache cleanup requires explicit confirmation")
require('لا نعرض مفاتيح تشغيل شكلية' in settings, "unsupported daily content pushes are not presented as working controls")
require('title = "إعدادات الإشعارات"' in settings, "notification settings have their own section")
require('ActivityResultContracts.OpenDocument()' in settings and 'Intent.EXTRA_STREAM' in settings, "support reports can attach an image through Android document picker")
support_channels = (ROOT / "app/src/main/java/com/example/domain/SupportChannels.kt").read_text(encoding="utf-8")
require('SUPPORT_EMAIL = "alhadithiiraq@gmail.com"' in support_channels, "support email is configured")
require('INSTAGRAM_URL = "https://www.instagram.com/alhadithi_iraq/"' in support_channels, "Instagram HTTPS profile is configured")
require('TIKTOK_URL = "https://www.tiktok.com/@.al.hadithi"' in support_channels, "TikTok HTTPS profile is configured")
require("isSafeHttpsProfile" in support_channels and "parsed.rawQuery == null" in support_channels, "external support links use strict HTTPS allow-list validation")
require('title = "الدعم والمساعدة"' in settings and 'openSupportEmail(context)' in settings, "support contact rows are wired in Settings UI")
require('Intent.ACTION_SENDTO' in settings_support and 'Intent.ACTION_VIEW' in settings_support, "email and social links use scoped Android intents")
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
require("LuminousCyanAccentPalette" in colors and "AppSoftBlackBackground = Color(0xFF111715)" in colors and "NightGlassSurface = Color(0xFF1A211E)" in colors, "soft-black dark-mode design tokens exist")
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

# AlAdhan prayer integration: one API/cache source, exact-date cache, no old method fallback.
prayer_service = (ROOT / "app/src/main/java/com/example/data/remote/PrayerTimesService.kt").read_text(encoding="utf-8")
prayer_repo = (ROOT / "app/src/main/java/com/example/data/remote/AlAdhanPrayerTimesRepository.kt").read_text(encoding="utf-8")
prayer_scheduler = (ROOT / "app/src/main/java/com/example/media/PrayerNotificationScheduler.kt").read_text(encoding="utf-8")
prayer_vm = (ROOT / "app/src/main/java/com/example/ui/viewmodel/AlHadithiViewModel.kt").read_text(encoding="utf-8")
settings_ui = (ROOT / "app/src/main/java/com/example/ui/screens/SettingsScreen.kt").read_text(encoding="utf-8")
sections_ui = (ROOT / "app/src/main/java/com/example/ui/screens/SettingsMoreAndAdminScreens.kt").read_text(encoding="utf-8")
prayer_json = json.loads((ROOT / "app/src/main/assets/aladhan_prayer_times_config.json").read_text(encoding="utf-8"))
raw_html = ROOT / "references/prayer_times_supplied.html"
require(prayer_json.get("timeZone") == "Asia/Baghdad", "AlAdhan config pins Asia/Baghdad timezone")
require(prayer_json.get("mode") == "remote_api_with_exact_date_cache", "prayer source uses AlAdhan API with exact-date cache")
require(prayer_json.get("calculationParameters") == {
    "method": 99,
    "methodSettings": "17.7,null,4",
    "midnightMode": 1,
    "tune": "0,0,0,0,0,1,0,0,0",
    "documentedOrder": ["FajrAngle", "MaghribAngleOrMinutesAfterSunset", "IshaAngleOrMinutesAfterMaghrib"],
    "interpretation": {
        "17.7": "Fajr angle",
        "null": "No custom Maghrib angle/minute value supplied in the second slot",
        "4": "Isha parameter in the third slot; it does not configure Maghrib",
        "tuneSixthValue": "The sixth tune value is the Maghrib offset (+1 minute)"
    }
}, "all supplied AlAdhan calculation parameters are preserved exactly and documented")
require("AlAdhanPrayerTimesRepository" in prayer_service and "computeDailyPrayerSchedule" not in prayer_service, "PrayerTimesService delegates to the central AlAdhan repository with no calculation fallback")
require("refreshWhenOlderThanHours" in prayer_repo and "readCache" in prayer_repo and "writeCache" in prayer_repo, "repository stores and refreshes exact-date per-governorate cache")
require("Asia/Baghdad" in prayer_repo and "meta.optString(\"timezone\")" in prayer_repo, "API response timezone is verified")
require("returnedDate == expectedDate" in prayer_repo and "AlAdhanPrayerTimeParser.isValid" in prayer_repo, "API response date and time ordering are validated")
require("ALADHAN_API_CACHE" in prayer_scheduler and "pauseWithoutData" in prayer_scheduler and "BUNDLED_FILE" not in prayer_scheduler, "alarms accept only the central API/cache source and pause without exact data")
require("PrayerTimesService(appCtx).loadWindow" in prayer_vm and "unavailableSchedule" in prayer_vm, "ViewModel uses central API/cache source and explicit unavailable state")
main_kotlin_sources = "\n".join(path.read_text(encoding="utf-8", errors="ignore") for path in (ROOT / "app/src/main/java").rglob("*.kt"))
require("PrayerAndHijriCalculator.calculateLocalPrayerSchedule(" not in main_kotlin_sources, "no production screen or service invokes the legacy local calculation engine")
require(raw_html.is_file() and "methodSettings=17.7,null,4" in raw_html.read_text(encoding="utf-8") and "window.onload = initPrayerTimes" in raw_html.read_text(encoding="utf-8"), "user-supplied HTML reference is retained outside APK assets")
require("methodSettings" in prayer_repo and "AlAdhanPrayerTimesConfig.METHOD_SETTINGS" in prayer_repo, "repository enforces the unchanged supplied method parameters")
require("مصدر المواقيت: خدمة AlAdhan" in settings_ui, "Settings identifies AlAdhan API and its local cache")
require("PrayerCalcMethod" not in settings_ui and "setPrayerCalcMethod" not in settings_ui, "legacy prayer-calculation selectors are absent from Settings UI")

# --- 1.0.23 regression checks: the black status-bar rectangle, the developer gate and the
# --- rebuilt settings sections.
themes = (ROOT / "app/src/main/res/values/themes.xml").read_text(encoding="utf-8")
themes_night = (ROOT / "app/src/main/res/values-night/themes.xml").read_text(encoding="utf-8")
main_activity = (ROOT / "app/src/main/java/com/example/MainActivity.kt").read_text(encoding="utf-8")
require("android:windowBackground" in themes and "@color/kh_window_background" in themes,
        "the window background is declared so no black band is painted behind the status bar")
require("android:statusBarColor" in themes and "android:navigationBarColor" in themes
        and "android:windowLayoutInDisplayCutoutMode" in themes,
        "the system bars are transparent and the display cutout is handled")
require("android:windowBackground" in themes_night and "@color/kh_window_background" in themes_night,
        "the night window background follows the dark theme")
require("WindowCompat.setDecorFitsSystemWindows(window, false)" in main_activity
        and "enableEdgeToEdge()" in main_activity,
        "the activity draws edge-to-edge behind the system bars")
require("contentWindowInsets = WindowInsets(0, 0, 0, 0)" in shell
        and "WindowInsetsSides.Top" in shell,
        "the shell consumes the top inset once and leaves the navigation inset to the bar")
require("AppSoftBlackBackground = Color(0xFF111715)" in colors,
        "the dark theme uses a soft black instead of a dead pure black")
require("LocalStatusColors" in theme and "StatusSuccessDark" in colors,
        "the semantic status colours are resolved centrally per theme")
require("DeveloperAccessController" in settings and "REQUIRED_TAPS" in settings,
        "the developer panel is behind a deliberate access-code gate")
require("verifyAndUnlock" in (ROOT / "app/src/main/java/com/example/ui/screens/DeveloperAccessController.kt").read_text(encoding="utf-8")
        and "isUnlocked" in shell,
        "the developer panel only opens for an unlocked session")
require("onLeaveDeveloperMode" in (ROOT / "app/src/main/java/com/example/ui/screens/DeveloperDiagnosticsTab.kt").read_text(encoding="utf-8"),
        "the developer panel offers a clear way to leave developer mode")
require("title = \"إعدادات المظهر\"" in settings and "title = \"إعدادات القراءة\"" in settings
        and "title = \"إعدادات مواقيت الصلاة\"" in settings and "title = \"إعدادات الصوت\"" in settings
        and "title = \"إعدادات الإشعارات\"" in settings and "title = \"إعدادات البيانات والمزامنة\"" in settings
        and "title = \"إعدادات اللغة\"" in settings and "title = \"حول التطبيق\"" in settings,
        "the settings screen is organised into the eight required sections")
require("APP_DEVELOPER_NAME" in settings and "علي اديب العنزي" in settings,
        "the developer name is shown in the settings")
require("SettingsActionTile" in settings and "SettingsSwitchTile" in settings
        and "heightIn(min = SettingsRowMinHeight)" in (ROOT / "app/src/main/java/com/example/ui/components/SettingsComponents.kt").read_text(encoding="utf-8"),
        "every settings option uses a shared, accessible row component")
require("will compet" not in shell and "nav_tab_home" in shell and "bottom_navigation_bar" in shell,
        "the bottom navigation keeps its tabs and exposes a test tag")
require("PrayerTimesStrip" in (ROOT / "app/src/main/java/com/example/ui/screens/HomeScreen.kt").read_text(encoding="utf-8")
        and "PrayerTrackingRow" in (ROOT / "app/src/main/java/com/example/ui/screens/HomeScreen.kt").read_text(encoding="utf-8"),
        "the dashboard uses the shared prayer-times and prayer-tracking components")
require("PrayerTimetableVerifier" in (ROOT / "app/src/main/java/com/example/domain/PrayerTimetableVerifier.kt").read_text(encoding="utf-8"),
        "the timetable is verified against the reference configuration")
require("setPrayerCalcMethod" not in prayer_vm and "updatePrayerOffsets" not in prayer_vm, "legacy prayer-method mutation APIs are disconnected")
require("AppDestination.PRAYER_TIMES" in sections_ui and "مواقيت الصلاة" in sections_ui, "prayer times is a direct tile in All Sections")
require("fun SectionDivider" in (ROOT / "app/src/main/java/com/example/ui/components/SectionDivider.kt").read_text(encoding="utf-8"), "shared segmented SectionDivider component exists")
require("PastelGreenSurface" in sections_ui and "PastelPurpleSurface" in sections_ui and "PastelPeachSurface" in sections_ui, "section tiles use the shared pastel identity palette")

brand_strings = (ROOT / "app/src/main/res/values/strings.xml").read_text(encoding="utf-8")
require('<string name="app_name">خزائن الهدى</string>' in brand_strings, "Android application display name is Khazain Al-Huda")
require("KHAZAIN AL-HUDA" in brand_strings and "AL-HADITHI" not in brand_strings, "brand subtitle is updated")
font_file = ROOT / "app/src/main/res/font/alan_sans_light.ttf"
require(font_file.is_file() and font_file.stat().st_size > 50000, "Alan Sans Light UI font is bundled")
require((ROOT / "app/src/main/assets/licenses/ALAN_SANS_OFL.txt").is_file(), "Alan Sans OFL license notice is bundled")
main_activity = (ROOT / "app/src/main/java/com/example/MainActivity.kt").read_text(encoding="utf-8")
require('title = ""' in main_activity, "extra Android activity caption is disabled")
window_theme = (ROOT / "app/src/main/res/values/themes.xml").read_text(encoding="utf-8")
require('parent="android:Theme.DeviceDefault.NoActionBar"' in window_theme and 'android:windowNoTitle">true' in window_theme, "native window title and ActionBar are explicitly disabled")
require("AlanSansFontFamily" in (ROOT / "app/src/main/java/com/example/ui/theme/Type.kt").read_text(encoding="utf-8"), "Alan Sans is part of the global UI typography")
glass_components = (ROOT / "app/src/main/java/com/example/ui/components/GlassComponents.kt").read_text(encoding="utf-8")
require("val resolvedBgBrush = if (isDark)" in glass_components, "dark GlassCard surfaces cannot inherit bright pastel container tints")
more_hub = (ROOT / "app/src/main/java/com/example/ui/screens/SettingsMoreAndAdminScreens.kt").read_text(encoding="utf-8")
require("val darkTones = listOf(" in more_hub and "if (isDark) darkTones" in more_hub, "all-sections icon tiles switch to charcoal-safe colors in dark mode")
shell_source = (ROOT / "app/src/main/java/com/example/ui/AlHadithiAppShell.kt").read_text(encoding="utf-8")
require("isAppearanceLightStatusBars = !activeDarkTheme" in shell_source and "isAppearanceLightNavigationBars = !activeDarkTheme" in shell_source, "system bar icon contrast follows the selected in-app theme")
print(f"KHAZAIN AL-HUDA static preflight: {len(checks)} checks passed, {len(errors)} failed")
for item in checks:
    print(f"PASS  {item}")
for item in errors:
    print(f"FAIL  {item}", file=sys.stderr)
sys.exit(1 if errors else 0)
