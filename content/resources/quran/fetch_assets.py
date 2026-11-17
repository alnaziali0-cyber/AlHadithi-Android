#!/usr/bin/env python3
"""Download selected, publisher-authorized Quran resources for Khazain AlHuda.

Standard-library only. Run from the extracted project folder using Python 3.10+.
This script deliberately does not scrape audio/library websites or bypass access controls.
"""
from __future__ import annotations
import argparse
import csv
import hashlib
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import zipfile
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent
LOG_DIR = ROOT / "15_LOGS"
LOG_DIR.mkdir(parents=True, exist_ok=True)

SOURCES = {
    "tanzil_text": "https://tanzil.net/pub/download/index.php",
    "tanzil_metadata": "https://tanzil.net/res/text/metadata/quran-data.xml",
    "quranenc_font_bundle": "https://quranenc.com/downloads/assets/Hafs_20.zip",
    "quranenc_translation_list": "https://quranenc.com/api/v1/translations/list",
    "quranenc_api_docs": "https://quranenc.com/en/home/api",
    "quran_foundation_docs": "https://api-docs.quran.com/",
    "quran_foundation_sync": "https://api-docs.quran.com/docs/tutorials/content-sync/getting-started/",
    "openiti_license_docs": "https://openiti.org/documentation/",
}


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def safe_name(s: str) -> str:
    return re.sub(r"[^A-Za-z0-9._-]+", "_", s).strip("._")[:120] or "resource"


def fetch(url: str, dest: Path, *, data: bytes | None = None, expected: str | None = None) -> dict:
    dest.parent.mkdir(parents=True, exist_ok=True)
    req = urllib.request.Request(
        url,
        data=data,
        headers={
            "User-Agent": "KhazainAlHuda-ResourceFetcher/1.0 (respecting publisher terms)",
            "Accept": "*/*",
        },
        method="POST" if data is not None else "GET",
    )
    try:
        with urllib.request.urlopen(req, timeout=90) as response:
            body = response.read()
            content_type = response.headers.get("Content-Type", "")
            final_url = response.geturl()
        # Avoid saving an HTML error / challenge page as a data file.
        if expected == "zip" and not body.startswith(b"PK\x03\x04"):
            raise ValueError(f"Expected ZIP, received {content_type or 'unknown content'}")
        if expected == "xml" and b"<" not in body[:100]:
            raise ValueError("Expected XML content")
        if expected == "json":
            json.loads(body.decode("utf-8-sig"))
        if expected == "text" and ("html" in content_type.lower() or body.lstrip().lower().startswith(b"<!doctype html")):
            raise ValueError("Server returned HTML instead of a text download")
        dest.write_bytes(body)
        return {
            "status": "downloaded",
            "url": url,
            "final_url": final_url,
            "file": str(dest.relative_to(ROOT)),
            "bytes": len(body),
            "content_type": content_type,
            "sha256": sha256_file(dest),
            "downloaded_at_utc": datetime.now(timezone.utc).isoformat(),
        }
    except Exception as exc:
        return {"status": "failed", "url": url, "error": f"{type(exc).__name__}: {exc}"}


def fetch_tanzil_text() -> dict:
    # Official download page is a form-driven download. These fields correspond
    # to its documented selector values; Quran text is kept verbatim.
    form = {
        "quranType": "uthmani",
        "outType": "txt-2",
        "marks": "true",
        "sajdah": "true",
        "tatweel": "true",
        "agree": "true",
    }
    data = urllib.parse.urlencode(form).encode("utf-8")
    dest = ROOT / "01_QURAN" / "tanzil_uthmani_verbatim.txt"
    result = fetch(SOURCES["tanzil_text"], dest, data=data, expected="text")
    if result["status"] == "downloaded":
        notice = (
            "Tanzil Quran Text\n"
            "Copyright (C) 2007-2026 Tanzil Project\n"
            "License: Creative Commons Attribution 3.0\n\n"
            "Permission is granted to copy and distribute verbatim copies of this text, "
            "but CHANGING IT IS NOT ALLOWED.\n"
            "This Quran text can be used in any website or application, provided that "
            "its source (Tanzil Project) is clearly indicated, and a link is made to "
            "https://tanzil.net/ to enable users to keep track of changes.\n"
            "Please check updates at https://tanzil.net/updates/\n"
        )
        (ROOT / "13_LICENSES" / "TANZIL_NOTICE.txt").write_text(notice, encoding="utf-8")
    return result


def fetch_quranenc_translations(languages: list[str] | None, all_translations: bool) -> list[dict]:
    catalog_url = SOURCES["quranenc_translation_list"] + "?" + urllib.parse.urlencode({"localization": "ar"})
    catalog_path = ROOT / "03_QURAN_TRANSLATIONS" / "translations_catalog_ar.json"
    catalog_result = fetch(catalog_url, catalog_path, expected="json")
    results = [catalog_result]
    if catalog_result["status"] != "downloaded":
        return results
    try:
        payload = json.loads(catalog_path.read_text(encoding="utf-8"))
        translations = payload.get("translations", [])
    except Exception as exc:
        return results + [{"status": "failed", "error": f"Cannot parse translations catalog: {exc}"}]

    wanted = None if all_translations else set(languages or ["ar"])
    selected = [t for t in translations if wanted is None or t.get("language_iso_code") in wanted]
    for item in selected:
        url = item.get("database_url")
        if not url:
            continue
        key = safe_name(str(item.get("key", "translation")))
        version = safe_name(str(item.get("version", "unknown")))
        target = ROOT / "03_QURAN_TRANSLATIONS" / f"{key}_{version}.zip"
        result = fetch(url, target, expected="zip")
        result.update({
            "translation_key": item.get("key"),
            "language_iso_code": item.get("language_iso_code"),
            "title": item.get("title"),
            "version": item.get("version"),
            "last_update": item.get("last_update"),
            "publisher_source": "QuranEnc.com",
        })
        results.append(result)
        time.sleep(0.25)
    return results


def main() -> int:
    parser = argparse.ArgumentParser(description="Download licensed Quran resources for Khazain AlHuda")
    parser.add_argument("--languages", default="ar", help="Comma-separated QuranEnc translation language codes; default: ar")
    parser.add_argument("--all-translations", action="store_true", help="Download every QuranEnc translation database available in the public catalog")
    parser.add_argument("--skip-tanzil-text", action="store_true", help="Skip form-based Tanzil text download")
    parser.add_argument("--skip-font", action="store_true", help="Skip QuranEnc Hafs_20 font+text ZIP")
    args = parser.parse_args()

    report: list[dict] = []
    if not args.skip_tanzil_text:
        report.append(fetch_tanzil_text())

    report.append(fetch(
        SOURCES["tanzil_metadata"],
        ROOT / "02_QURAN_METADATA" / "quran-data.xml",
        expected="xml",
    ))

    if not args.skip_font:
        zip_path = ROOT / "11_FONTS" / "QuranEnc_Hafs_20.zip"
        r = fetch(SOURCES["quranenc_font_bundle"], zip_path, expected="zip")
        if r["status"] == "downloaded":
            try:
                with zipfile.ZipFile(zip_path) as z:
                    bad = z.testzip()
                    r["zip_integrity"] = "ok" if bad is None else f"bad member: {bad}"
                    if bad is None:
                        extract_dir = ROOT / "11_FONTS" / "Hafs_20"
                        extract_dir.mkdir(exist_ok=True)
                        z.extractall(extract_dir)
            except Exception as exc:
                r["zip_integrity"] = f"failed: {exc}"
        report.append(r)

    langs = [x.strip() for x in args.languages.split(",") if x.strip()]
    report.extend(fetch_quranenc_translations(langs, args.all_translations))

    output = {
        "project": "Khazain AlHuda",
        "generated_at_utc": datetime.now(timezone.utc).isoformat(),
        "note": "Only resources successfully downloaded by this run are marked downloaded. Check each publisher's current terms before app release.",
        "results": report,
    }
    (LOG_DIR / "fetch_report.json").write_text(json.dumps(output, ensure_ascii=False, indent=2), encoding="utf-8")
    success = sum(1 for x in report if x.get("status") == "downloaded")
    failed = sum(1 for x in report if x.get("status") == "failed")
    print(f"Finished. Downloaded: {success}; failed: {failed}.")
    print(f"Report: {LOG_DIR / 'fetch_report.json'}")
    return 0 if success else 2

if __name__ == "__main__":
    raise SystemExit(main())
