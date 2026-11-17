# Round 5 engineering changelog — 2026-11-13

## Quran reading API

- Added `GET /api/v1/public/quran/surahs/{surah_number}/ayahs` with strict route and pagination validation.
- The API fails closed unless the complete 114-surah / 6,236-ayah import has complete juz, hizb and page metadata and the latest import has documented reviewer approval.
- It validates the requested surah count, contiguous ayah numbering and non-empty original Uthmani text before returning any ayah.
- Returns source name, source version and the approved import digest with each paginated response.
- Reuses the existing `quran_ayah` device-favorites type and verifies the referenced ayah server-side. Bookmark visibility and writes now require the latest import to be approved; a newer pending import hides old marks and blocks new ones. No new database migration was required.

## Flutter mobile

- Added typed Quran surah, ayah and response models.
- Added Riverpod providers for the verified surah index and one-sura reader response.
- Replaced the Quran status-only placeholder with an interactive surah index when the server reports a fully approved dataset.
- Added a Quran reader screen showing original Uthmani text, ayah numbers, juz/hizb/page metadata, sajdah indicator and source digest.
- Added per-ayah device bookmark actions, connected to the existing anonymous favorites API.
- Favorites now open the saved surah for `quran_ayah` entries.
- No offline Quran cache was introduced; caching remains disabled until source-license policy and explicit offline rights are represented in the content model.

## Test coverage

- Added a test-only synthetic structural fixture containing explicitly non-Quranic placeholder text. It exists solely to exercise the API response contract; it is removed after the test and is never shipped as application content.
- Covered fail-closed behavior, pagination validation, verified response structure, ayah favorites, latest-import approval precedence, and rejection of an incomplete surah sequence.
- Latest backend suite: 26 passing tests.

## Limitations

- No production Quran text was imported, verified or published.
- Flutter/Dart, Android SDK and Xcode are unavailable in the execution environment, so mobile static analysis, platform builds and device tests could not be run.
- PostgreSQL production integration was not run in this round.
