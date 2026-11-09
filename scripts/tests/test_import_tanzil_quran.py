import tempfile
import unittest
from pathlib import Path

from scripts.import_tanzil_quran import (
    build_asset_payload,
    parse_metadata_xml,
    parse_quran_xml,
    validate_production_payload,
)


class TanzilImporterTests(unittest.TestCase):
    def test_preserves_ayah_string_and_assigns_structural_fields(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            quran_path = root / "quran.xml"
            metadata_path = root / "metadata.xml"
            exact_ayah = "قَالَ رَبِّ ٱشْرَحْ لِي صَدْرِي ۩"
            quran_path.write_text(
                '<quran><sura index="1"><aya index="1" text="بِسْمِ اللَّهِ"/></sura>'
                '<sura index="2"><aya index="1" text="' + exact_ayah + '"/></sura></quran>',
                encoding="utf-8",
            )
            # A minimal, synthetic 2-surah metadata fixture verifies parsing only.
            # It is not Quran content and is never used to build a production asset.
            metadata_path.write_text(
                '<root><suras><sura index="1" ayas="1"/><sura index="2" ayas="1"/></suras>'
                '<juzs><juz index="1" sura="1" aya="1"/></juzs>'
                '<quarters><quarter index="1" sura="1" aya="1"/></quarters>'
                '<sajdas><sajda index="1" sura="2" aya="1" type="optional"/></sajdas></root>',
                encoding="utf-8",
            )
            quran = parse_quran_xml(quran_path)
            counts, juz, quarters, sajdas = parse_metadata_xml(
                metadata_path, expected_juz_count=1, expected_hizb_quarter_count=1
            )
            payload = build_asset_payload(quran, counts, juz, quarters, sajdas)

            self.assertEqual(exact_ayah, payload[1]["a"][0][5])
            self.assertEqual(1, payload[1]["a"][0][4])
            self.assertEqual(2, payload[1]["a"][0][1])


    def test_accepts_tanzil_hizbs_container_with_quarter_children(self):
        with tempfile.TemporaryDirectory() as temp:
            metadata_path = Path(temp) / "metadata.xml"
            metadata_path.write_text(
                '<root><suras><sura index="1" ayas="1"/></suras>'
                '<juzs><juz index="1" sura="1" aya="1"/></juzs>'
                '<hizbs><hizb index="1"><quarter index="1" sura="1" aya="1"/></hizb></hizbs>'
                '<sajdas><sajda index="1" sura="1" aya="1"/></sajdas></root>',
                encoding="utf-8",
            )
            counts, juz, quarters, sajdas = parse_metadata_xml(
                metadata_path, expected_juz_count=1, expected_hizb_quarter_count=1
            )
            self.assertEqual(1, len(counts))
            self.assertEqual([(1, 1, 1)], juz)
            self.assertEqual([(1, 1, 1)], quarters)
            self.assertEqual({(1, 1)}, sajdas)

    def test_rejects_metadata_boundary_outside_surah(self):
        with tempfile.TemporaryDirectory() as temp:
            metadata_path = Path(temp) / "metadata.xml"
            metadata_path.write_text(
                '<root><suras><sura index="1" ayas="1"/></suras>'
                '<juzs><juz index="1" sura="1" aya="1"/><juz index="2" sura="1" aya="2"/></juzs>'
                '<quarters><quarter index="1" sura="1" aya="1"/><quarter index="2" sura="1" aya="2"/></quarters>'
                '<sajdas><sajda index="1" sura="1" aya="1"/></sajdas></root>',
                encoding="utf-8",
            )
            with self.assertRaisesRegex(ValueError, "outside the Quran"):
                parse_metadata_xml(
                    metadata_path, expected_juz_count=2, expected_hizb_quarter_count=2
                )


    def test_rejects_sajda_marker_set_that_disagrees_with_metadata(self):
        quran = {1: ["نَصٌّ عَرَبِيٌّ ۩"]}
        counts = {1: 1}
        juz = [(1, 1, 1)]
        quarters = [(1, 1, 1)]
        with self.assertRaisesRegex(ValueError, "do not match"):
            build_asset_payload(quran, counts, juz, quarters, set())

    def test_rejects_unexpected_source_sequence(self):
        with tempfile.TemporaryDirectory() as temp:
            quran_path = Path(temp) / "quran.xml"
            quran_path.write_text(
                '<quran><sura index="1"><aya index="2" text="نَصٌّ عَرَبِيٌّ"/></sura></quran>',
                encoding="utf-8",
            )
            with self.assertRaisesRegex(ValueError, "expected ayah 1"):
                parse_quran_xml(quran_path)

    def test_production_validator_rejects_non_production_fixture(self):
        with self.assertRaisesRegex(ValueError, "Expected 114 surahs"):
            validate_production_payload([{"s": 1, "a": [[1, 1, 1, 1, 0, "نص عربي"]]}])

    def test_production_validator_rejects_missing_sajdah_signs(self):
        payload = []
        global_ayah = 1
        for surah in range(1, 115):
            count = 55 if surah <= 80 else 54
            rows = []
            for ayah in range(1, count + 1):
                rows.append([ayah, global_ayah, 1, 1, 0, "نص عربي تجريبي"])
                global_ayah += 1
            payload.append({"s": surah, "a": rows})
        with self.assertRaisesRegex(ValueError, "sajdah markers"):
            validate_production_payload(payload)


if __name__ == "__main__":
    unittest.main()
