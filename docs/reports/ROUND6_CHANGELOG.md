# Round 6 changelog — 2026-11-13

## Supplied requirements pack

- Inspected all 50 entries in `KHAZAIN_AL_HUDA_requirements_pack.zip` and preserved the complete pack under `docs/reference/requirements-pack/`.
- Copied the supplied source catalogue (JSON/CSV) and Iraqi city seed files (JSON/CSV) into `content/reference/`.
- Bundled the versioned source catalogue and city seed JSON inside `services/api/app/data/` so they are included in the API Docker image.
- Added `content/reference/MANIFEST.json` with SHA-256 hashes and explicit verification counts.
- Added `docs/reports/ROUND6_REQUIREMENTS_INTEGRATION.md` documenting exactly what was imported and what the original pack did not contain.

## Reference catalogue in the application

- Added `GET /api/v1/public/reference/sources`, exposing only safe public metadata for the 9 supplied sources. Contact email fields are not returned.
- Added a Flutter “المصادر والتوثيق” screen in Settings that shows the source category, observed information, intended use, official site link and rights status.
- The screen explicitly warns that a public webpage or available stream does not grant permission to republish, stream, download or cache content.

## Iraqi city-level prayer support

- Added the `PrayerCity` SQLAlchemy model and migration `0005_prayer_cities`.
- Added an idempotent seed loader that inserts records only after the governorate registry exists. It uses SQLite/PostgreSQL conflict-safe inserts and is bundled with the API package.
- Imported 20 city rows covering 19 governorates, including both Tikrit and Samarra under Salah al-Din.
- Added `GET /api/v1/public/prayer/cities` with optional governorate and text filters and a bounded search term.
- Extended `GET /api/v1/public/prayer/times` to accept `city_id`, reject conflicting city/governorate selectors and return provenance, timezone, coordinate accuracy and verification state.
- Changed the Flutter prayer screen to select a city and call the city-based API.
- Namespaced the prayer cache by city ID as well as coordinates to prevent one city's cached metadata from being returned for another location.
- Both built-in calculation presets now explicitly report `officially_verified=false`. The UI warns that the angle profiles are not yet approved by a religious authority and city-center coordinates are approximate.

## Preserved fail-closed behavior

- No actual audio, adhan recording, Quran recitation, majlis recording, logo, radio image, or direct stream URL was in the uploaded pack. No placeholder media has been substituted.
- No authority-approved prayer profile or timetable was included.
- No Quran text snapshot or verified book/hadith corpus was included.
- No licence or permission record was upgraded to approved based only on an official website appearing in the source catalogue.
- The supplied PostgreSQL schema remains a reference draft; Alembic migrations remain the project's executable schema history.

## Validation

See `VALIDATION_REPORT.md`. Python test suite: 29 passed. Alembic revision 0005 upgraded, downgraded and upgraded again; schema drift check reported no new operations. API smoke test confirmed the migrated database bootstraps 20 cities and serves 9 public source records. Flutter/Dart and Docker were not available for native/client or container builds.
