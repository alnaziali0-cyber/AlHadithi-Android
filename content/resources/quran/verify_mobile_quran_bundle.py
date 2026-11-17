#!/usr/bin/env python3
"""Audit the bundled Quran JSON files and emit Arabic QA/SHA-256 reports."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
from pathlib import Path
from typing import Any

EXPECTED_TEXT_SHA256 = "703246a73a0a3d57f721b02e3edad9a2ee6831b79f49ffb9513ddfe99a65b801"
EXPECTED_QURAN_JSON_REVISION = "6523a9ad3b73bb7e8cb3a9cdf0ea774b8bab3123"
BUNDLE_JSON = [
    "quran.json",
    "surahs.json",
    "ayahs.json",
    "juz.json",
    "hizb.json",
    "sajdah.json",
    "bookmarks.json",
    "quran_sources.json",
    "quran_attribution.json",
]


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def read_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def audit(project: Path, source_repo: Path | None) -> tuple[list[str], list[str]]:
    app = project / "apps" / "mobile"
    bundle = app / "assets" / "data" / "quran"
    checks: list[str] = []
    warnings: list[str] = []

    manifest_path = bundle / "quran.json"
    manifest_bytes = manifest_path.read_bytes()
    manifest = json.loads(manifest_bytes.decode("utf-8"))
    manifest_hash = sha256(manifest_bytes)
    repo_code = (app / "lib" / "core" / "quran_text_repository.dart").read_text(encoding="utf-8")
    pin = re.search(r"quranManifestSha256\s*=\s*'([0-9a-f]{64})'", repo_code)
    require(pin is not None and pin.group(1) == manifest_hash, "Dart manifest pin does not match quran.json.")
    checks.append(f"بصمة بيان الحزمة المثبتة في Dart مطابقة: {manifest_hash}")

    for name in BUNDLE_JSON[1:]:
        path = bundle / name
        data = path.read_bytes()
        expected = manifest["integrity"].get(name)
        require(expected == sha256(data), f"SHA-256 mismatch: {name}")
        json.loads(data.decode("utf-8"))
    for name, expected in manifest["integrity"].items():
        file_path = bundle / name
        require(file_path.is_file(), f"Manifest references missing file: {name}")
        require(sha256(file_path.read_bytes()) == expected, f"Manifest SHA-256 mismatch: {name}")
    checks.append("كل ملفات البيانات والإشعارات المدرجة في بيان الحزمة تطابق SHA-256")

    ayah_bytes = (bundle / "ayahs.json").read_bytes()
    ayahs = json.loads(ayah_bytes.decode("utf-8"))
    surahs = read_json(bundle / "surahs.json")["items"]
    require(sha256(ayah_bytes) == EXPECTED_TEXT_SHA256, "Bundled text hash differs from the pinned source.")
    require(sha256(ayah_bytes) == manifest["text_sha256"] == manifest["text_source_sha256"], "Text hashes disagree.")
    require(len(surahs) == 114 and len(ayahs) == 114, "Expected 114 surahs.")
    require(list(ayahs.keys()) == [str(number) for number in range(1, 115)], "Surah keys are not ordered 1-114.")
    count = 0
    for number, surah in enumerate(surahs, start=1):
        rows = ayahs[str(number)]
        require(surah["number"] == number, f"Surah index out of order at {number}.")
        require(surah["ayah_count"] == len(rows), f"Ayah count mismatch for surah {number}.")
        for ayah_number, row in enumerate(rows, start=1):
            require(row["chapter"] == number and row["verse"] == ayah_number, f"Bad verse number at {number}:{ayah_number}.")
            require(isinstance(row["text"], str) and row["text"].strip(), f"Empty ayah at {number}:{ayah_number}.")
            count += 1
    require(count == 6236 == manifest["counts"]["ayahs"], f"Expected 6,236 ayahs, found {count}.")
    require(manifest["reading"]["id"] == "hafs" and manifest["script"]["id"] == "simple", "Wrong reading or script metadata.")
    checks.append("114 سورة و6,236 آية متسلسلة؛ النص الكامل يطابق بصمة لقطة Tanzil المثبتة")

    if source_repo is not None:
        source_repo = source_repo.resolve()
        revision = subprocess.check_output(["git", "-C", str(source_repo), "rev-parse", "HEAD"], text=True).strip()
        require(revision == EXPECTED_QURAN_JSON_REVISION, "quran-json source commit is not the reviewed revision.")
        source_path = source_repo / "data" / "tanzil" / "simple.json"
        require(ayah_bytes == source_path.read_bytes(), "Bundled ayahs.json is not byte-identical to source simple.json.")
        source_catalog = read_json(source_repo / "data" / "meta" / "sources.json")
        record = next(row for row in source_catalog if row.get("path") == "data/tanzil/simple.json")
        require(sha256(source_path.read_bytes()) == record["sha256"] == EXPECTED_TEXT_SHA256, "Tanzil source-catalog hash mismatch.")
        checks.append(f"ayahs.json مطابق بايتاً لملف المصدر عند commit {revision}")
    else:
        warnings.append("لم يُمرَّر مستودع quran-json؛ جرى التحقق من بصمة اللقطة المثبتة فقط، لا من الملف المصدر في هذا التشغيل.")

    juz = read_json(bundle / "juz.json")["items"]
    hizb_data = read_json(bundle / "hizb.json")
    hizbs = hizb_data["items"]
    quarters = hizb_data["quarters"]
    sajdahs = read_json(bundle / "sajdah.json")["items"]
    require(len(juz) == 30 and len(hizbs) == 60 and len(quarters) == 240 and len(sajdahs) == 15, "Index cardinalities differ.")
    for collection, label in ((juz, "juz"), (hizbs, "hizb"), (quarters, "quarter")):
        previous = (0, 0)
        for expected_number, item in enumerate(collection, start=1):
            require(item["number"] == expected_number, f"{label} index numbering is invalid.")
            position = item["start"]
            current = (position["surah"], position["ayah"])
            require(current > previous, f"{label} boundaries are not strictly increasing.")
            require(1 <= current[0] <= 114 and 1 <= current[1] <= len(ayahs[str(current[0])]), f"Invalid {label} reference {current}.")
            previous = current
    for item in sajdahs:
        position = item["position"]
        require(1 <= position["surah"] <= 114 and 1 <= position["ayah"] <= len(ayahs[str(position["surah"])]), "Sajdah position is out of range.")
    require(manifest["counts"]["hizb"] == 60 and manifest["counts"]["hizb_quarters"] == 240, "Hizb/quarter counts mislabeled.")
    checks.append("الفهارس: 30 جزءاً، 60 حزباً، 240 ربع حزب، 15 موضع سجدة؛ كل المراجع ضمن حدود السور")

    source_data = read_json(bundle / "quran_sources.json")
    attribution = read_json(bundle / "quran_attribution.json")
    require(any(item.get("source_id") == "tanzil_simple_text" for item in source_data["items"]), "Missing Tanzil text source.")
    require(any(item.get("source_id") == "hafs_reading_chain_reference" for item in source_data["items"]), "Missing sanad citation in source catalog.")
    require(attribution["reading_sanad"]["reference"]["url"] == "https://www.islamweb.net/ar/fatwa/100241/", "Reading-chain source is missing.")
    require(attribution["institutional_attribution"]["marja_or_hawza"] is None, "Institutional attribution must remain absent.")
    require(attribution["audio"]["bundled_in_app"] is False and manifest["audio"]["bundled_in_app"] is False, "Audio must not be prebundled.")
    require(attribution["audio"]["reading"] == "hafs" and attribution["audio"]["reciter_id"] == "ar.alafasy", "Audio metadata is inconsistent.")
    audio_files = list((app / "assets").rglob("*.mp3"))
    require(not audio_files, "Audio files were unexpectedly bundled in the app assets.")
    checks.append("مصدر النص وسند القراءة والصوت موثّقون منفصلين؛ لا يوجد ادعاء مؤسسي ولا صوت مضمّن")

    font_path = app / "assets" / "fonts" / "AmiriQuran.ttf"
    font_license = app / "assets" / "fonts" / "AMIRI_OFL_LICENSE.txt"
    require(font_path.is_file() and font_license.is_file(), "Local Quran font or its license is missing.")
    all_chars = {ord(char) for rows in ayahs.values() for row in rows for char in row["text"] if not char.isspace()}
    try:
        from fontTools.ttLib import TTFont

        font = TTFont(font_path)
        cmap = set().union(*(set(table.cmap) for table in font["cmap"].tables))
        missing = sorted(all_chars - cmap)
        require(not missing, "Amiri Quran lacks glyphs for: " + ", ".join(f"U+{code:04X}" for code in missing))
        checks.append(f"خط Amiri Quran يغطي كل {len(all_chars)} محرفاً مميزاً في نص الآيات؛ رخصة OFL موجودة")
    except ImportError:
        warnings.append("fontTools غير مثبت؛ فحص تغطية Unicode للخط لم يُنفّذ في هذا التشغيل.")

    pubspec = (app / "pubspec.yaml").read_text(encoding="utf-8")
    require("- assets/data/quran/" in pubspec, "The Quran bundle is not registered as a Flutter asset directory.")
    require("family: AmiriQuran" in pubspec, "Amiri Quran font is not registered in pubspec.yaml.")
    require("quran-hafs-rasm_uthmani.json" not in pubspec, "Legacy Uthmani asset is still registered in the app bundle.")
    checks.append("Flutter يضمّن الحزمة والخط المحليين دون ملف المصحف القديم المكرر")

    return checks, warnings


def create_reports(project: Path, checks: list[str], warnings: list[str]) -> None:
    app = project / "apps" / "mobile"
    bundle = app / "assets" / "data" / "quran"
    report_dir = project / "docs" / "reports"
    report_dir.mkdir(parents=True, exist_ok=True)

    files = [bundle / name for name in BUNDLE_JSON]
    files += [
        bundle / "QURAN_SOURCE_NOTICE_AR.md",
        bundle / "licenses" / "QURAN_JSON_CC-BY-SA-4.0.txt",
        app / "assets" / "fonts" / "AmiriQuran.ttf",
        app / "assets" / "fonts" / "AMIRI_OFL_LICENSE.txt",
    ]
    lines = ["SHA-256 checksums — Khazain Al-Huda Quran bundle", "Format: <sha256>  <path relative to project root>", ""]
    for path in files:
        require(path.is_file(), f"Cannot checksum missing file: {path}")
        lines.append(f"{sha256(path.read_bytes())}  {path.relative_to(project).as_posix()}")
    (report_dir / "QURAN_SHA256SUMS.txt").write_text("\n".join(lines) + "\n", encoding="utf-8")

    report = [
        "# تقرير تحقق حزمة المصحف المحلية",
        "",
        "## النتيجة",
        "",
        "اجتازت الملفات الفحوص الآلية المدرجة أدناه على لقطة البيانات المثبتة. هذا التقرير **لا يعلن اكتمال الاعتماد**: يلزم تدقيق قارئ مختص للنص وعلامات الوقف مقابل مصحف مطبوع، كما يلزم اختبار عملي على Android قبل اعتماد الإصدار.",
        "",
        "## فحوص اجتازت",
        "",
    ]
    report += [f"- {item}" for item in checks]
    report += [
        "",
        "## حدود لم تُحسم آلياً",
        "",
        "- فحوص البصمات والعدادات تثبت سلامة اللقطة الحالية والتسلسل البنيوي، ولا تثبت بذاتها مراجعة بصرية مستقلة للنص أو لكل علامة وقف.",
        "- مصدر سلسلة حفص عن عاصم مسجل منفصلاً عن مصدر النص، لكن يلزم تدقيق مختص لمرجع السند والطبعة قبل الاعتماد النهائي. لا يدعي التطبيق إجازة أداء أو نسبة إلى مرجعية/حوزة.",
        "- تغطية Unicode للخط فُحصت آلياً؛ تشكيل الحروف ومواضع العلامات يحتاجان مشاهدة فعلية على Android وأحجام شاشة متعددة.",
        "- التلاوة غير مضمنة. التنزيل الاختياري للسور من Islamic Network/AlQuran Cloud مقيد بالاستخدام الشخصي/التعليمي وبحقوق التسجيل؛ الاستخدام التجاري أو إعادة التوزيع غير معتمد هنا.",
    ]
    if warnings:
        report += ["", "## تحذيرات تشغيل هذا التقرير", ""]
        report += [f"- {item}" for item in warnings]
    report += [
        "",
        "## الملفات المرجعية",
        "",
        "- بيانات الحزمة: `apps/mobile/assets/data/quran/`.",
        "- إشعار المصادر والتراخيص: `apps/mobile/assets/data/quran/QURAN_SOURCE_NOTICE_AR.md`.",
        "- سلسلة القراءة المنفصلة: `apps/mobile/assets/data/quran/quran_attribution.json`.",
        "- البصمات: `docs/reports/QURAN_SHA256SUMS.txt`.",
        "- مولّد الحزمة والمدقق: `content/resources/quran/build_mobile_quran_bundle.py` و`content/resources/quran/verify_mobile_quran_bundle.py`.",
        "",
    ]
    (report_dir / "QURAN_VALIDATION_REPORT_AR.md").write_text("\n".join(report), encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", type=Path, default=Path(__file__).resolve().parents[3])
    parser.add_argument("--source-repo", type=Path, default=Path("/workspace/quran-json"))
    parser.add_argument("--skip-source-compare", action="store_true")
    args = parser.parse_args()
    checks, warnings = audit(args.project.resolve(), None if args.skip_source_compare else args.source_repo)
    create_reports(args.project.resolve(), checks, warnings)
    print("Automated Quran bundle audit passed.")
    for item in checks:
        print(f"PASS: {item}")
    for item in warnings:
        print(f"WARN: {item}")
    print(f"Reports: {args.project.resolve()}/docs/reports/QURAN_SHA256SUMS.txt")
    print(f"         {args.project.resolve()}/docs/reports/QURAN_VALIDATION_REPORT_AR.md")


if __name__ == "__main__":
    main()
