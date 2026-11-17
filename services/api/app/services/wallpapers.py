"""Fixed, locally bundled wallpaper metadata and owner-managed visibility."""

from sqlalchemy.orm import Session

from app.models import AppSetting

SETTING_KEY = "wallpaper_visibility"

# Only these reviewed, bundled assets can be exposed by this feature. The API
# never accepts arbitrary image paths or remote source URLs.
WALLPAPER_CATALOG = (
    {
        "id": "karbala-shrine-night",
        "title": "مرقد الإمام الحسين عليه السلام ليلاً",
        "creator": "Bahr Alwatifi",
        "license": "CC BY-SA 4.0",
        "sha256": "0a770423b324c859275d5f825cc14f675a4c0e77f9a7ef52560d2091f4b7d301",
        "default_published": True,
    },
    {
        "id": "najaf-imam-ali-shrine",
        "title": "مرقد الإمام علي عليه السلام",
        "creator": "khosh1341",
        "license": "CC BY 3.0",
        "sha256": "503ada65e0e88010e4e66ba14ce95f987dce73556db5264db16df57388a65735",
        "default_published": True,
    },
)


def list_wallpapers(db: Session) -> list[dict]:
    setting = db.get(AppSetting, SETTING_KEY)
    stored = setting.value.get("published", {}) if setting and isinstance(setting.value, dict) else {}
    if not isinstance(stored, dict):
        stored = {}
    result = []
    for item in WALLPAPER_CATALOG:
        row = dict(item)
        override = stored.get(item["id"])
        row["published"] = override if isinstance(override, bool) else item["default_published"]
        result.append(row)
    return result


def set_wallpaper_published(db: Session, wallpaper_id: str, published: bool) -> dict | None:
    if not any(item["id"] == wallpaper_id for item in WALLPAPER_CATALOG):
        return None
    states = {item["id"]: item["published"] for item in list_wallpapers(db)}
    states[wallpaper_id] = published
    setting = db.get(AppSetting, SETTING_KEY)
    if setting is None:
        setting = AppSetting(key=SETTING_KEY, value={"published": states})
        db.add(setting)
    else:
        setting.value = {"published": states}
    return next({**item, "published": states[item["id"]]} for item in WALLPAPER_CATALOG if item["id"] == wallpaper_id)
