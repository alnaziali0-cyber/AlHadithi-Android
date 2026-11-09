#!/usr/bin/env python3
"""Build AlHadithi's offline Quran asset from official Tanzil XML downloads.

This tool never rewrites, normalizes, strips, or edits an ayah string. It only
adds structural indices required by the Android reader. Text is taken verbatim
from each XML `text` attribute and serialized as UTF-8 JSON.
"""
from __future__ import annotations

import argparse
import json
import os
import sys
import tempfile
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Iterable

EXPECTED_SURAH_COUNT = 114
EXPECTED_AYAH_COUNT = 6236
SAJDAH_MARK = "۩"


def _local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def _first_descendant(root: ET.Element, name: str) -> ET.Element:
    for element in root.iter():
        if _local_name(element.tag) == name:
            return element
    raise ValueError(f"Missing <{name}> section in metadata XML")


def _integer_attr(element: ET.Element, name: str) -> int:
    value = element.attrib.get(name)
    try:
        return int(value)  # type: ignore[arg-type]
    except (TypeError, ValueError) as exc:
        raise ValueError(
            f"Invalid or missing integer attribute '{name}' in <{_local_name(element.tag)}>"
        ) from exc


def parse_quran_xml(path: Path) -> dict[int, list[str]]:
    """Read source ayah strings without altering them."""
    try:
        root = ET.parse(path).getroot()
    except (ET.ParseError, OSError) as exc:
        raise ValueError(f"Cannot parse Quran XML '{path}': {exc}") from exc

    result: dict[int, list[str]] = {}
    for surah_element in root.iter():
        if _local_name(surah_element.tag) != "sura":
            continue
        ayah_elements = [child for child in list(surah_element) if _local_name(child.tag) == "aya"]
        if not ayah_elements:
            continue

        surah_number = _integer_attr(surah_element, "index")
        if surah_number in result:
            raise ValueError(f"Duplicate surah {surah_number} in Quran XML")
        ayahs: list[str] = []
        for expected_ayah, ayah_element in enumerate(ayah_elements, start=1):
            ayah_number = _integer_attr(ayah_element, "index")
            if ayah_number != expected_ayah:
                raise ValueError(
                    f"Surah {surah_number}: expected ayah {expected_ayah}, found {ayah_number}"
                )
            if "text" not in ayah_element.attrib:
                raise ValueError(f"Surah {surah_number}, ayah {ayah_number}: missing text attribute")
            text = ayah_element.attrib["text"]
            if not text or not text.strip():
                raise ValueError(f"Surah {surah_number}, ayah {ayah_number}: empty source text")
            if not any("\u0600" <= character <= "\u06ff" for character in text):
                raise ValueError(f"Surah {surah_number}, ayah {ayah_number}: no Arabic characters")
            # Deliberately preserve the attribute value exactly. Do not strip tashkeel,
            # sajdah signs, tatweel, spaces, or any other Quran text characters.
            ayahs.append(text)
        result[surah_number] = ayahs

    if not result:
        raise ValueError("No <sura><aya text=...> elements found in Quran XML")
    return result


def _parse_boundaries(metadata_root: ET.Element, section: str, child_name: str,
                      expected_count: int) -> list[tuple[int, int, int]]:
    container = _first_descendant(metadata_root, section)
    # Tanzil metadata has used both a dedicated <quarters> section and
    # <hizbs> containing 240 <quarter> elements. Keep support for both.
    entries = [
        element for element in container.iter()
        if element is not container and _local_name(element.tag) == child_name
    ]
    boundaries: list[tuple[int, int, int]] = []
    for element in entries:
        boundaries.append((
            _integer_attr(element, "index"),
            _integer_attr(element, "sura"),
            _integer_attr(element, "aya"),
        ))
    if len(boundaries) != expected_count:
        raise ValueError(
            f"Metadata section <{section}> must contain {expected_count} <{child_name}> entries; "
            f"found {len(boundaries)}"
        )
    if [item[0] for item in boundaries] != list(range(1, expected_count + 1)):
        raise ValueError(f"Indices in <{section}> are not sequential from 1 to {expected_count}")
    positions = [(sura, aya) for _, sura, aya in boundaries]
    if positions[0] != (1, 1):
        raise ValueError(f"First boundary in <{section}> must start at surah 1, ayah 1")
    if positions != sorted(positions) or len(set(positions)) != len(positions):
        raise ValueError(f"Boundaries in <{section}> are not strictly increasing")
    return boundaries


def _parse_quarter_boundaries(
    metadata_root: ET.Element, expected_count: int = 240
) -> list[tuple[int, int, int]]:
    """Read rub-al-hizb boundaries from either supported Tanzil metadata layout."""
    errors: list[str] = []
    for section in ("quarters", "hizbs"):
        try:
            return _parse_boundaries(metadata_root, section, "quarter", expected_count)
        except ValueError as exc:
            errors.append(str(exc))
    raise ValueError(f"No valid {expected_count}-entry quarter section found: " + " | ".join(errors))


def _parse_sajda_positions(metadata_root: ET.Element) -> set[tuple[int, int]]:
    container = _first_descendant(metadata_root, "sajdas")
    positions: set[tuple[int, int]] = set()
    for element in list(container):
        if _local_name(element.tag) != "sajda":
            continue
        position = (_integer_attr(element, "sura"), _integer_attr(element, "aya"))
        if position in positions:
            raise ValueError(f"Duplicate sajda metadata position: {position}")
        positions.add(position)
    if not positions:
        raise ValueError("Metadata <sajdas> section is empty")
    return positions


def _validate_boundary_positions(
    boundaries: list[tuple[int, int, int]], surah_counts: dict[int, int], label: str
) -> None:
    for index, surah, ayah in boundaries:
        if surah not in surah_counts or ayah < 1 or ayah > surah_counts[surah]:
            raise ValueError(
                f"{label} boundary {index} points outside the Quran: surah {surah}, ayah {ayah}"
            )


def parse_metadata_xml(
    path: Path,
    expected_juz_count: int = 30,
    expected_hizb_quarter_count: int = 240,
) -> tuple[
    dict[int, int],
    list[tuple[int, int, int]],
    list[tuple[int, int, int]],
    set[tuple[int, int]],
]:
    try:
        root = ET.parse(path).getroot()
    except (ET.ParseError, OSError) as exc:
        raise ValueError(f"Cannot parse Quran metadata XML '{path}': {exc}") from exc

    suras_container = _first_descendant(root, "suras")
    surah_counts: dict[int, int] = {}
    for element in list(suras_container):
        if _local_name(element.tag) != "sura":
            continue
        number = _integer_attr(element, "index")
        count = _integer_attr(element, "ayas")
        if number in surah_counts:
            raise ValueError(f"Duplicate metadata entry for surah {number}")
        if count <= 0:
            raise ValueError(f"Invalid ayah count for surah {number}")
        surah_counts[number] = count

    if not surah_counts:
        raise ValueError("No surah metadata entries found")
    expected_surah_numbers = list(range(1, len(surah_counts) + 1))
    if sorted(surah_counts) != expected_surah_numbers:
        raise ValueError("Surah metadata indices are not contiguous from 1")

    juz_boundaries = _parse_boundaries(root, "juzs", "juz", expected_juz_count)
    quarter_boundaries = _parse_quarter_boundaries(root, expected_hizb_quarter_count)
    if len(quarter_boundaries) != expected_hizb_quarter_count:
        raise ValueError(
            f"Expected {expected_hizb_quarter_count} hizb-quarter boundaries; "
            f"found {len(quarter_boundaries)}"
        )
    sajda_positions = _parse_sajda_positions(root)
    _validate_boundary_positions(juz_boundaries, surah_counts, "Juz")
    _validate_boundary_positions(quarter_boundaries, surah_counts, "Hizb-quarter")
    _validate_boundary_positions(
        [(index + 1, surah, ayah) for index, (surah, ayah) in enumerate(sorted(sajda_positions))],
        surah_counts,
        "Sajda",
    )
    return surah_counts, juz_boundaries, quarter_boundaries, sajda_positions


def _value_at_position(boundaries: list[tuple[int, int, int]], surah: int, ayah: int) -> int:
    value = 1
    for index, boundary_surah, boundary_ayah in boundaries:
        if (boundary_surah, boundary_ayah) > (surah, ayah):
            break
        value = index
    return value


def build_asset_payload(
    quran: dict[int, list[str]],
    surah_counts: dict[int, int],
    juz_boundaries: list[tuple[int, int, int]],
    hizb_boundaries: list[tuple[int, int, int]],
    sajda_positions: set[tuple[int, int]],
) -> list[dict[str, object]]:
    if set(quran) != set(surah_counts):
        raise ValueError("Quran text and metadata do not contain the same surah numbers")

    actual_sajda_positions = {
        (surah_number, ayah_number)
        for surah_number, ayahs in quran.items()
        for ayah_number, text in enumerate(ayahs, start=1)
        if SAJDAH_MARK in text
    }
    if actual_sajda_positions != sajda_positions:
        missing = sorted(sajda_positions - actual_sajda_positions)[:5]
        unexpected = sorted(actual_sajda_positions - sajda_positions)[:5]
        raise ValueError(
            "Sajdah markers in the Quran XML do not match quran-data.xml metadata; "
            f"missing marks at {missing}, unexpected marks at {unexpected}. "
            "Re-download Tanzil with 'Include sajdah signs (۩)' enabled."
        )

    payload: list[dict[str, object]] = []
    global_ayah = 1
    for surah_number in sorted(surah_counts):
        ayahs = quran[surah_number]
        expected_count = surah_counts[surah_number]
        if len(ayahs) != expected_count:
            raise ValueError(
                f"Surah {surah_number} contains {len(ayahs)} ayahs; metadata expects {expected_count}"
            )
        rows: list[list[object]] = []
        for ayah_number, text in enumerate(ayahs, start=1):
            juz = _value_at_position(juz_boundaries, surah_number, ayah_number)
            hizb_quarter = _value_at_position(hizb_boundaries, surah_number, ayah_number)
            sajda_flag = 1 if SAJDAH_MARK in text else 0
            rows.append([ayah_number, global_ayah, juz, hizb_quarter, sajda_flag, text])
            global_ayah += 1
        payload.append({"s": surah_number, "a": rows})
    return payload


def validate_production_payload(payload: list[dict[str, object]]) -> None:
    if len(payload) != EXPECTED_SURAH_COUNT:
        raise ValueError(f"Expected {EXPECTED_SURAH_COUNT} surahs; got {len(payload)}")
    total_ayahs = sum(len(item["a"]) for item in payload)  # type: ignore[arg-type]
    if total_ayahs != EXPECTED_AYAH_COUNT:
        raise ValueError(f"Expected {EXPECTED_AYAH_COUNT} ayahs; got {total_ayahs}")
    sajda_markers = sum(
        1 for item in payload for row in item["a"]  # type: ignore[index]
        if isinstance(row, list) and len(row) >= 6 and row[4] == 1
    )
    if sajda_markers < 4:
        raise ValueError(
            "Fewer than four sajdah markers were found. Re-download Tanzil XML and enable "
            "'Include sajdah signs (۩)' before importing."
        )
    expected_global = 1
    for expected_surah, item in enumerate(payload, start=1):
        if item.get("s") != expected_surah:
            raise ValueError(f"Surah sequence error at position {expected_surah}")
        rows = item.get("a")
        if not isinstance(rows, list):
            raise ValueError(f"Surah {expected_surah} has no ayah list")
        for expected_ayah, row in enumerate(rows, start=1):
            if not isinstance(row, list) or len(row) != 6:
                raise ValueError(f"Invalid tuple in surah {expected_surah}, ayah {expected_ayah}")
            if row[0] != expected_ayah or row[1] != expected_global:
                raise ValueError(f"Ayah numbering mismatch at global ayah {expected_global}")
            if not (1 <= int(row[2]) <= 30 and 1 <= int(row[3]) <= 240):
                raise ValueError(f"Invalid juz/hizb at global ayah {expected_global}")
            if row[4] not in (0, 1) or not isinstance(row[5], str) or not row[5].strip():
                raise ValueError(f"Invalid sajda/text value at global ayah {expected_global}")
            expected_global += 1


def atomic_write_json(path: Path, payload: list[dict[str, object]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    fd, temp_name = tempfile.mkstemp(prefix=f".{path.name}.", suffix=".tmp", dir=path.parent)
    try:
        with os.fdopen(fd, "w", encoding="utf-8", newline="\n") as stream:
            json.dump(payload, stream, ensure_ascii=False, separators=(",", ":"))
            stream.write("\n")
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(temp_name, path)
    except Exception:
        try:
            os.unlink(temp_name)
        except OSError:
            pass
        raise


def main(argv: Iterable[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--quran", type=Path, required=True,
                        help="Path to official Tanzil Uthmani Quran XML download")
    parser.add_argument("--metadata", type=Path, required=True,
                        help="Path to official Tanzil quran-data.xml metadata")
    parser.add_argument("--output", type=Path,
                        default=Path("app/src/main/assets/quran_uthmani_full.json"),
                        help="Output asset path (default: Android app assets)")
    args = parser.parse_args(argv)

    try:
        quran = parse_quran_xml(args.quran)
        counts, juz_boundaries, quarter_boundaries, sajda_positions = parse_metadata_xml(args.metadata)
        payload = build_asset_payload(
            quran, counts, juz_boundaries, quarter_boundaries, sajda_positions
        )
        validate_production_payload(payload)
        atomic_write_json(args.output, payload)
        total = sum(len(item["a"]) for item in payload)  # type: ignore[arg-type]
        print(f"OK: wrote {len(payload)} surahs / {total} ayahs to {args.output}")
        print("Notice: Ayah strings were preserved as found in the Tanzil XML source.")
        return 0
    except (ValueError, OSError, ET.ParseError) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
