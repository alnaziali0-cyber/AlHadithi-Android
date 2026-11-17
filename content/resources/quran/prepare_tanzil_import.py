#!/usr/bin/env python3
"""Prepare a structurally mapped Tanzil TSV for validation only.

The source verse text is copied verbatim from Tanzil's official 3-column export.
This script does not connect to or modify the application database, approve the
text, or enable the Quran reader. A qualified human review remains mandatory.
"""
from __future__ import annotations

import bisect
import hashlib
import json
from pathlib import Path
import shutil
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent
SOURCE_TEXT = ROOT / "01_QURAN" / "tanzil_uthmani_verbatim.txt"
SOURCE_METADATA = ROOT / "02_QURAN_METADATA" / "quran-data.xml"
OUTPUT_TSV = ROOT / "01_QURAN" / "tanzil_uthmani_structured_import.tsv"
OUTPUT_NOTICE = ROOT / "01_QURAN" / "TANZIL_NOTICE.txt"
OUTPUT_REPORT = ROOT / "01_QURAN" / "TANZIL_IMPORT_PREVIEW.json"


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def verse_position(surah: int, ayah: int, counts: list[int]) -> int:
    if surah < 1 or surah > len(counts) or ayah < 1 or ayah > counts[surah - 1]:
        raise ValueError(f"Verse outside source metadata range: {surah}:{ayah}")
    return sum(counts[: surah - 1]) + ayah


def boundary_index(group: ET.Element, item_tag: str, counts: list[int]) -> tuple[list[int], list[int]]:
    starts: list[int] = []
    values: list[int] = []
    for item in group.findall(item_tag):
        surah = int(item.attrib["sura"])
        ayah = int(item.attrib["aya"])
        index = int(item.attrib["index"])
        starts.append(verse_position(surah, ayah, counts))
        values.append(index)
    if not starts or starts[0] != 1 or starts != sorted(starts) or values != sorted(values):
        raise ValueError(f"Invalid or incomplete {group.tag} boundaries")
    return starts, values


def value_at(boundary: tuple[list[int], list[int]], position: int) -> int:
    starts, values = boundary
    offset = bisect.bisect_right(starts, position) - 1
    if offset < 0:
        raise ValueError(f"No structural boundary covers verse position {position}")
    return values[offset]


def main() -> None:
    if not SOURCE_TEXT.is_file() or not SOURCE_METADATA.is_file():
        raise FileNotFoundError("Run fetch_assets.py successfully before preparing the Tanzil preview")

    xml_root = ET.parse(SOURCE_METADATA).getroot()
    sura_rows = xml_root.find("suras")
    if sura_rows is None:
        raise ValueError("Tanzil metadata is missing the suras group")
    surahs = sura_rows.findall("sura")
    counts = [int(item.attrib["ayas"]) for item in surahs]
    if len(counts) != 114 or sum(counts) != 6236:
        raise ValueError("Tanzil metadata does not describe 114 surahs and 6,236 ayahs")

    text_raw = SOURCE_TEXT.read_bytes()
    text = text_raw.decode("utf-8-sig")
    verses: list[tuple[int, int, str]] = []
    expected = (1, 1)
    for line_number, line in enumerate(text.splitlines(), start=1):
        if not line or line.startswith("#"):
            continue
        parts = line.split("|", 2)
        if len(parts) != 3:
            raise ValueError(f"Unexpected Tanzil source shape at line {line_number}")
        surah, ayah = int(parts[0]), int(parts[1])
        if (surah, ayah) != expected or not parts[2]:
            raise ValueError(f"Unexpected verse order/content at source line {line_number}")
        verses.append((surah, ayah, parts[2]))
        expected = (surah + 1, 1) if ayah == counts[surah - 1] else (surah, ayah + 1)
    if len(verses) != 6236 or expected != (115, 1):
        raise ValueError(f"Expected 6,236 sequential verse rows; found {len(verses)}")

    juz = boundary_index(xml_root.find("juzs"), "juz", counts)  # type: ignore[arg-type]
    quarter = boundary_index(xml_root.find("hizbs"), "quarter", counts)  # type: ignore[arg-type]
    pages = boundary_index(xml_root.find("pages"), "page", counts)  # type: ignore[arg-type]
    if len(juz[1]) != 30 or len(quarter[1]) != 240 or len(pages[1]) != 604:
        raise ValueError("Unexpected count of juz, quarter-hizb, or page boundaries")

    sajdas_group = xml_root.find("sajdas")
    if sajdas_group is None:
        raise ValueError("Tanzil metadata is missing the sajdas group")
    sajdas = {
        (int(item.attrib["sura"]), int(item.attrib["aya"]))
        for item in sajdas_group.findall("sajda")
    }
    output_lines: list[str] = []
    seen = {"juz": set(), "hizb": set(), "page": set()}
    text_copied: list[str] = []
    for surah, ayah, verse_text in verses:
        position = verse_position(surah, ayah, counts)
        juz_number = value_at(juz, position)
        quarter_number = value_at(quarter, position)
        hizb_number = ((quarter_number - 1) // 4) + 1
        page_number = value_at(pages, position)
        sajdah = 1 if (surah, ayah) in sajdas else 0
        output_lines.append(
            f"{surah}|{ayah}|{verse_text}|{juz_number}|{hizb_number}|{page_number}|{sajdah}"
        )
        text_copied.append(verse_text)
        seen["juz"].add(juz_number)
        seen["hizb"].add(hizb_number)
        seen["page"].add(page_number)

    if seen["juz"] != set(range(1, 31)) or seen["hizb"] != set(range(1, 61)) or seen["page"] != set(range(1, 605)):
        raise ValueError("Mapped structure does not cover all expected juz, hizb, and pages")

    output_raw = ("\n".join(output_lines) + "\n").encode("utf-8")
    # Verify that the third field survives the mapping byte-for-byte as Unicode text.
    recovered = [line.split("|", 6)[2] for line in output_raw.decode("utf-8").splitlines()]
    if recovered != text_copied:
        raise ValueError("Verse text changed while preparing the structural preview")

    notice_source = ROOT / "13_LICENSES" / "TANZIL_NOTICE.txt"
    if not notice_source.is_file():
        raise FileNotFoundError("Tanzil copyright notice is missing")
    OUTPUT_TSV.write_bytes(output_raw)
    shutil.copyfile(notice_source, OUTPUT_NOTICE)
    report = {
        "source": "Tanzil Project",
        "source_url": "https://tanzil.net/",
        "license_url": "https://tanzil.net/docs/text_license",
        "publisher_version": "not stated by the fetched file; confirm before release",
        "source_file": str(SOURCE_TEXT.relative_to(ROOT)),
        "source_sha256": sha256(text_raw),
        "metadata_file": str(SOURCE_METADATA.relative_to(ROOT)),
        "output_file": str(OUTPUT_TSV.relative_to(ROOT)),
        "output_sha256": sha256(output_raw),
        "surah_count": len(counts),
        "ayah_count": len(verses),
        "juz_count": len(seen["juz"]),
        "hizb_count": len(seen["hizb"]),
        "page_count": len(seen["page"]),
        "sajdah_mark_count": len(sajdas),
        "text_field_unchanged": True,
        "status": "staged_for_validation_and_scholarly_review_not_imported",
        "database_modified": False,
        "reader_enabled": False,
        "scholarly_review": "required_not_completed",
        "license_notice": str(OUTPUT_NOTICE.relative_to(ROOT)),
    }
    OUTPUT_REPORT.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Prepared {len(verses)} verses with Tanzil structure; no database was changed.")
    print(f"Preview: {OUTPUT_TSV}")
    print(f"SHA-256: {report['output_sha256']}")


if __name__ == "__main__":
    main()
