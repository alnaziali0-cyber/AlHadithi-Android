from pathlib import Path
import pytest
from scripts.import_tanzil import AYAH_COUNTS, parse_export


def test_tanzil_import_rejects_incomplete_dataset(tmp_path: Path):
    fixture = tmp_path / "partial.txt"
    fixture.write_text("1|1|بِسْمِ اللَّهِ|1|1|1|0\n", encoding="utf-8")
    with pytest.raises(ValueError):
        parse_export(fixture)


def test_complete_structural_snapshot_passes_without_modifying_text(tmp_path: Path):
    fixture = tmp_path / "synthetic-structure-only.txt"
    rows = []
    index = 0
    for surah, count in enumerate(AYAH_COUNTS, start=1):
        for ayah in range(1, count + 1):
            juz = min(30, (index * 30) // 6236 + 1)
            hizb = min(60, (index * 60) // 6236 + 1)
            page = min(604, (index * 604) // 6236 + 1)
            rows.append(f"{surah}|{ayah}|نص اختبار لا يُستخدم في التطبيق|{juz}|{hizb}|{page}|0")
            index += 1
    fixture.write_text("\n".join(rows) + "\n", encoding="utf-8")
    parsed, digest = parse_export(fixture)
    assert len(parsed) == 6236
    assert parsed[0][2] == "نص اختبار لا يُستخدم في التطبيق"
    assert parsed[-1][0:2] == (114, 6)
    assert len(digest) == 64
