# Release readiness checklist

## Android

- [ ] Generate platform project using Flutter SDK (`flutter create --platforms=android,ios ...`).
- [ ] Confirm `applicationId` / namespace and register Play Console ownership.
- [ ] Set Android `android:label` (or `@string/app_name`) exactly to `خَزَائِنُ الهُدَى`; create platform-sized launcher icons from `apps/mobile/assets/branding/khazain_logo.jpg`. Do not add the logo back to the home-screen content.
- [ ] Set a production HTTPS `API_BASE_URL`; prohibit cleartext traffic in release.
- [ ] Configure signing key in CI secret storage, never in Git.
- [ ] Test Android 10–16, different aspect ratios, tablets, font scaling and RTL.
- [ ] Add and verify `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `POST_NOTIFICATIONS`, notification boot receiver and exact-alarm declarations only for released features; do not add background-location permission.
- [ ] Test offline start, expired cache, process death, low storage and upload interruption.
- [ ] Produce signed APK/AAB and verify SHA-256.

## iOS

- [ ] Build on macOS with a maintained Xcode version.
- [ ] Confirm bundle ID, team, provisioning profile and signing.
- [ ] Add accurate privacy usage descriptions only for requested permissions.
- [ ] Verify ATS/HTTPS and supported audio session behavior.
- [ ] Test on physical iPhone and iPad, background/foreground transitions and force-quit behavior.
- [ ] Archive, validate and export IPA using Xcode; upload through App Store Connect.

## Shared

- [ ] Test the bundled Noto Naskh Arabic UI interface font and separate Amiri Quran font, including Quranic marks and shaping, on physical Android/iOS devices.
- [ ] On-device, verify that the bundled Quran JSON has SHA-256 `d3865331602f30d919a872c7766c6043de7dc5101276da871c3b9dd422f798e0`, contains 114 surahs and 6,236 ayahs, works offline, and ships with `apps/mobile/assets/data/QURAN_SOURCE_NOTICE_AR.md`.
- [ ] Confirm the exact Tanzil file snapshot/hash, reviewer approval and visual Quran text review.
- [ ] Compare 30+ days of prayer calculations across Iraq's 19 governorates against an approved Ja'fari timetable.
- [ ] Verify source and rights for every Quran recitation, adhan, majlis and radio stream.
- [ ] Integrate and physically test the Round 11 local prayer-notification scheduler; decide separately whether remote push through FCM/APNs is needed.
- [ ] Do not claim full-length adhan background playback: the Round 11 build removes the adhan MP3/player and currently uses the OS notification sound only. Add full background audio only as a separately licensed and implemented feature.
- [ ] Complete account registration, secure recovery, account deletion and sync before advertising accounts.
- [ ] Add privacy policy, terms, data retention and deletion flows.
- [ ] Complete accessibility, screen reader, dynamic type, contrast and RTL QA.
- [ ] Run unit, integration, API security, UI, physical-device and penetration tests.
- [ ] Execute production backup restore drill and monitoring alert tests.
- [ ] Obtain scholarly/editorial sign-off for all religious texts and date claims.
- [ ] Store listing review, screenshots, content rating and privacy disclosures.

**A build that opens is not a release approval.**
