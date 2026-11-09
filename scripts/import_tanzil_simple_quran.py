#!/usr/bin/env python3
"""Build AlHadithi's bundled Tanzil Simple Quran asset from the official pipe text.

Ayah text is copied verbatim from each `surah|ayah|text` record. Blank lines
and the source's trailing comment/license block are not Quran records and are
not copied into the corpus JSON. Structural indices are added from metadata.
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path
from typing import Iterable

try:  # Support both direct execution and unittest/package imports.
    from .import_tanzil_quran import (
        atomic_write_json, build_asset_payload, parse_metadata_xml, validate_production_payload
    )
except ImportError:  # pragma: no cover - exercised by the command-line entry point
    from import_tanzil_quran import (
        atomic_write_json, build_asset_payload, parse_metadata_xml, validate_production_payload
    )

EXPECTED_SURAH_COUNT = 114
EXPECTED_AYAH_COUNT = 6236


def parse_quran_simple_text(path: Path, expected_counts: dict[int, int] | None = None) -> dict[int, list[str]]:
    """Parse Tanzil Simple pipe text without altering the third field (ayah text)."""
    result: dict[int, list[str]] = {}
    seen: set[tuple[int, int]] = set()
    records_in_source_order: list[tuple[int, int]] = []
    try:
        lines = path.read_text(encoding="utf-8-sig").splitlines()
    except (OSError, UnicodeDecodeError) as exc:
        raise ValueError(f"Cannot read UTF-8 Quran text '{path}': {exc}") from exc

    for line_number, line in enumerate(lines, start=1):
        if not line or line.lstrip().startswith("#"):
            continue
        surah_field, separator, remainder = line.partition("|")
        ayah_field, separator2, ayah_text = remainder.partition("|")
        if not separator or not separator2:
            raise ValueError(f"Malformed Tanzil record at line {line_number}")
        if not surah_field.isdecimal() or not ayah_field.isdecimal():
            raise ValueError(f"Invalid surah/ayah number at line {line_number}")
        surah_number = int(surah_field)
        ayah_number = int(ayah_field)
        if not (1 <= surah_number <= EXPECTED_SURAH_COUNT):
            raise ValueError(f"Surah number out of range at line {line_number}: {surah_number}")
        if ayah_number < 1:
            raise ValueError(f"Ayah number must be positive at line {line_number}")
        key = (surah_number, ayah_number)
        if key in seen:
            raise ValueError(f"Duplicate ayah record at line {line_number}: {key}")
        if not ayah_text or not ayah_text.strip():
            raise ValueError(f"Empty ayah text at line {line_number}: {key}")
        if not any("\u0600" <= character <= "\u06FF" for character in ayah_text):
            raise ValueError(f"No Arabic characters at line {line_number}: {key}")

        # Do not call strip(), normalize Unicode, remove marks, or edit basmala.
        result.setdefault(surah_number, []).append(ayah_text)
        seen.add(key)
        records_in_source_order.append(key)

    if not result:
        raise ValueError("No Quran ayah records found in Simple text file")
    if expected_counts is not None:
        expected_keys = [
            (surah_number, ayah_number)
            for surah_number in range(1, EXPECTED_SURAH_COUNT + 1)
            for ayah_number in range(1, expected_counts[surah_number] + 1)
        ]
        if records_in_source_order != expected_keys:
            mismatch = next(
                (index for index, pair in enumerate(zip(records_in_source_order, expected_keys))
                 if pair[0] != pair[1]),
                min(len(records_in_source_order), len(expected_keys)),
            )
            actual = records_in_source_order[mismatch] if mismatch < len(records_in_source_order) else None
            expected = expected_keys[mismatch] if mismatch < len(expected_keys) else None
            raise ValueError(
                f"Quran records are missing, duplicated, or out of order at record {mismatch + 1}: "
                f"expected {expected}, found {actual}"
            )
    else:
        expected_keys = [
            (surah_number, ayah_number)
            for surah_number in sorted(result)
            for ayah_number in range(1, len(result[surah_number]) + 1)
        ]
        if records_in_source_order != expected_keys:
            raise ValueError("Quran records are not in strict surah/ayah order")
    return result


def validate_metadata_offsets(metadata_path: Path, surah_counts: dict[int, int]) -> None:
    """Confirm the metadata's optional zero-based `start` offsets match its counts."""
    import xml.etree.ElementTree as ET

    try:
        root = ET.parse(metadata_path).getroot()
    except (ET.ParseError, OSError) as exc:
        raise ValueError(f"Cannot parse metadata XML '{metadata_path}': {exc}") from exc
    container = next(
        (element for element in root.iter() if element.tag.rsplit("}", 1)[-1] == "suras"),
        None,
    )
    if container is None:
        raise ValueError("Missing <suras> section in metadata")
    expected_start = 0
    for expected_number, element in enumerate(
        (child for child in list(container) if child.tag.rsplit("}", 1)[-1] == "sura"),
        start=1,
    ):
        number = int(element.attrib.get("index", "0"))
        count = int(element.attrib.get("ayas", "0"))
        if number != expected_number:
            raise ValueError(f"Metadata surah order mismatch at {expected_number}")
        if surah_counts.get(number) != count:
            raise ValueError(
                f"Surah {number}: text has {surah_counts.get(number)} ayahs; metadata says {count}"
            )
        source_start = element.attrib.get("start")
        if source_start is not None and int(source_start) != expected_start:
            raise ValueError(
                f"Metadata start offset mismatch for surah {number}: "
                f"expected {expected_start}, found {source_start}"
            )
        expected_start += count
    if expected_start != EXPECTED_AYAH_COUNT:
        raise ValueError(f"Metadata total must be {EXPECTED_AYAH_COUNT}; got {expected_start}")


def main(argv: Iterable[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--quran", type=Path, required=True,
                        help="Path to official Tanzil Simple pipe text file")
    parser.add_argument("--metadata", type=Path, required=True,
                        help="Path to official Tanzil quran-data.xml metadata")
    parser.add_argument("--output", type=Path,
                        default=Path("app/src/main/assets/quran_simple_full.json"),
                        help="Output Android asset path")
    args = parser.parse_args(argv)

    try:
        counts, juz_boundaries, quarter_boundaries, sajda_positions = parse_metadata_xml(args.metadata)
        if len(counts) != EXPECTED_SURAH_COUNT:
            raise ValueError(f"Metadata must contain {EXPECTED_SURAH_COUNT} surahs; got {len(counts)}")
        quran = parse_quran_simple_text(args.quran, counts)
        validate_metadata_offsets(args.metadata, counts)
        if sum(counts.values()) != EXPECTED_AYAH_COUNT:
            raise ValueError(f"Metadata must contain {EXPECTED_AYAH_COUNT} ayahs; got {sum(counts.values())}")
        if set(quran) != set(counts):
            raise ValueError("Text and metadata do not contain the same surah numbers")

        payload = build_asset_payload(
            quran, counts, juz_boundaries, quarter_boundaries, sajda_positions
        )
        validate_production_payload(payload)
        atomic_write_json(args.output, payload)
        print(f"OK: wrote {len(payload)} surahs / {sum(len(item['a']) for item in payload)} ayahs to {args.output}")
        print("Edition: Tanzil Quran Text (Simple, Version 1.1), CC BY 3.0")
        print("Notice: All ayah text strings, including signs and basmala, were preserved verbatim.")
        return 0
    except (ValueError, OSError) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
