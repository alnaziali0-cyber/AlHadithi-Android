import hashlib
import json
import unittest
from pathlib import Path

from scripts.import_tanzil_quran import build_asset_payload, parse_metadata_xml, validate_production_payload
from scripts.import_tanzil_simple_quran import parse_quran_simple_text, validate_metadata_offsets

ROOT = Path(__file__).resolve().parents[2]
SOURCE_DIR = ROOT / "scripts" / "source"
ASSETS = ROOT / "app" / "src" / "main" / "assets"


class ActualTanzilCorpusIntegrityTests(unittest.TestCase):
    def test_uploaded_source_rebuilds_the_bundled_asset_exactly(self):
        metadata_path = SOURCE_DIR / "quran-data.xml"
        text_path = SOURCE_DIR / "quran-simple.txt"
        counts, juz, quarters, sajdas = parse_metadata_xml(metadata_path)
        quran = parse_quran_simple_text(text_path, counts)
        validate_metadata_offsets(metadata_path, counts)
        payload = build_asset_payload(quran, counts, juz, quarters, sajdas)
        validate_production_payload(payload)

        asset_path = ASSETS / "quran_simple_full.json"
        bundled = json.loads(asset_path.read_text(encoding="utf-8"))
        self.assertEqual(114, len(payload))
        self.assertEqual(6236, sum(len(item["a"]) for item in payload))
        self.assertEqual(payload, bundled)
        self.assertEqual(15, sum(row[4] for item in bundled for row in item["a"]))

    def test_license_notice_is_preserved_verbatim_from_source(self):
        raw_source = (SOURCE_DIR / "quran-simple.txt").read_bytes()
        marker = b"# PLEASE DO NOT REMOVE OR CHANGE THIS COPYRIGHT BLOCK"
        self.assertIn(marker, raw_source)
        source_notice = raw_source[raw_source.find(marker):]
        bundled_notice = (ASSETS / "licenses" / "TANZIL_QURAN_SIMPLE_NOTICE.txt").read_bytes()
        self.assertEqual(source_notice, bundled_notice)

    def test_manifest_checksums_match_all_bundled_and_source_files(self):
        manifest = json.loads((ASSETS / "quran_simple_manifest.json").read_text(encoding="utf-8"))
        files = {
            "uploadedSourceSha256": SOURCE_DIR / "quran-simple.txt",
            "metadataSourceSha256": SOURCE_DIR / "quran-data.xml",
            "bundledJsonSha256": ASSETS / "quran_simple_full.json",
        }
        for key, path in files.items():
            with self.subTest(key=key):
                self.assertEqual(manifest[key], hashlib.sha256(path.read_bytes()).hexdigest())


if __name__ == "__main__":
    unittest.main()
