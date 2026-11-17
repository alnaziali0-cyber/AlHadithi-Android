import collections
import hashlib
import json
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[3]
ASSET_PATH = PROJECT_ROOT / "apps/mobile/assets/data/quran-hafs-rasm_uthmani.json"
EXPECTED_ASSET_SHA256 = "d3865331602f30d919a872c7766c6043de7dc5101276da871c3b9dd422f798e0"
EXPECTED_SOURCE_SHA256 = "cdec7341b7c684e7b8dd469c4b68988914d2784e924c7e6cbb9cb4b57c24f013"
EXPECTED_SURAH_AYAH_COUNTS = [
    7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128,
    111, 110, 98, 135, 112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73,
    54, 45, 83, 182, 88, 75, 85, 54, 53, 89, 59, 37, 35, 38, 29, 18, 45, 60,
    49, 62, 55, 78, 96, 29, 22, 24, 13, 14, 11, 11, 18, 12, 12, 30, 52, 52,
    44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42, 29, 19, 36, 25, 22, 17, 19,
    26, 30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11, 11, 8, 3, 9, 5, 4, 7, 3,
    6, 3, 5, 4, 5, 6,
]


def test_quran_hafs_asset_is_the_unchanged_source_file():
    raw = ASSET_PATH.read_bytes()
    assert hashlib.sha256(raw).hexdigest() == EXPECTED_ASSET_SHA256

    data = json.loads(raw.decode("utf-8"))
    meta = data["meta"]
    assert meta["project"] == "https://github.com/quran-ws/quran-text"
    assert meta["source"]["package"] == "UthmanicHafs-v-3.0.zip"
    assert meta["source"]["sha256"] == EXPECTED_SOURCE_SHA256
    assert "KFGQPC" in meta["license"]
    assert meta["edition"]["key"] == "hafs"
    assert meta["edition"]["counting_system"] == "kufi"


def test_quran_hafs_asset_has_all_surahs_and_sequential_ayahs():
    data = json.loads(ASSET_PATH.read_text(encoding="utf-8"))
    ayahs = data["ayahs"]
    assert len(ayahs) == data["meta"]["records"] == 6236

    counts = collections.Counter()
    for row in ayahs:
        assert set(row) == {"surah", "ayah", "text"}
        assert isinstance(row["surah"], int) and 1 <= row["surah"] <= 114
        assert isinstance(row["ayah"], int) and row["ayah"] > 0
        assert isinstance(row["text"], str) and row["text"].strip()
        counts[row["surah"]] += 1
        assert row["ayah"] == counts[row["surah"]]

    assert sorted(counts) == list(range(1, 115))
    assert [counts[n] for n in range(1, 115)] == EXPECTED_SURAH_AYAH_COUNTS
    assert ayahs[0]["surah"] == ayahs[0]["ayah"] == 1
    assert ayahs[-1]["surah"] == 114 and ayahs[-1]["ayah"] == 6
