# ROUND 7 — Iraqi governorate onboarding and location selection

## Implemented

- Added a transparent first-run governorate selection route, enforced by GoRouter before the main shell.
- Added a true cyclic 3D wheel based on `ListWheelScrollView`, with fixed-extent snapping, tap-to-select adjacent entries, perspective, magnification, opacity/scale depth treatment, RTL, and reduced-motion handling through `MediaQuery.disableAnimations`.
- Added an explicit neutral initial wheel prompt; no governorate is selected by default and confirmation stays disabled until the user moves the wheel or successfully detects a location.
- Added the 19-governorate bundled catalogue with stable IDs, Arabic/English names, approximate center coordinates, timezone, coordinate quality, and geocoder aliases.
- Added `GovernorateRepository` with API-first refresh and cached/bundled offline fallback.
- Added `LocationDetectionService` using foreground-only Geolocator permission and reverse geocoding. The service checks plausible Iraq coordinate bounds and requires an Iraq country code/name before matching a governorate.
- Added manual fallback for denied permission, permanently denied permission, disabled location services, timeout, low-accuracy fix, outside-Iraq result, and reverse-geocoding mismatch.
- Added `GovernorateStorage` and `InitialSetupController`; precise coordinates are stored through `flutter_secure_storage` (Keystore/Keychain), while stable governorate metadata remains in preferences. Persistence rolls back on failed writes. GPS coordinates are not persisted in the backend prayer cache, SharedPreferences, or the local prayer-cache payload; the local cache uses a random secure token, and previous GPS cache entries are removed on location changes.
- Added settings entry to change governorate later.
- Connected selected coordinates to the existing prayer-times API. GPS coordinates are sent only in a POST body when the user chose GPS and prayer times are requested; manual choice uses approximate governorate center coordinates.
- Added 20 Flutter tests for route guarding, wheel initial state, adjacent tap selection, circular navigation, secure-storage orchestration/rollback, invalid source/coordinates, catalogue integrity, offline fallback, and reverse-geocoding country/governorate matching. They are authored but not executed because Flutter/Dart are unavailable. Added a Backend test proving GPS coordinate requests are not persisted in `PrayerCache`.
- Added native permission setup documentation. Native Android/iOS project directories were absent and were not fabricated.

## Files added

- `apps/mobile/lib/core/governorate_repository.dart`
- `apps/mobile/lib/core/location_coordinates_store.dart`
- `apps/mobile/lib/core/initial_setup_route_policy.dart`
- `apps/mobile/lib/core/services/location_detection_service.dart`
- `apps/mobile/lib/features/governorate/governorate_selection_screen.dart`
- `apps/mobile/lib/features/governorate/governorate_wheel_picker.dart`
- `apps/mobile/assets/data/iraq_governorates.json`
- `apps/mobile/test/core/governorate_catalog_test.dart`
- `apps/mobile/test/core/governorate_repository_test.dart`
- `apps/mobile/test/core/governorate_storage_test.dart`
- `apps/mobile/test/core/location_resolution_test.dart`
- `apps/mobile/test/core/initial_setup_route_policy_test.dart`
- `apps/mobile/test/features/governorate/governorate_wheel_picker_test.dart`
- `docs/PLATFORM_LOCATION_SETUP.md`

## Files modified

- `apps/mobile/pubspec.yaml`
- `apps/mobile/lib/core/models.dart`
- `apps/mobile/lib/core/providers.dart`
- `apps/mobile/lib/app/router.dart`
- `apps/mobile/lib/features/settings/settings_screen.dart`
- `apps/mobile/lib/features/prayer/prayer_screen.dart`
- `services/api/app/main.py`
- `services/api/tests/test_api.py`
- `docs/API_CATALOG.md`
- `docs/PRAYER_TIMES.md`

## Constraints

- Flutter and Dart SDKs were not installed in the execution environment. `flutter pub get`, `dart format`, `flutter analyze`, and Flutter widget/unit tests could not be executed.
- `apps/mobile/android` and `apps/mobile/ios` did not exist in the input archive. Native permission declarations are documented but not applied to generated platform projects.
- Reverse geocoding depends on the platform geocoder and network/service availability. All failures fall back to manual selection. The mobile client uses a POST body for GPS coordinates; production request-body logging must remain disabled for this endpoint. Legacy GET coordinate parameters, if enabled for compatibility, must be redacted from access logs.
- The Iraq bounding rectangle is a sanity filter only; country identity is checked using reverse-geocoder country data. It is not a polygon boundary test.
- Governorate and city center coordinates remain approximate and not field-verified.
