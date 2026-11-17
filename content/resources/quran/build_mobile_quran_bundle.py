#!/usr/bin/env python3
"""Build the app's offline Hafs/Imla'i corpus from a pinned quran-json snapshot.

The Arabic text file is copied byte-for-byte; this script never edits verse strings.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any

EXPECTED_TEXT_SHA256 = "703246a73a0a3d57f721b02e3edad9a2ee6831b79f49ffb9513ddfe99a65b801"
EXPECTED_CHAPTERS_SHA256 = "5b18adae945fcb6f9fbb865dd7dc596f89a63607a4e6617ca9e37f7f10d3386b"
EXPECTED_QURAN_JSON_REVISION = "6523a9ad3b73bb7e8cb3a9cdf0ea774b8bab3123"
SNAPSHOT_DATE = "2026-11-15"
QURAN_JSON_REPO = "https://github.com/risan/quran-json"
TANZIL_LICENSE_URL = "https://tanzil.net/docs/text_license"
TANZIL_TYPES_URL = "https://tanzil.net/docs/quran_text_types"
ISLAMIC_NETWORK_TERMS = "https://alquran.cloud/terms-and-conditions"


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def read_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(value, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
        newline="\n",
    )


def source_record(records: list[dict[str, Any]], relpath: str) -> dict[str, Any]:
    for record in records:
        if record.get("path") == relpath:
            return record
    raise ValueError(f"Missing upstream source record: {relpath}")


def verify_source_text(raw: bytes, text: dict[str, Any]) -> int:
    if sha256(raw) != EXPECTED_TEXT_SHA256:
        raise ValueError("The source text snapshot hash differs from the reviewed commit.")
    if not isinstance(text, dict) or set(text) != {str(n) for n in range(1, 115)}:
        raise ValueError("Text snapshot must contain exactly the 114 numbered surahs.")
    total = 0
    for surah_number in range(1, 115):
        rows = text[str(surah_number)]
        if not isinstance(rows, list) or not rows:
            raise ValueError(f"Surah {surah_number} has no ayah rows.")
        for ayah_number, row in enumerate(rows, start=1):
            if (
                not isinstance(row, dict)
                or row.get("chapter") != surah_number
                or row.get("verse") != ayah_number
                or not isinstance(row.get("text"), str)
                or not row["text"].strip()
            ):
                raise ValueError(f"Invalid source row at {surah_number}:{ayah_number}.")
        total += len(rows)
    if total != 6236:
        raise ValueError(f"Expected 6,236 ayahs, got {total}.")
    return total


def verify_chapters(chapters_data: dict[str, Any]) -> None:
    chapters = chapters_data.get("chapters")
    indexes = chapters_data.get("indexes")
    if not isinstance(chapters, list) or len(chapters) != 114:
        raise ValueError("Tanzil chapter metadata must contain 114 surahs.")
    if not isinstance(indexes, dict):
        raise ValueError("Tanzil division metadata is missing.")
    if sum(int(chapter["total_verses"]) for chapter in chapters) != 6236:
        raise ValueError("Surah verse counts do not sum to 6,236.")
    if len(indexes.get("juzs", [])) != 30:
        raise ValueError("Expected 30 juz boundaries.")
    if len(indexes.get("hizbs", [])) != 240:
        raise ValueError("Expected 240 rub' al-hizb quarter boundaries.")
    if len(indexes.get("sajdas", [])) != 15:
        raise ValueError("Expected 15 sajdah positions in the upstream metadata.")


def source_reference(row: dict[str, Any]) -> dict[str, int]:
    return {"surah": int(row["sura"]), "ayah": int(row["aya"])}


def build(args: argparse.Namespace) -> Path:
    source_repo = args.source_repo.resolve()
    project_root = Path(__file__).resolve().parents[3]
    app_root = project_root / "apps" / "mobile"
    output = app_root / "assets" / "data" / "quran"
    data_root = source_repo / "data"

    revision = (source_repo / ".git").exists()
    if revision:
        import subprocess

        actual_revision = subprocess.check_output(
            ["git", "-C", str(source_repo), "rev-parse", "HEAD"], text=True
        ).strip()
        if actual_revision != EXPECTED_QURAN_JSON_REVISION:
            raise ValueError(
                "quran-json commit differs from the reviewed snapshot: "
                f"{actual_revision} != {EXPECTED_QURAN_JSON_REVISION}"
            )
    else:
        raise ValueError("Pass the cloned quran-json Git repository, including .git.")

    text_path = data_root / "tanzil" / "simple.json"
    chapters_path = data_root / "tanzil" / "chapters.json"
    source_catalog = read_json(data_root / "meta" / "sources.json")
    text_source = source_record(source_catalog, "data/tanzil/simple.json")
    chapter_source = source_record(source_catalog, "data/tanzil/chapters.json")
    text_bytes = text_path.read_bytes()
    chapters_bytes = chapters_path.read_bytes()
    if sha256(text_bytes) != text_source.get("sha256"):
        raise ValueError("quran-json text hash disagrees with its source catalog.")
    if sha256(chapters_bytes) != EXPECTED_CHAPTERS_SHA256:
        raise ValueError("The Tanzil chapter/index snapshot hash is not the reviewed version.")
    if sha256(chapters_bytes) != chapter_source.get("sha256"):
        raise ValueError("quran-json chapter hash disagrees with its source catalog.")

    text_data = json.loads(text_bytes.decode("utf-8"))
    chapters_data = json.loads(chapters_bytes.decode("utf-8"))
    total_ayahs = verify_source_text(text_bytes, text_data)
    verify_chapters(chapters_data)
    chapters = chapters_data["chapters"]
    indexes = chapters_data["indexes"]
    counts_by_surah = {int(row["id"]): int(row["total_verses"]) for row in chapters}
    for number in range(1, 115):
        if len(text_data[str(number)]) != counts_by_surah[number]:
            raise ValueError(f"Ayah count mismatch for surah {number}.")

    output.mkdir(parents=True, exist_ok=True)
    # Preserve the source byte stream exactly: no reserialization or Unicode normalization.
    (output / "ayahs.json").write_bytes(text_bytes)

    surahs = {
        "version": 1,
        "items": [
            {
                "number": int(chapter["id"]),
                "name_ar": chapter["name"],
                "transliteration": chapter["transliteration"],
                "revelation_type": chapter["type"],
                "ayah_count": int(chapter["total_verses"]),
            }
            for chapter in chapters
        ],
    }
    write_json(output / "surahs.json", surahs)

    juzs = indexes["juzs"]
    write_json(
        output / "juz.json",
        {
            "version": 1,
            "count": 30,
            "items": [
                {"number": int(row["index"]), "start": source_reference(row)}
                for row in juzs
            ],
        },
    )

    # Tanzil's 240 `hizbs` rows are rub' al-hizb (quarters), not 240 full hizbs.
    quarters = indexes["hizbs"]
    whole_hizbs = quarters[::4]
    if len(whole_hizbs) != 60:
        raise ValueError("Could not derive 60 complete hizb starts from 240 quarters.")
    write_json(
        output / "hizb.json",
        {
            "version": 1,
            "count": 60,
            "items": [
                {
                    "number": (int(row["index"]) + 3) // 4,
                    "start": source_reference(row),
                }
                for row in whole_hizbs
            ],
            "quarter_count": 240,
            "quarters": [
                {
                    "number": int(row["index"]),
                    "hizb_number": (int(row["index"]) + 3) // 4,
                    "quarter_in_hizb": ((int(row["index"]) - 1) % 4) + 1,
                    "start": source_reference(row),
                }
                for row in quarters
            ],
        },
    )

    write_json(
        output / "sajdah.json",
        {
            "version": 1,
            "count": len(indexes["sajdas"]),
            "items": [
                {
                    "number": int(row["index"]),
                    "position": source_reference(row),
                    "source_type": row.get("type", "unspecified"),
                }
                for row in indexes["sajdas"]
            ],
            "note": "Source classifications are metadata, not a jurisprudential ruling.",
        },
    )

    write_json(
        output / "bookmarks.json",
        {
            "version": 1,
            "storage": "runtime bookmarks are stored locally in SharedPreferences; this is the empty seed schema",
            "items": [],
            "last_read": None,
        },
    )

    repo_license_text = (source_repo / "LICENSE.txt").read_text(encoding="utf-8")
    app_license_dir = output / "licenses"
    app_license_dir.mkdir(parents=True, exist_ok=True)
    (app_license_dir / "QURAN_JSON_CC-BY-SA-4.0.txt").write_text(
        repo_license_text, encoding="utf-8", newline="\n"
    )
    notice = f"""# إشعار مصدر وترخيص حزمة المصحف

## نص القرآن

الملف `ayahs.json` نسخة بايتية مطابقة لملف `data/tanzil/simple.json` من المستودع [risan/quran-json]({QURAN_JSON_REPO}) عند الالتزام `{EXPECTED_QURAN_JSON_REVISION}`. لم تُعد كتابة بيانات JSON أو تطبيع أو تعديل أي سلسلة من سلاسل الآيات. بصمة الملف المصدر والمضمّن: `{sha256(text_bytes)}`.

مصدر النص الأصلي: **Tanzil Quran Text**، الإصدار 1.1، نوع **Simple / Imla'ei** (رسم إملائي حديث، كامل التشكيل). يعرّف بيان المستودع النسخة بأنها حفص عن عاصم وبترقيم حفص، وعددها 6,236 آية في 114 سورة. لا يثبت ذلك مراجعة بصرية مستقلة لهذه الحزمة؛ تظل مراجعة قارئ مختص مقابل مصحف مطبوع مطلوبة قبل النشر النهائي.

حقوق Tanzil: **Creative Commons Attribution 3.0**، نسخ حرفي فقط، لا يجوز تغيير النص. يجب إبقاء الإشعار ورابط Tanzil في النسخ الموزعة:

> Tanzil Quran Text — Copyright (C) 2007-2021 Tanzil Project — CC BY 3.0 — https://tanzil.net/

المصدر: [Tanzil Text License]({TANZIL_LICENSE_URL})، [تعريف أنواع الرسم]({TANZIL_TYPES_URL}).

## قاعدة البيانات المجمّعة

بيانات الفهرسة والبنية المأخوذة من `quran-json` تخضع لترخيص المستودع **CC BY-SA 4.0**؛ يرفق نص الترخيص في `licenses/QURAN_JSON_CC-BY-SA-4.0.txt`. هذا لا يغيّر ولا يستبدل شروط Tanzil الخاصة بنص الآيات.

## سند القراءة

تم فصل تعريف القراءة وسندها في `quran_attribution.json`. سلسلة حفص عن عاصم كما يوردها مرجع إسلام ويب هي: حفص بن سليمان ← عاصم بن أبي النجود ← أبو عبد الرحمن السلمي ← علي بن أبي طالب ← النبي محمد. هذا توثيق مرجعي عام للرواية، وليس ادعاءً بأن التطبيق أو ناشره يملك إجازة أو سند أداء خاصاً، ولا نسبة إلى مرجعية أو حوزة.

## الخط

الخط القرآني المحلي `AmiriQuran.ttf` مضمّن مع إشعار ترخيص SIL Open Font License في `../../fonts/AMIRI_OFL_LICENSE.txt`. أُجري فحص تغطية Unicode لمحارف النص المضمّن: راجع تقرير التحقق.

## الصوت

لا تُضمّن ملفات صوتية في حزمة التطبيق. رابط القراءة على Islamic Network/AlQuran Cloud يُستخدم للبث والتنزيل الاختياري إلى مساحة التطبيق للاستخدام الشخصي/التعليمي دون اتصال. الشروط المنشورة تبقي حقوق كل تسجيل لقارئه، وتجيز التنزيل الشخصي/التعليمي؛ الاستخدام التجاري أو إعادة توزيع التسجيلات يحتاجان مراجعة إذن مستقلة. المصدر: [{ISLAMIC_NETWORK_TERMS}]({ISLAMIC_NETWORK_TERMS}). لا تُستخدم روابط EveryAyah أو MP3Quran لأن حالة الترخيص في سجل المصدر غير معلومة.
"""
    (output / "QURAN_SOURCE_NOTICE_AR.md").write_text(notice, encoding="utf-8", newline="\n")

    sources: list[dict[str, Any]] = []
    for record in (text_source, chapter_source):
        sources.append(
            {
                "source_id": "tanzil_simple_text" if record is text_source else "tanzil_chapter_metadata",
                "path_in_source_repo": record["path"],
                "source": record["source"],
                "url": record["url"],
                "license": record["license"],
                "license_status": record["license_status"],
                "license_url": record["license_url"],
                "sha256": record["sha256"],
                "bytes": record["bytes"],
                "revision": record["revision"],
            }
        )
    for relpath in (
        "data/audio/islamic_network_by_ayah.json",
        "data/audio/islamic_network_by_surah.json",
    ):
        record = source_record(source_catalog, relpath)
        sources.append(
            {
                "source_id": relpath.rsplit("/", 1)[-1].removesuffix(".json"),
                "path_in_source_repo": record["path"],
                "source": record["source"],
                "url": record["url"],
                "license": record["license"],
                "license_status": record["license_status"],
                "license_url": record["license_url"],
                "sha256": record["sha256"],
                "bytes": record["bytes"],
                "revision": record["revision"],
            }
        )
    sources.append(
        {
            "source_id": "quran_json_dataset",
            "source": "Quran JSON",
            "url": QURAN_JSON_REPO,
            "license": "Creative Commons Attribution-ShareAlike 4.0 International",
            "license_status": "granted",
            "license_url": "https://creativecommons.org/licenses/by-sa/4.0/",
            "revision": EXPECTED_QURAN_JSON_REVISION,
            "license_file": "licenses/QURAN_JSON_CC-BY-SA-4.0.txt",
        }
    )
    sources.extend(
        [
            {
                "source_id": "quran_json_reading_manifest",
                "source": "Quran JSON published reading/script metadata",
                "url": "https://quran-json.risanb.com/manifest.json",
                "license_status": "metadata reference",
                "claim": "simple script; Hafs riwayah; Asim qiraah; Hafs verse numbering",
                "repository_commit": EXPECTED_QURAN_JSON_REVISION,
            },
            {
                "source_id": "hafs_reading_chain_reference",
                "source": "إسلام ويب — فتوى سند رواية عاصم بن أبي النجود",
                "url": "https://www.islamweb.net/ar/fatwa/100241/",
                "reference_number": "100241",
                "license_status": "citation reference only; no content copied",
                "claim_scope": "chain as reported by the page; not an app-specific ijazah or institutional endorsement",
            },
        ]
    )
    write_json(
        output / "quran_sources.json",
        {
            "version": 1,
            "upstream_repository": QURAN_JSON_REPO,
            "upstream_commit": EXPECTED_QURAN_JSON_REVISION,
            "items": sources,
        },
    )

    write_json(
        output / "quran_attribution.json",
        {
            "version": 1,
            "text_source": {
                "publisher": "Tanzil Project",
                "dataset": "Tanzil Quran Text 1.1 — Simple / Imla'ei",
                "repository_mirror": QURAN_JSON_REPO,
                "upstream_commit": EXPECTED_QURAN_JSON_REVISION,
                "source_path": "data/tanzil/simple.json",
                "reading_label_in_source_manifest": "Hafs 'an Asim",
                "reading_metadata_url": "https://quran-json.risanb.com/manifest.json",
                "verse_numbering": "Hafs / Kufi",
                "license": "CC BY 3.0; verbatim only; text must not be changed",
                "license_url": TANZIL_LICENSE_URL,
                "source_url": text_source["url"],
                "sha256": sha256(text_bytes),
                "verbatim_copy": True,
                "required_notice": "Tanzil Quran Text — Copyright (C) 2007-2021 Tanzil Project — CC BY 3.0 — https://tanzil.net/",
            },
            "reading_sanad": {
                "riwayah": "حفص عن عاصم",
                "narrator": "حفص بن سليمان الأسدي الكوفي",
                "reader": "عاصم بن أبي النجود",
                "chain_as_reported": [
                    "حفص بن سليمان",
                    "عاصم بن أبي النجود",
                    "أبو عبد الرحمن السلمي",
                    "علي بن أبي طالب",
                    "النبي محمد صلى الله عليه وآله وسلم",
                ],
                "reference": {
                    "publisher": "إسلام ويب",
                    "title": "سند رواية عاصم بن أبي النجود",
                    "reference_number": "100241",
                    "url": "https://www.islamweb.net/ar/fatwa/100241/",
                },
                "scope_note": "هذا مرجع لتعريف السلسلة المنقولة للرواية فقط؛ ليس إجازة أو سند أداء للتطبيق أو ناشره، وليس تزكية لجهة أو مرجعية أو حوزة.",
                "specialist_review_status": "pending; scholarly review of the exact edition and sanad reference is required before final release",
            },
            "dataset_license": {
                "publisher": "Quran JSON (risan)",
                "commit": EXPECTED_QURAN_JSON_REVISION,
                "license": "CC BY-SA 4.0",
                "url": QURAN_JSON_REPO,
                "license_url": "https://creativecommons.org/licenses/by-sa/4.0/",
                "license_file": "licenses/QURAN_JSON_CC-BY-SA-4.0.txt",
                "scope_note": "Applies to the repository's database compilation/arrangement; it does not replace the upstream Tanzil text license.",
            },
            "audio": {
                "provider": "Islamic Network CDN / AlQuran Cloud",
                "reciter": "Mishary Rashid Alafasy",
                "reciter_id": "ar.alafasy",
                "reading": "hafs",
                "verse_numbering": "global Hafs ayah index, 1-6236",
                "license_status": "download allowed for personal and educational use under current provider terms; recording copyright remains with the reciter",
                "terms_url": ISLAMIC_NETWORK_TERMS,
                "ayah_url_template": "https://cdn.islamic.network/quran/audio/128/ar.alafasy/{global_ayah}.mp3",
                "surah_url_template": "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/{surah}.mp3",
                "bundled_in_app": False,
                "commercial_distribution_review_required": True,
            },
            "font": {
                "family": "Amiri Quran",
                "file": "assets/fonts/AmiriQuran.ttf",
                "license": "SIL Open Font License",
                "license_file": "assets/fonts/AMIRI_OFL_LICENSE.txt",
            },
            "institutional_attribution": {
                "marja_or_hawza": None,
                "statement": "لا تُنسب هذه النسخة إلى مرجعية أو حوزة.",
            },
        },
    )

    data_files = [
        "surahs.json",
        "ayahs.json",
        "juz.json",
        "hizb.json",
        "sajdah.json",
        "bookmarks.json",
        "quran_sources.json",
        "quran_attribution.json",
    ]
    integrity = {name: sha256((output / name).read_bytes()) for name in data_files}
    integrity["QURAN_SOURCE_NOTICE_AR.md"] = sha256(
        (output / "QURAN_SOURCE_NOTICE_AR.md").read_bytes()
    )
    integrity["licenses/QURAN_JSON_CC-BY-SA-4.0.txt"] = sha256(
        (app_license_dir / "QURAN_JSON_CC-BY-SA-4.0.txt").read_bytes()
    )
    write_json(
        output / "quran.json",
        {
            "schema_version": 1,
            "bundle_version": "1.0.0",
            "generated": SNAPSHOT_DATE,
            "reading": {
                "id": "hafs",
                "riwayah": "حفص",
                "qiraah": "عاصم الكوفي",
                "verse_numbering": "kufi_hafs",
                "ayah_count": total_ayahs,
            },
            "script": {
                "id": "simple",
                "name_ar": "الرسم الإملائي (البسيط)",
                "name_en": "Imla'ei / Simple",
                "vocalization": "full",
                "source": "Tanzil Quran Text 1.1",
            },
            "counts": {
                "surahs": 114,
                "ayahs": total_ayahs,
                "juz": 30,
                "hizb": 60,
                "hizb_quarters": 240,
                "sajdah_positions": len(indexes["sajdas"]),
            },
            "text_sha256": sha256(text_bytes),
            "text_source_sha256": text_source["sha256"],
            "upstream_repository": QURAN_JSON_REPO,
            "upstream_commit": EXPECTED_QURAN_JSON_REVISION,
            "offline_text_and_font": True,
            "audio": {
                "reciter_id": "ar.alafasy",
                "reading": "hafs",
                "provider": "Islamic Network CDN / AlQuran Cloud",
                "ayah_url_template": "https://cdn.islamic.network/quran/audio/128/ar.alafasy/{global_ayah}.mp3",
                "surah_url_template": "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/{surah}.mp3",
                "licensed_for_personal_educational_download": True,
                "bundled_in_app": False,
                "terms_url": ISLAMIC_NETWORK_TERMS,
            },
            "integrity": integrity,
        },
    )

    print(f"Wrote offline Quran bundle: {output}")
    print(f"Surahs: 114; ayahs: {total_ayahs}; juz: 30; hizb: 60; quarters: 240; sajdah: {len(indexes['sajdas'])}")
    print(f"Text (source and bundled) SHA-256: {sha256(text_bytes)}")
    print(f"quran-json revision: {EXPECTED_QURAN_JSON_REVISION}")
    return output


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--source-repo",
        type=Path,
        default=Path("/workspace/quran-json"),
        help="path to the pinned clone of https://github.com/risan/quran-json.git",
    )
    args = parser.parse_args()
    build(args)


if __name__ == "__main__":
    main()
