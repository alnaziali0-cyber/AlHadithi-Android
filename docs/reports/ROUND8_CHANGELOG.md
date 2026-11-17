# ROUND 8 — Settings expansion and display preferences

## Implemented

- Settings hub rebuilt as grouped, tappable button rows: icon badge, title, value pill or toggle, chevron. Read-only status rows are styled as status and never look tappable.
- In-app bottom sheets with selected-state marks for theme, text size and calculation file. The previous segmented control and dropdown keep the same providers and side effects.
- App-wide text size (Small, Default, Large, Extra large = 0.9x, 1.0x, 1.15x, 1.3x). It multiplies the operating-system text scale and is clamped to 0.8x–2.4x so OS accessibility settings still apply.
- Persisted haptic-feedback preference. The tasbih screen and the settings hub read the same value.
- Prayer display sub-page (`/settings/prayer`):
  - Show or hide sunrise and sunset as reference times (default: shown, unchanged behavior).
  - Manual display offsets for Fajr, Dhuhr, Maghrib and Isha, limited to ±30 minutes in 1-minute steps. Offsets are applied identically on the prayer tab and in the home prayer card, including the next-prayer countdown. Shifted rows show a "معدّل يدوياً" note. Offsets never cross midnight; values are clamped within the same day.
  - Offsets are display-only. The calculation, the backend response and the cached source values are not modified.
- Privacy and data sub-page (`/settings/privacy`):
  - Describes what is stored on this device and on the server, row by row. Each row matches the code that writes the item: favourites are stored on the server under an anonymous device key; reading positions are saved on the server and kept as a local copy; tasbih state stays on the device.
  - Shows whether precise GPS coordinates are stored, or "تعذر الفحص" when secure storage cannot be read.
  - Delete precise location: removes secure-storage coordinates and GPS-derived prayer cache entries. If the location source was GPS, it changes to manual (approximate governorate centre).
  - Clear cached prayer times; clear offline content (catalog, book page and favourites copies, plus the governorate catalogue cache).
  - Reset display and prayer settings. Theme, governorate, calculation file, favourites, tasbih counters and book reading positions are kept.
  - Every data-removing action asks for confirmation that states exactly what is removed and what is kept.
- Licences screen through Flutter's `showLicensePage`.
- Notifications, adhan and background audio are shown as honest "not enabled" status. No fake toggles were added.
- The background-location disclosure from the earlier settings screen is restored, with its original wording, as the footer of the location group.

## Review fixes

A compile-level review found no blocking errors. It did find the runtime and copy problems below. They are fixed in this round.

- Actions that use `ref` after an `await` check `context.mounted` first: theme, text size, calculation file, precise-location deletion and prayer-cache clearing. Before this fix, a screen that closed during the await could throw when `ref` was used.
- The governorate subtitle is reactive. `selectedLocationSourceProvider` is invalidated when the setup flow returns, after a city is chosen on the prayer tab, and after precise-location deletion. Before this fix, the subtitle could stay stale until the screen was rebuilt.
- Opening an external link is wrapped in try/catch. A failure shows the same message as a `false` result.
- Privacy page: the reading-position row said "on this device". It now says the data is saved on the server with a local copy. Tasbih has its own row.
- Settings hub: the favourites subtitle said "saved on this device". Favourites are stored on the server under an anonymous device key. The original text had the same error, and it is corrected here.
- Secure-storage read failures show "تعذر الفحص" instead of "غير محفوظ".

## Files added

- `apps/mobile/lib/core/app_preferences.dart` — preference model, controller, provider, text-size enum, pure time and offset helpers.
- `apps/mobile/lib/core/data_maintenance.dart` — data-removal operations, pure key-selection helpers, and `selectedLocationSourceProvider`, which the settings hub reads.
- `apps/mobile/lib/features/settings/settings_widgets.dart` — shared settings UI kit (groups, tiles, switches, status rows, option sheets, confirmation dialog).
- `apps/mobile/lib/features/settings/settings_subpages.dart` — prayer display and privacy/data sub-pages.
- `apps/mobile/test/core/app_preferences_test.dart` — unit tests (authored, not executed).
- `docs/reports/ROUND8_CHANGELOG.md` — this file.

## Files modified (with impact)

- `apps/mobile/lib/features/settings/settings_screen.dart` — rewritten layout. Retained: theme mode selection, governorate change (`/setup?mode=change`), calculation-file selection, favourites, sources, TikTok/Instagram/WhatsApp links, version. Replaced: the segmented theme control and the method dropdown (now bottom sheets, same providers and invalidation); the placeholder text rows for notifications and audio (now explicit status rows). The governorate change now refreshes the location subtitle when the setup flow returns. Copy corrected for favourites. No behaviour was removed.
- `apps/mobile/lib/app/khazain_app.dart` — `builder` added to apply the text scale. Theme, router and locale are unchanged.
- `apps/mobile/lib/app/router.dart` — two top-level routes added outside the shell (no bottom bar, back navigation). No existing route changed.
- `apps/mobile/lib/features/prayer/prayer_screen.dart` — displayed times pass through the offset helper (identical output when all offsets are zero); reference rows are filtered only when the user hides them. One line added: `ref.invalidate(selectedLocationSourceProvider)` after a city is chosen. The city choice, the stored value and the times are unchanged.
- `apps/mobile/lib/features/home/home_screen.dart` — the prayer summary card and next-prayer countdown use the same offset helper.
- `apps/mobile/lib/features/tasbih/tasbih_screen.dart` — vibration state moved from widget-local state (reset on every launch, default on) to the persisted preference (default on). Behaviour change: the choice now persists across launches and matches the settings hub.

## Not implemented (and why)

- Local notifications, adhan scheduling, per-prayer alerts: no notification plugin in `pubspec.yaml`, and delivery cannot be verified without native platform setup. Shown as status only.
- Background audio controls: no audio service implementation exists yet.
- Language switch: only Arabic strings exist. English and Persian options would not change anything, so they were not added.
- Hijri date correction: the app has no Hijri calendar implementation yet.
- Downloads and storage usage: no download manager exists yet.
- Asr (العصر): **not calculated by the backend and not displayed anywhere.** This is a significant gap for a prayer application. Adding it requires an authority-approved calculation rule and must not be guessed.
- Full privacy policy and terms: not drafted. The privacy page describes what the app actually stores, on the device and on the server, and states that the official policy has not been published.
- Governorate and city selection as an embedded bottom sheet (section 7 of the engineering brief): not changed in this round. The existing full-page setup flow is preserved.

## Verification status

- Flutter and Dart SDKs were not available in the execution environment. pub.dev and storage.googleapis.com were unreachable from the sandbox (HTTP 403 through the proxy).
- The code was **not compiled, analysed, formatted or tested.** Review was performed by reading the code and cross-checking every new symbol against its definition and import. The review fixes were checked the same way.
- A lexical check (bracket balance and string literals, comments skipped) passes on the 11 changed Dart files. The same check passes on the 29 pristine round-7 Dart files, which confirms it does not raise false alarms. It is not a parser.
- The test import uses the package name `khazain_al_huda`, confirmed in `pubspec.yaml`. The logo asset is declared and present.
- Icon identifiers, `TextScaler` and `MediaQuery.copyWith(textScaler:)` usage, `showDragHandle`, `IconButton.filledTonal`, `AsyncValue.hasError`, `Provider.autoDispose` and `showLicensePage` were chosen against Flutter 3.22+ and Riverpod 2.x APIs and must be confirmed by the analyser.
- Unit tests in `test/core/app_preferences_test.dart` were authored but not executed. Expected values were traced by hand against the implementation.
- Backend: unchanged in this round.

## Required before release

1. `flutter pub get`, `dart format --set-exit-if-changed lib test`, `flutter analyze`, `flutter test`.
2. Manual checks on Android and iOS: text scale at Large and Extra large (overflow in the hub rows, the tasbih counter and the prayer rows), dark mode for the option sheets and the privacy page, RTL alignment of chevrons and switches.
3. Confirm the Asr decision with an approved reference before the next prayer-time change.
4. Check the privacy page rows against the server's retention rules and the published privacy policy once it exists.
