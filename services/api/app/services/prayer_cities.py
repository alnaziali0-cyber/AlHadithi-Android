"""Seed the versioned Iraqi city coordinate catalogue.

Coordinates are approximate city centers supplied by the requirements pack.
They are explicitly unverified and must not be represented as mosque-level GPS.
"""
import json
from pathlib import Path

from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.dialects.sqlite import insert as sqlite_insert
from sqlalchemy.orm import Session

from app.models import Governorate, PrayerCity


GOVERNORATE_IDS = {
    "بغداد": "baghdad",
    "بابل": "babil",
    "كربلاء": "karbala",
    "النجف": "najaf",
    "البصرة": "basra",
    "نينوى": "nineveh",
    "الأنبار": "anbar",
    "صلاح الدين": "saladin",
    "ديالى": "diyala",
    "واسط": "wasit",
    "القادسية": "qadisiyyah",
    "ذي قار": "dhi_qar",
    "ميسان": "maysan",
    "المثنى": "muthanna",
    "كركوك": "kirkuk",
    "أربيل": "erbil",
    "السليمانية": "sulaymaniyah",
    "دهوك": "duhok",
    "حلبجة": "halabja",
}


def seed_prayer_cities(db: Session) -> int:
    seed_file = Path(__file__).resolve().parents[1] / "data" / "iraq_cities.seed.json"
    records = json.loads(seed_file.read_text(encoding="utf-8"))
    if not isinstance(records, list):
        raise RuntimeError("Prayer city seed file must contain a JSON array")

    dialect = db.get_bind().dialect.name
    inserted = 0
    try:
        for record in records:
            if not isinstance(record, dict):
                raise RuntimeError("Prayer city seed contains a non-object record")
            governorate_id = GOVERNORATE_IDS.get(record.get("governorate", ""))
            if governorate_id is None or db.get(Governorate, governorate_id) is None:
                raise RuntimeError(f"Unknown governorate in prayer city seed: {record.get('governorate')!r}")
            values = {
                "city_id": record["city_id"],
                "governorate_id": governorate_id,
                "governorate_name_ar": record["governorate"],
                "city_name_ar": record["city_name_ar"],
                "city_name_en": record["city_name_en"],
                "latitude": float(record["latitude"]),
                "longitude": float(record["longitude"]),
                "timezone": record["timezone"],
                "coordinate_source": record["coordinate_source"],
                "coordinate_accuracy": record["coordinate_accuracy"],
                "verified": bool(record["verified"]),
            }
            if dialect == "postgresql":
                statement = pg_insert(PrayerCity).values(**values).on_conflict_do_nothing(
                    index_elements=["city_id"]
                )
                result = db.execute(statement)
            elif dialect == "sqlite":
                statement = sqlite_insert(PrayerCity).values(**values).on_conflict_do_nothing(
                    index_elements=["city_id"]
                )
                result = db.execute(statement)
            else:
                if db.get(PrayerCity, values["city_id"]) is None:
                    db.add(PrayerCity(**values))
                    db.flush()
                    inserted += 1
                continue
            inserted += max(0, result.rowcount or 0)
        db.commit()
        return inserted
    except Exception:
        db.rollback()
        raise
