import tempfile
import unittest
from pathlib import Path

from scripts.import_tanzil_simple_quran import parse_quran_simple_text


class TanzilSimpleImporterTests(unittest.TestCase):
    def _write(self, directory: str, content: str) -> Path:
        path = Path(directory) / "quran-simple.txt"
        path.write_text(content, encoding="utf-8")
        return path

    def test_preserves_text_marks_and_basmala_verbatim(self):
        with tempfile.TemporaryDirectory() as temp:
            text = "بِسْمِ اللَّهِ الرَّحْمَـٰنِ الرَّحِيمِ الم ۞"  # synthetic parser fixture
            path = self._write(temp, f"1|1|{text}\n2|1|بِسْمِ اللَّهِ رَحْمَٰنِ رَحِيمٍ ۩\n")
            result = parse_quran_simple_text(path)
            self.assertEqual(text, result[1][0])
            self.assertEqual("بِسْمِ اللَّهِ رَحْمَٰنِ رَحِيمٍ ۩", result[2][0])

    def test_rejects_duplicate_ayah(self):
        with tempfile.TemporaryDirectory() as temp:
            path = self._write(temp, "1|1|نص عربي\n1|1|نص عربي آخر\n")
            with self.assertRaisesRegex(ValueError, "Duplicate"):
                parse_quran_simple_text(path)

    def test_rejects_out_of_order_ayahs(self):
        with tempfile.TemporaryDirectory() as temp:
            path = self._write(temp, "1|1|نص أول\n1|3|نص ثالث\n1|2|نص ثانٍ\n")
            with self.assertRaisesRegex(ValueError, "strict surah/ayah order"):
                parse_quran_simple_text(path)

    def test_skips_only_blank_and_copyright_comment_lines(self):
        with tempfile.TemporaryDirectory() as temp:
            path = self._write(
                temp,
                "1|1|نص عربي\n\n# PLEASE DO NOT REMOVE OR CHANGE THIS COPYRIGHT BLOCK\n"
            )
            self.assertEqual({1: ["نص عربي"]}, parse_quran_simple_text(path))

    def test_rejects_non_arabic_or_empty_text(self):
        with tempfile.TemporaryDirectory() as temp:
            path = self._write(temp, "1|1|   \n")
            with self.assertRaisesRegex(ValueError, "Empty ayah text"):
                parse_quran_simple_text(path)
            path.write_text("1|1|text only\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "No Arabic characters"):
                parse_quran_simple_text(path)


if __name__ == "__main__":
    unittest.main()
