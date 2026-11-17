# Architecture Decision Record — Khazain Al-Huda

## Selected stack

- **Mobile:** Flutter / Dart, feature-first folders, Riverpod for state, GoRouter for navigation, Dio for API, SharedPreferences for small offline preferences and prayer-day cache.
- **Backend:** FastAPI modular monolith, SQLAlchemy 2.x, PostgreSQL, Alembic migrations.
- **Admin:** same-origin RTL admin console served by the API; admin mutations are server-authorized and audited.
- **Deployment:** Docker Compose, private PostgreSQL network, persistent media volume, Caddy TLS termination, separate CMS publication worker.
- **Audio:** `just_audio` currently supports foreground playback when the API exposes a verified stream. Background playback is deliberately not enabled before `audio_service` and native lifecycle configuration are completed.

## Why a modular monolith first

The product has tightly related content, timetable, permissions, user-preference and editorial domains. A modular monolith provides transaction boundaries, simpler deployment and easier auditability at the current stage. Services are separated by domain inside the API; the mobile client depends on versioned HTTP contracts, not database internals. Individual domains can be extracted later if scale or operational isolation justifies it.

## Data flow

1. Flutter requests public, read-only versioned endpoints.
2. Public prayer-time requests are calculated server-side using a single selected method and cached in `prayer_cache`.
3. The mobile client stores the last successful daily response locally to display stale/offline data with an explicit stale marker.
4. Admin users authenticate through a server-side opaque session cookie. The cookie is HttpOnly, Secure in production and SameSite=Strict; mutating routes also require a session-bound CSRF token.
5. Admin content is stored as draft/scheduled/published/paused/archived. A separate worker promotes due items and archives expired items.
6. Media files are stored under UUID names after size and magic-header checks; metadata and SHA-256 are persisted in PostgreSQL.
7. Scholarly text datasets are ingested from source snapshots and carry source, license, version and hash metadata. Content is not considered publishable merely because it exists in a table.

## Domain boundaries

- `services/prayer_times.py`: solar-time computation and method parameter metadata.
- `services/governorates.py`: Iraq administrative-center registry; coordinates are approximate.
- `QuranSurah` / `QuranAyah` / `QuranImport`: immutable imported Quran text plus an approval record for the exact file hash.
- Books, hadith, dua/visits, audio, events and ethics: normalized storage tables with read-only public catalog endpoints. Each endpoint filters unpublished rows; books/audio additionally require an explicit reusable-rights state, audio requires rights evidence and an HTTPS source, and duas/visits/events require recorded citations or historical sources. Hadith grading records remain separate from the narration text and are returned as attributed scholarly statements, never collapsed into an automatic authenticity verdict. Full specialist-domain admin CRUD and immutable reviewer approval workflows remain subsequent work.
- Admin authentication, sessions, throttles, media and audit logs: privileged server-only operations.

## Decisions intentionally deferred

- Managed object storage/CDN (S3-compatible) rather than local disk for multi-node production.
- FCM/APNs provider credentials and delivery worker.
- Account registration / password recovery / OAuth and cross-device sync.
- Per-identity scholar review workflow with multiple reviewers and immutable approval signatures.
- A dedicated search engine (PostgreSQL full text is the initial candidate); current catalog search uses escaped SQL `ILIKE` terms with bounded pagination.
- Full native scheduling for adhan/audio on Android and iOS.

## Key quality gates

No religious content goes public until it has a source citation, edition/version and editorial approval. No audio goes live until the direct URL, source attribution and reuse permissions have been verified. Prayer calculations expose method and definitions and must be compared to trusted local calendars before production release.
