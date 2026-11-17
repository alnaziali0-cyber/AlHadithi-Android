#!/usr/bin/env python3
"""Import an unmodified Tanzil Uthmani TSV export after strict structural checks.

Expected format: surah|ayah|verbatim Uthmani text|juz|hizb|page|sajdah (0/1), one ayah per line.
The imported Arabic text is stored verbatim. Do not normalize, trim, or rewrite it.
"""
from __future__ import annotations
import argparse
import hashlib
from datetime import datetime, timezone
from pathlib import Path
import sys

from sqlalchemy.orm import Session

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from app.db.session import SessionLocal  # noqa: E402
from app.models import AdminUser, QuranAyah, QuranImport, QuranSurah  # noqa: E402

SURAH_NAMES_AR = [
    "الفاتحة", "البقرة", "آل عمران", "النساء", "المائدة", "الأنعام", "الأعراف", "الأنفال", "التوبة", "يونس",
    "هود", "يوسف", "الرعد", "إبراهيم", "الحجر", "النحل", "الإسراء", "الكهف", "مريم", "طه",
    "الأنبياء", "الحج", "المؤمنون", "النور", "الفرقان", "الشعراء", "النمل", "القصص", "العنكبوت", "الروم",
    "لقمان", "السجدة", "الأحزاب", "سبأ", "فاطر", "يس", "الصافات", "ص", "الزمر", "غافر",
    "فصلت", "الشورى", "الزخرف", "الدخان", "الجاثية", "الأحقاف", "محمد", "الفتح", "الحجرات", "ق",
    "الذاريات", "الطور", "النجم", "القمر", "الرحمن", "الواقعة", "الحديد", "المجادلة", "الحشر", "الممتحنة",
    "الصف", "الجمعة", "المنافقون", "التغابن", "الطلاق", "التحريم", "الملك", "القلم", "الحاقة", "المعارج",
    "نوح", "الجن", "المزمل", "المدثر", "القيامة", "الإنسان", "المرسلات", "النبأ", "النازعات", "عبس",
    "التكوير", "الانفطار", "المطففين", "الانشقاق", "البروج", "الطارق", "الأعلى", "الغاشية", "الفجر", "البلد",
    "الشمس", "الليل", "الضحى", "الشرح", "التين", "العلق", "القدر", "البينة", "الزلزلة", "العاديات",
    "القارعة", "التكاثر", "العصر", "الهمزة", "الفيل", "قريش", "الماعون", "الكوثر", "الكافرون", "النصر",
    "المسد", "الإخلاص", "الفلق", "الناس",
]
SURAH_NAMES_EN = [
    "Al-Fatihah", "Al-Baqarah", "Aal-Imran", "An-Nisa", "Al-Maidah", "Al-Anam", "Al-Araf", "Al-Anfal", "At-Tawbah", "Yunus",
    "Hud", "Yusuf", "Ar-Rad", "Ibrahim", "Al-Hijr", "An-Nahl", "Al-Isra", "Al-Kahf", "Maryam", "Ta-Ha",
    "Al-Anbiya", "Al-Hajj", "Al-Muminun", "An-Nur", "Al-Furqan", "Ash-Shuara", "An-Naml", "Al-Qasas", "Al-Ankabut", "Ar-Rum",
    "Luqman", "As-Sajdah", "Al-Ahzab", "Saba", "Fatir", "Ya-Sin", "As-Saffat", "Sad", "Az-Zumar", "Ghafir",
    "Fussilat", "Ash-Shura", "Az-Zukhruf", "Ad-Dukhan", "Al-Jathiyah", "Al-Ahqaf", "Muhammad", "Al-Fath", "Al-Hujurat", "Qaf",
    "Adh-Dhariyat", "At-Tur", "An-Najm", "Al-Qamar", "Ar-Rahman", "Al-Waqiah", "Al-Hadid", "Al-Mujadilah", "Al-Hashr", "Al-Mumtahanah",
    "As-Saff", "Al-Jumuah", "Al-Munafiqun", "At-Taghabun", "At-Talaq", "At-Tahrim", "Al-Mulk", "Al-Qalam", "Al-Haqqah", "Al-Maarij",
    "Nuh", "Al-Jinn", "Al-Muzzammil", "Al-Muddaththir", "Al-Qiyamah", "Al-Insan", "Al-Mursalat", "An-Naba", "An-Naziat", "Abasa",
    "At-Takwir", "Al-Infitar", "Al-Mutaffifin", "Al-Inshiqaq", "Al-Buruj", "At-Tariq", "Al-Ala", "Al-Ghashiyah", "Al-Fajr", "Al-Balad",
    "Ash-Shams", "Al-Layl", "Ad-Duha", "Ash-Sharh", "At-Tin", "Al-Alaq", "Al-Qadr", "Al-Bayyinah", "Az-Zalzalah", "Al-Adiyat",
    "Al-Qariah", "At-Takathur", "Al-Asr", "Al-Humazah", "Al-Fil", "Quraysh", "Al-Maun", "Al-Kawthar", "Al-Kafirun", "An-Nasr",
    "Al-Masad", "Al-Ikhlas", "Al-Falaq", "An-Nas",
]
AYAH_COUNTS = [
    7,286,200,176,120,165,206,75,129,109,123,111,43,52,99,128,111,110,98,135,112,78,118,64,77,227,
    93,88,69,60,34,30,73,54,45,83,182,88,75,85,54,53,89,59,37,35,38,29,18,45,60,49,62,55,78,96,
    29,22,24,13,14,11,11,18,12,12,30,52,52,44,28,28,20,56,40,31,50,40,46,42,29,19,36,25,22,17,19,
    26,30,20,15,21,11,8,8,19,5,8,8,11,11,8,3,9,5,4,7,3,6,3,5,4,5,6,
]
assert len(SURAH_NAMES_AR) == len(SURAH_NAMES_EN) == len(AYAH_COUNTS) == 114
assert sum(AYAH_COUNTS) == 6236


def parse_export(path: Path) -> tuple[list[tuple[int, int, str, int, int, int, bool]], str]:
    raw = path.read_bytes()
    digest = hashlib.sha256(raw).hexdigest()
    text = raw.decode("utf-8-sig")
    rows: list[tuple[int, int, str, int, int, int, bool]] = []
    expected = (1, 1)
    seen_juz: set[int] = set()
    seen_hizb: set[int] = set()
    seen_pages: set[int] = set()
    previous_structure = (0, 0, 0)
    for line_no, line in enumerate(text.splitlines(), start=1):
        if not line:
            continue
        parts = line.split("|", 6)
        if len(parts) != 7:
            raise ValueError(f"Line {line_no}: expected surah|ayah|text|juz|hizb|page|sajdah")
        try:
            surah_num, ayah_num = int(parts[0]), int(parts[1])
            juz_num, hizb_num, page_num = int(parts[3]), int(parts[4]), int(parts[5])
        except ValueError as exc:
            raise ValueError(f"Line {line_no}: invalid numeric structural metadata") from exc
        if not (1 <= surah_num <= 114) or not (1 <= ayah_num <= AYAH_COUNTS[surah_num - 1]):
            raise ValueError(f"Line {line_no}: surah/ayah outside canonical range")
        if (surah_num, ayah_num) != expected:
            raise ValueError(f"Line {line_no}: expected {expected[0]}:{expected[1]}, got {surah_num}:{ayah_num}")
        if not parts[2]:
            raise ValueError(f"Line {line_no}: empty ayah text")
        if not (1 <= juz_num <= 30 and 1 <= hizb_num <= 60 and 1 <= page_num <= 604):
            raise ValueError(f"Line {line_no}: juz/hizb/page outside expected ranges")
        if juz_num < previous_structure[0] or hizb_num < previous_structure[1] or page_num < previous_structure[2]:
            raise ValueError(f"Line {line_no}: juz/hizb/page metadata must be monotonic")
        previous_structure = (juz_num, hizb_num, page_num)
        seen_juz.add(juz_num)
        seen_hizb.add(hizb_num)
        seen_pages.add(page_num)
        sajdah_value = parts[6].strip()
        if sajdah_value not in {"0", "1"}:
            raise ValueError(f"Line {line_no}: sajdah must be 0 or 1")
        rows.append((surah_num, ayah_num, parts[2], juz_num, hizb_num, page_num, sajdah_value == "1"))
        if ayah_num == AYAH_COUNTS[surah_num - 1]:
            expected = (surah_num + 1, 1)
        else:
            expected = (surah_num, ayah_num + 1)
    if len(rows) != 6236 or expected != (115, 1):
        raise ValueError(f"Expected 6,236 ordered ayahs in 114 surahs; found {len(rows)}")
    if len({(row[0], row[1]) for row in rows}) != 6236:
        raise ValueError("Duplicate surah/ayah key")
    if seen_juz != set(range(1, 31)) or seen_hizb != set(range(1, 61)) or seen_pages != set(range(1, 605)):
        raise ValueError("Structural metadata must cover all 30 juz, 60 hizb and 604 mushaf pages")
    return rows, digest

def import_rows(db: Session, rows: list[tuple[int, int, str, int, int, int, bool]], digest: str, source_version: str, approved_by: int, reviewer_name: str, reviewer_credential: str, review_notes: str) -> None:
    reviewer = db.get(AdminUser, approved_by)
    if not reviewer or reviewer.role != "owner" or not reviewer.is_active:
        raise RuntimeError("--approved-by must be an active owner account ID")
    if db.query(QuranAyah).count() or db.query(QuranSurah).count():
        raise RuntimeError("Quran tables are not empty. Use a reviewed migration/restore procedure; import never overwrites existing Quran data.")
    for i, (name_ar, name_en, ayah_count) in enumerate(zip(SURAH_NAMES_AR, SURAH_NAMES_EN, AYAH_COUNTS), start=1):
        db.add(QuranSurah(number=i, name_ar=name_ar, name_en=name_en, ayah_count=ayah_count))
    db.flush()
    # Store each ayah text verbatim; structural metadata can be imported separately.
    db.add_all([QuranAyah(surah_number=s, ayah_number=a, text_uthmani=t, juz_number=j, hizb_number=h, page_number=p, sajdah=sajdah) for s, a, t, j, h, p, sajdah in rows])
    db.add(QuranImport(
        source_name="Tanzil Quran Text", source_version=source_version,
        license_text=("Copyright (C) 2007-2026 Tanzil Project; License: CC BY 3.0. "
                      "Verbatim redistribution only; text must not be changed. Source: https://tanzil.net/"),
        text_sha256=digest, surah_count=114, ayah_count=6236, approved_by=approved_by,
        reviewer_name=reviewer_name, reviewer_credential=reviewer_credential,
        review_notes=review_notes, reviewed_at=datetime.now(timezone.utc),
    ))
    db.commit()


def main() -> None:
    parser = argparse.ArgumentParser(description="Validate and import a verbatim Tanzil Uthmani text export joined to structural metadata")
    parser.add_argument("file", type=Path)
    parser.add_argument("--version", default="1.1")
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--approved-by", type=int, help="ID of the active owner account recording this approval")
    parser.add_argument("--reviewer-name", help="Name of the qualified Quran reviewer who inspected this exact snapshot")
    parser.add_argument("--reviewer-credential", help="Institution/qualification details for the reviewer")
    parser.add_argument("--review-notes", default="", help="Review notes and reference mushaf details")
    args = parser.parse_args()
    rows, digest = parse_export(args.file)
    print(f"Validated: 114 surahs, {len(rows)} ayahs, structural metadata present, SHA-256 {digest}")
    if args.dry_run:
        print("Dry run only; database unchanged.")
        return
    if not args.approved_by or not args.reviewer_name or not args.reviewer_credential:
        parser.error("--approved-by, --reviewer-name and --reviewer-credential are required for a reviewed import; dry-run does not need them")
    db = SessionLocal()
    try:
        import_rows(db, rows, digest, args.version, args.approved_by, args.reviewer_name, args.reviewer_credential, args.review_notes)
    finally:
        db.close()
    print("Import committed. Verify /api/v1/public/quran/status before enabling the Quran reader.")


if __name__ == "__main__":
    main()
