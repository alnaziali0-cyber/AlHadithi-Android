#!/usr/bin/env python3
"""Static guardrails for AL-HADITHI's foreground-only, one-shot location permission flow."""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID_NS = "{http://schemas.android.com/apk/res/android}"

def fail(message: str) -> None:
    raise SystemExit(f"FAIL: {message}")

manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
permissions = {
    node.attrib.get(ANDROID_NS + "name")
    for node in manifest.findall("uses-permission")
}
if "android.permission.ACCESS_FINE_LOCATION" not in permissions:
    fail("ACCESS_FINE_LOCATION missing from manifest")
if "android.permission.ACCESS_COARSE_LOCATION" not in permissions:
    fail("ACCESS_COARSE_LOCATION missing from manifest")
if "android.permission.ACCESS_BACKGROUND_LOCATION" in permissions:
    fail("background location permission must not be declared")

source_root = ROOT / "app/src/main/java"
kotlin = "\n".join(path.read_text(encoding="utf-8") for path in source_root.rglob("*.kt"))
if kotlin.count("ActivityResultContracts.RequestMultiplePermissions()") != 1:
    fail("location runtime prompt must have exactly one centralized RequestMultiplePermissions launcher")
if "getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY" not in kotlin:
    fail("high-accuracy current-location request missing")
if ".lastLocation" in kotlin:
    fail("stale lastLocation access remains in source")
if kotlin.count("rememberPreciseDeviceLocationAction(") < 4:
    fail("shared location action must be used by selector, prayer screen and qibla screen")
if "MAX_ACCEPTED_ACCURACY_METERS = 100f" not in kotlin:
    fail("100m accuracy threshold missing")
if "MAX_FIX_AGE_MILLIS = 120_000L" not in kotlin:
    fail("120s freshness threshold missing")

backup = ET.parse(ROOT / "app/src/main/res/xml/backup_rules.xml").getroot()
extract = ET.parse(ROOT / "app/src/main/res/xml/data_extraction_rules.xml").getroot()
marker = "alhadithi_location_permission_v1.xml"
if not any(node.attrib.get("path") == marker for node in backup.findall("exclude")):
    fail("location one-shot marker must be excluded from legacy backup")
if not any(node.attrib.get("path") == marker for node in extract.findall(".//exclude")):
    fail("location one-shot marker must be excluded from cloud/device transfer")

policy_test = ROOT / "app/src/test/java/com/example/domain/LocationPermissionPolicyTest.kt"
if not policy_test.is_file():
    fail("LocationPermissionPolicyTest missing")

print("Location access static guardrails: 12/12 checks passed")
