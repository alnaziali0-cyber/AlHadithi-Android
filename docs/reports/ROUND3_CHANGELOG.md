# Round 3 engineering changelog — 13 November 2026

## Delivered

- Owner-only CRUD-style CMS routes for book, hadith source, hadith, dua, visit, event, ethics lesson and audio records.
- Strict server-side payload schemas reject unknown fields and validate URLs, identifiers, publication state, references and rights evidence.
- Append-only hadith grading route keeps scholarly assessment separate from narration text and chain.
- Transactional book-page batch import (up to 250 pages / 2,000,000 aggregate characters), collision-safe rollback, volume-bound validation, paginated admin inspection, and page update only while parent book is not published.
- Admin console tab for JSON record creation/editing/archive, book-page import and grading submission.
- Corrected password-rotation tab visibility / loader condition and kept markup compatible with the strict Content Security Policy.
- Pydantic validation errors return JSON-safe 422 details; admin UI renders those messages without injecting HTML.
- Offline audio-cache publication output now also checks download permission and an HTTPS download URL.

## Validation performed

- `PYTHONPATH=. pytest -q`: 24 passed.
- `python -m compileall -q services/api/app services/api/scripts services/api/alembic services/api/tests`: passed.
- `node --check apps/admin/admin.js`: passed.
- Fresh SQLite migration cycle: Alembic upgrade to head, `alembic check`, downgrade to base, and upgrade to head: passed.
- Dashboard markup contains no inline style attributes that conflict with `style-src 'self'`.
- Original logo SHA-256 compared to supplied source archive: unchanged.

## Still outstanding

This round does not complete the Flutter / native platform / production release scope. Flutter and Dart SDKs, Docker, PostgreSQL, macOS/Xcode, production credentials, real source licenses and verified religious/audio datasets were not present. Granular editorial roles, multi-reviewer workflow, rich per-content editors, Quran approval CMS, notification scheduling, user account lifecycle and audio playback controls remain unfinished.
