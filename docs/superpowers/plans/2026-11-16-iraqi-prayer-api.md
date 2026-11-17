# Iraqi Prayer API Integration Implementation Plan

> **For agentic workers:** Follow the applicable Superpowers workflow skill and the approved design in `docs/superpowers/specs/2026-11-16-prayer-times-api-design.md`. Steps use checkboxes.

**Goal:** Make the user-selected MisrQuran API the default source for today’s prayer times for the selected Iraqi location, and retain the existing calculation for future notification days.

**Architecture:** Add a dedicated Dio client without the app’s device-identification header for `https://api.misrquran.gov.eg`. Parse the provider’s UTC clock strings into `Asia/Baghdad` once, use keyed local cache for API results, and fall back to a clearly labeled cached/local result. Keep the existing local provider for future dates; feed the same today result to the prayer screen, home summary, and today’s notification.

**Tech Stack:** Flutter/Dart, Riverpod, Dio, `timezone`, SharedPreferences; existing FastAPI service and pytest audit suite.

**Spec:** `docs/superpowers/specs/2026-11-16-prayer-times-api-design.md` (the user approved API-default, today-API/future-local, and sharing precise GPS with this API).

## Global Constraints

- Use only the API parameters `lat`, `lng`, and `dayLightSaving=false`; never use the sample coordinates `1,1`.
- Convert UTC to `Asia/Baghdad` once and handle local-day rollover.
- Do not send the app’s `X-Device-Key`, log exact GPS coordinates, or put coordinates in local preference/cache keys.
- For manual city/governorate selection, use the existing chosen center; for GPS, send the precise coordinates only because the user explicitly approved this API recipient.
- Today’s screen, home summary, and today’s local alert use the same result. The existing local calculation remains the source for the next 11 days’ alerts.
- Clearly label provider/fallback and the non-authoritative calculation status. Do not infer a Ja'fari authority or claim official approval.
- Preserve existing content, Quran assets/hashes, database, package identity, and permissions. Do not generate platform folders or modify signing configuration.
- The uploaded ZIP has no `.git` metadata, Android/iOS directories, or Flutter/Dart SDK in this environment; work only in the extracted copy and report native checks unavailable unless the SDK is safely installed.

## Review Focus

- Invalid/missing JSON fields or non-200 responses: show a clear error, then use cached/local fallback without crashing.
- UTC conversion at times near midnight: the date shown and alert time must be the correct Baghdad local date.
- API `sunset` and `maghrib` may match; display only supplied fields and never synthesize missing Islamic-midnight values from another source.
- Stale API cache: preserve its service date and stale marker; never display it as fresh today.
- Exact GPS values: send only in the user-approved outbound request, and never persist them in the API cache key or debug output.

---

### Task 1: Build and test the API parser/repository

**Files:**
- Create: `apps/mobile/lib/core/misr_quran_prayer_times.dart`
- Create: `apps/mobile/test/core/misr_quran_prayer_times_test.dart`
- Modify: `apps/mobile/lib/core/models.dart` only if the current result type needs explicit source/Hijri metadata.

**Interfaces:**
- Produces a repository method taking latitude/longitude and returning a validated today result; it uses a dedicated Dio client and exposes no raw coordinate in logs/cache.

- [x] Write tests for a known UTC sample converting to Baghdad; response parsing for the seven returned clock fields and Arabic Hijri date; missing/null/malformed time; invalid HTTP/JSON; and local-day boundary.
- [x] Run the targeted test and confirm it fails for missing behavior.
- [x] Implement the smallest parser and repository that satisfy these tests. Use `currentServerUTCTime` to establish the Baghdad service date and the timezone database for conversion.
- [x] Run the target test and confirm it passes; run it again after any refactor.

### Task 2: Wire default provider, cache, and location source

**Files:**
- Modify: `apps/mobile/lib/core/providers.dart`
- Modify: `apps/mobile/lib/core/data_maintenance.dart`
- Modify: `apps/mobile/lib/core/governorate_repository.dart` only if location metadata needs reuse.
- Create/update targeted provider tests under `apps/mobile/test/core/`.

**Interfaces:**
- `prayerTimesProvider` becomes the shared “today” source: API first, then same-location cached API result, then local calculation with explicit source/stale metadata.
- `prayerTimesForDateProvider` remains the existing local calculation for dates outside today.

- [x] Write tests for chosen city/governorate center, consented GPS coordinates, location identity without coordinates in cache keys, API failure fallback, and cache invalidation when the selected location/date changes.
- [x] Run provider tests and confirm they fail for missing behavior.
- [x] Implement provider routing without sending `X-Device-Key`; preserve current GPS permission/reverse-geocoding checks.
- [x] Run provider tests and confirm they pass.

### Task 3: Keep screen, home, and notifications consistent

**Files:**
- Modify: `apps/mobile/lib/core/prayer_notification_sync.dart`
- Modify: `apps/mobile/lib/core/prayer_notification_schedule.dart` if a minimal source/date field is needed.
- Modify: `apps/mobile/lib/features/prayer/prayer_screen.dart`
- Modify: `apps/mobile/lib/features/home/home_screen.dart`
- Modify: `apps/mobile/lib/features/settings/settings_subpages.dart`
- Add/update: targeted widget/pure-function tests under `apps/mobile/test/`.

**Interfaces:**
- Screen and home use `prayerTimesProvider`; notification sync uses it for the current Baghdad day and `prayerTimesForDateProvider` for the following 11 days.

- [x] Write tests that assert the screen/home show API time and Hijri label, expose its source/caution, omit missing Islamic-midnight rows, and schedule today’s alert at the same adjusted local time shown on screen.
- [x] Write tests that future alerts still use the existing local method, user minute offsets still apply once, and all dates remain in Asia/Baghdad.
- [x] Run the notification-sync target and confirm its expected failure; add screen/home tests before their UI changes and verify them after implementation.
- [x] Implement the UI/source labels, current-day alert path, future-day path, and failure/stale states; rename the calculation-method setting label so it clearly applies to the remaining locally calculated days.
- [x] Run all targeted tests and confirm they pass.

### Task 4: Audit, document, and package

**Files:**
- Update: `docs/PRAYER_TIMES.md`
- Update: `docs/API_CATALOG.md`
- Update: `docs/reports/ROUND20_AUDIT_AND_REPAIRS_AR.md` or create a Round 21 implementation/validation report.
- Update: `docs/superpowers/specs/2026-11-16-prayer-times-api-design.md` only if verification changes the approved behavior.

- [x] Run `python3 scripts/audit_project.py`, `python3 -m compileall -q services/api content/resources/quran`, and `node --check apps/admin/admin.js`.
- [x] Install reviewed backend requirements into an isolated temporary venv, then run `pytest -q` and record baseline plus final results.
- [x] Run `flutter pub get`, `flutter analyze`, and `flutter test` with an official stable Flutter SDK; document the analyzer warnings/info findings and lack of native platform folders.
- [x] Inspect final diff, Quran SHA-256/bundle invariants, API source labels, cache contents, and outbound request/logging for coordinates.
- [x] Produce the updated full project ZIP and run `unzip -t` on that exact final archive.

## Self-review checklist

- API parser, provider wiring, UI, notifications, and docs each have test coverage appropriate to the project’s available tools.
- The same local-date and source semantics are used by home, prayer details, and today’s alert.
- An absent/invalid API response cannot silently masquerade as an API result.
- Future dates never use the `GetTodaysTimes` endpoint, which was observed to ignore a `date` query.
- No change is presented as Android/iOS-tested unless those platforms actually build and run.
