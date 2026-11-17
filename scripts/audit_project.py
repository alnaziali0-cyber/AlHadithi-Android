#!/usr/bin/env python3
"""Offline integrity and release-blocker audit for Khazain Al-Huda.

This script validates files that are present in the repository. It does not
claim that an external source is online, licensed, or approved for republication.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import re
import sys
from pathlib import Path
from zipfile import BadZipFile, ZipFile

ROOT = Path(__file__).resolve().parents[1]
AUDIO_EXTENSIONS = {".mp3", ".m4a", ".aac", ".ogg", ".wav", ".flac", ".opus"}


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def read_json(path: Path, errors: list[str]):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:  # report exact path and continue auditing
        errors.append(f"JSON غير صالح: {path.relative_to(ROOT)} — {exc}")
        return None


def audit_json_files() -> int:
    files = list(ROOT.rglob("*.json"))
    errors: list[str] = []
    for path in files:
        read_json(path, errors)
    for error in errors:
        print(f"ERROR: {error}")
    return len(files) - len(errors)


def audit_zip_files() -> tuple[int, int]:
    archives = list(ROOT.rglob("*.zip"))
    bad = 0
    for path in archives:
        try:
            with ZipFile(path) as archive:
                member = archive.testzip()
                if member:
                    bad += 1
                    print(f"ERROR: ZIP تالف: {path.relative_to(ROOT)} :: {member}")
        except (BadZipFile, OSError) as exc:
            bad += 1
            print(f"ERROR: ZIP غير قابل للقراءة: {path.relative_to(ROOT)} :: {exc}")
    return len(archives), bad


def audit_quran_bundle() -> tuple[int, int, list[str]]:
    errors: list[str] = []
    quran_dir = ROOT / "apps/mobile/assets/data/quran"
    manifest_path = quran_dir / "quran.json"
    manifest = read_json(manifest_path, errors)
    if not isinstance(manifest, dict):
        return 0, 0, errors
    integrity = manifest.get("integrity")
    if not isinstance(integrity, dict):
        errors.append("بيان المصحف لا يحتوي جدول integrity.")
        return 0, 0, errors
    verified = 0
    for relative, expected in integrity.items():
        target = quran_dir / relative
        if not target.is_file():
            errors.append(f"ملف مصحف مفقود: {target.relative_to(ROOT)}")
            continue
        actual = sha256_file(target)
        if actual != expected:
            errors.append(f"بصمة SHA-256 غير مطابقة: {target.relative_to(ROOT)}")
            continue
        verified += 1
    ayahs_path = quran_dir / "ayahs.json"
    try:
        ayahs = json.loads(ayahs_path.read_text(encoding="utf-8"))
        total = sum(len(rows) for rows in ayahs.values())
        if len(ayahs) != 114 or total != 6236:
            errors.append(f"بنية الآيات غير مطابقة: {len(ayahs)} سورة / {total} آية")
    except Exception as exc:
        errors.append(f"تعذر حساب بنية آيات المصحف: {exc}")
    expected_text_hash = manifest.get("text_sha256")
    if expected_text_hash and sha256_file(ayahs_path) != expected_text_hash:
        errors.append("بصمة متن القرآن في البيان لا تطابق الملف الفعلي.")
    return len(integrity), verified, errors


def audit_audio_catalog() -> tuple[int, int, list[str]]:
    errors: list[str] = []
    path = ROOT / "apps/mobile/assets/data/catalogs/audio_library_catalog.json"
    data = read_json(path, errors)
    if not isinstance(data, dict) or not isinstance(data.get("groups"), list):
        errors.append("فهرس الصوتيات لا يحتوي groups صحيحة.")
        return 0, 0, errors
    entries = 0
    for group in data["groups"]:
        if not isinstance(group, dict) or not isinstance(group.get("items"), list):
            errors.append("مجموعة غير صالحة في فهرس الصوتيات.")
            continue
        for item in group["items"]:
            entries += 1
            if not isinstance(item, dict):
                errors.append("سجل غير صالح في فهرس الصوتيات.")
                continue
            url = item.get("url")
            name = item.get("name")
            rights = item.get("rights_status")
            note = item.get("rights_note_ar")
            if not isinstance(name, str) or not name.strip():
                errors.append("سجل صوتي بلا اسم.")
            if not isinstance(url, str) or not url.startswith("https://"):
                errors.append(f"رابط غير HTTPS في فهرس الصوتيات: {name}")
            if not isinstance(rights, str) or not rights.strip():
                errors.append(f"حالة حقوق غير موثقة في فهرس الصوتيات: {name}")
            if not isinstance(note, str) or not note.strip():
                errors.append(f"تنبيه حقوق مفقود في فهرس الصوتيات: {name}")
    return len(data["groups"]), entries, errors


def audit_pubspec_assets() -> tuple[int, list[str]]:
    errors: list[str] = []
    pubspec = ROOT / "apps/mobile/pubspec.yaml"
    if not pubspec.is_file():
        return 0, ["ملف pubspec.yaml مفقود."]
    declared: set[str] = set()
    for line in pubspec.read_text(encoding="utf-8").splitlines():
        match = re.match(r"\s+-\s+((?:assets|fonts)/[^#\s]+)\s*$", line)
        if match:
            declared.add(match.group(1))
    for item in sorted(declared):
        target = ROOT / "apps/mobile" / item
        if not target.exists():
            errors.append(f"أصل معلن في pubspec غير موجود: {item}")
    return len(declared), errors


def audit_migrations() -> tuple[int, list[str]]:
    folder = ROOT / "services/api/alembic/versions"
    rows: dict[str, str | None] = {}
    errors: list[str] = []
    for path in sorted(folder.glob("*.py")):
        source = path.read_text(encoding="utf-8")
        revision = re.search(r'^revision\s*=\s*["\']([^"\']+)', source, re.M)
        down = re.search(r'^down_revision\s*=\s*(None|["\'][^"\']+["\'])', source, re.M)
        if not revision or not down:
            errors.append(f"تعريف migration ناقص: {path.name}")
            continue
        parent = None if down.group(1) == "None" else down.group(1)[1:-1]
        key = revision.group(1)
        if key in rows:
            errors.append(f"معرّف migration مكرر: {key}")
        rows[key] = parent
    roots = [key for key, parent in rows.items() if parent is None]
    if len(roots) != 1:
        errors.append(f"سلسلة migrations يجب أن تحتوي جذراً واحداً؛ الموجود {len(roots)}")
    for revision, parent in rows.items():
        if parent is not None and parent not in rows:
            errors.append(f"migration {revision} تشير إلى أب غير موجود: {parent}")
    children: dict[str, list[str]] = {}
    for revision, parent in rows.items():
        if parent is not None:
            children.setdefault(parent, []).append(revision)
    branches = {parent: child for parent, child in children.items() if len(child) > 1}
    if branches:
        errors.append(f"سلسلة migrations متفرعة: {branches}")
    return len(rows), errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--strict-release", action="store_true", help="اجعل غياب ملفات المنصات أو ملفات الصوت عائق فشل")
    args = parser.parse_args()

    print("KHAZAIN AL-HUDA — OFFLINE PROJECT AUDIT")
    print(f"Root: {ROOT}")

    json_ok = audit_json_files()
    zip_count, bad_zips = audit_zip_files()
    quran_total, quran_ok, quran_errors = audit_quran_bundle()
    groups, audio_entries, audio_errors = audit_audio_catalog()
    declared_assets, pubspec_errors = audit_pubspec_assets()
    migrations, migration_errors = audit_migrations()

    for group in (quran_errors, audio_errors, pubspec_errors, migration_errors):
        for error in group:
            print(f"ERROR: {error}")

    audio_files = [p for p in ROOT.rglob("*") if p.is_file() and p.suffix.lower() in AUDIO_EXTENSIONS]
    translations = list((ROOT / "content/resources/quran/03_QURAN_TRANSLATIONS").glob("*.zip"))
    placeholders = list((ROOT / "docs/reference/requirements-pack").rglob("*PLACEHOLDER*"))
    platform_android = (ROOT / "apps/mobile/android").is_dir()
    platform_ios = (ROOT / "apps/mobile/ios").is_dir()

    print("\nملخص الملفات والتحقق:")
    print(f"- JSON صالح: {json_ok}")
    print(f"- أرشيفات ZIP: {zip_count}؛ تالفة: {bad_zips}")
    print(f"- ملفات بيان المصحف المطابقة للبصمة: {quran_ok}/{quran_total}")
    print(f"- مجموعات فهرس الصوتيات: {groups}؛ مصادر مفهرسة: {audio_entries}")
    print(f"- أرشيفات ترجمات QuranEnc: {len(translations)}")
    print(f"- أصول Flutter المعلنة: {declared_assets}")
    print(f"- Alembic migrations: {migrations}")
    print(f"- ملفات صوتية ثنائية محلية: {len(audio_files)}")
    print(f"- ملفات PLACEHOLDER المرجعية: {len(placeholders)}")
    print(f"- منصة Android: {'موجودة' if platform_android else 'غير موجودة'}")
    print(f"- منصة iOS: {'موجودة' if platform_ios else 'غير موجودة'}")

    blockers = []
    if not platform_android:
        blockers.append("يجب توليد وتهيئة منصة Android من Flutter SDK.")
    if not platform_ios:
        blockers.append("يجب توليد وتهيئة منصة iOS على macOS/Xcode.")
    if not audio_files:
        blockers.append("لا توجد ملفات صوتية فعلية ضمن الأرشيف؛ الفهرس لا يساوي تنزيل تسجيلات.")
    blockers.extend(quran_errors + audio_errors + pubspec_errors + migration_errors)
    if bad_zips:
        blockers.append(f"يوجد {bad_zips} أرشيف ZIP تالف.")

    print("\nالموانع/النواقص:")
    for blocker in blockers:
        print(f"- {blocker}")
    if not blockers:
        print("- لم يرصد الفحص البنيوي موانع ضمن نطاقه.")

    hard_errors = bool(quran_errors or audio_errors or pubspec_errors or migration_errors or bad_zips)
    return 1 if hard_errors or (args.strict_release and blockers) else 0


if __name__ == "__main__":
    raise SystemExit(main())
