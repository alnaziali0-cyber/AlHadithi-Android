from app.models import Governorate

# Centers are approximate city-center coordinates; exact GPS is optional and user-controlled.
GOVERNORATES = [
    ("baghdad", "بغداد", "Baghdad", 33.3152, 44.3661),
    ("basra", "البصرة", "Basra", 30.5085, 47.7804),
    ("nineveh", "نينوى", "Nineveh", 36.3350, 43.1189),
    ("erbil", "أربيل", "Erbil", 36.1901, 44.0091),
    ("sulaymaniyah", "السليمانية", "Sulaymaniyah", 35.5613, 45.4309),
    ("duhok", "دهوك", "Duhok", 36.8671, 42.9507),
    ("halabja", "حلبجة", "Halabja", 35.1778, 45.9861),
    ("kirkuk", "كركوك", "Kirkuk", 35.4681, 44.3922),
    ("diyala", "ديالى", "Diyala", 33.7466, 44.6434),
    ("saladin", "صلاح الدين", "Saladin", 34.6071, 43.6782),
    ("anbar", "الأنبار", "Al-Anbar", 33.4206, 43.3078),
    ("babil", "بابل", "Babil", 32.4800, 44.4300),
    ("karbala", "كربلاء", "Karbala", 32.6160, 44.0249),
    ("najaf", "النجف", "Najaf", 32.0003, 44.3354),
    ("qadisiyyah", "القادسية", "Al-Qadisiyyah", 31.9892, 44.9256),
    ("muthanna", "المثنى", "Al-Muthanna", 31.3186, 45.2806),
    ("dhi_qar", "ذي قار", "Dhi Qar", 31.0579, 46.2573),
    ("maysan", "ميسان", "Maysan", 31.8358, 47.1448),
    ("wasit", "واسط", "Wasit", 32.5128, 45.8182),
]


def seed_governorates(db) -> None:
    for item in GOVERNORATES:
        slug, name_ar, name_en, lat, lon = item
        existing = db.get(Governorate, slug)
        if existing is None:
            db.add(Governorate(
                id=slug, name_ar=name_ar, name_en=name_en,
                latitude=lat, longitude=lon, timezone="Asia/Baghdad",
                coordinates_quality="approximate_governorate_center",
            ))
    db.commit()
