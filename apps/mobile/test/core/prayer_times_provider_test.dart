import 'dart:convert';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:khazain_al_huda/core/data_maintenance.dart';
import 'package:khazain_al_huda/core/location_coordinates_store.dart';
import 'package:khazain_al_huda/core/models.dart';
import 'package:khazain_al_huda/core/misr_quran_prayer_times.dart';
import 'package:khazain_al_huda/core/providers.dart';
import 'package:timezone/data/latest_all.dart' as tz_data;
import 'package:timezone/timezone.dart' as tz;

final _baghdad = Governorate(
  id: 'baghdad',
  nameAr: 'بغداد',
  nameEn: 'Baghdad',
  latitude: 33.3152,
  longitude: 44.3661,
  timezone: 'Asia/Baghdad',
  coordinatesQuality: 'approximate_governorate_center',
);

final _arbil = PrayerCityEntry(
  cityId: 'IQ-ER-ARB',
  governorateId: 'erbil',
  governorateNameAr: 'أربيل',
  cityNameAr: 'أربيل',
  cityNameEn: 'Erbil',
  latitude: 36.1901,
  longitude: 44.0091,
  timezone: 'Asia/Baghdad',
  coordinateSource: 'supplied_city_center',
  coordinateAccuracy: 'approximate_city_center',
  verified: false,
);

DateTime _baghdadToday() {
  final zone = tz.getLocation('Asia/Baghdad');
  final now = tz.TZDateTime.from(DateTime.now().toUtc(), zone);
  return DateTime(now.year, now.month, now.day);
}

String _dateKey(DateTime date) =>
    '${date.year.toString().padLeft(4, '0')}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';

PrayerTimesResult _result(
  String date, {
  PrayerTimesSource source = PrayerTimesSource.localCalculation,
  bool stale = false,
  String? hijriDate,
}) =>
    PrayerTimesResult(
      date: date,
      displayTimes: const {
        'fajr': '05:01',
        'sunrise': '06:34',
        'dhuhr': '11:47',
        'asr': '14:40',
        'sunset': '17:00',
        'maghrib': '17:00',
        'isha': '18:23'
      },
      methodLabel: 'اختبار',
      definitions: const {},
      isStale: stale,
      source: source,
      hijriDate: hijriDate,
    );

class _FakePrayerApi implements PrayerTimesApiSource {
  _FakePrayerApi({required this.result, this.failure});

  final PrayerTimesResult result;
  final Object? failure;
  int calls = 0;
  double? lastLatitude;
  double? lastLongitude;

  @override
  Future<PrayerTimesResult> fetch(
      {required double latitude, required double longitude}) async {
    calls++;
    lastLatitude = latitude;
    lastLongitude = longitude;
    if (failure != null) throw failure!;
    return result;
  }
}

class _FakeCoordinatesStore implements LocationCoordinatesStore {
  _FakeCoordinatesStore(this.value);

  final StoredLocationCoordinates? value;

  @override
  Future<StoredLocationCoordinates?> read() async => value;

  @override
  Future<void> clear() async {}

  @override
  Future<void> write(StoredLocationCoordinates coordinates) async {}
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  tz_data.initializeTimeZones();

  test('uses the selected city center instead of the governorate center', () {
    final location = resolvePrayerApiLocation(
      selectedPrayerCityId: _arbil.cityId,
      selectedGovernorateId: 'baghdad',
      selectedLocationSource: 'manual',
      storedCoordinates: null,
      cities: [_arbil],
      governorates: [_baghdad],
    );

    expect(location!.latitude, 36.1901);
    expect(location.longitude, 44.0091);
    expect(location.identity, 'city_IQ-ER-ARB');
    expect(location.locationName, 'أربيل');
  });

  test(
      'sends explicitly selected GPS coordinates and keys cache only by its random token',
      () {
    const coordinates = StoredLocationCoordinates(
      latitude: 33.3128,
      longitude: 44.3615,
      source: 'gps',
      cacheToken: 'a-random-cache-token-that-is-long-enough',
    );
    final location = resolvePrayerApiLocation(
      selectedPrayerCityId: null,
      selectedGovernorateId: 'baghdad',
      selectedLocationSource: 'gps',
      storedCoordinates: coordinates,
      cities: const [],
      governorates: [_baghdad],
    );

    expect(location!.latitude, 33.3128);
    expect(location.longitude, 44.3615);
    expect(location.identity, 'gps_a-random-cache-token-that-is-long-enough');
    final key = misrPrayerTimesCacheKey(
      locationIdentity: location.identity,
      date: '2026-11-16',
    );
    expect(key,
        'prayer_misr_gps_a-random-cache-token-that-is-long-enough_2026-11-16');
    expect(key, isNot(contains('33.3128')));
    expect(key, isNot(contains('44.3615')));
  });

  test(
      'manual governorate selection uses the bundled center and cache keys vary by date',
      () {
    final location = resolvePrayerApiLocation(
      selectedPrayerCityId: null,
      selectedGovernorateId: 'baghdad',
      selectedLocationSource: 'manual',
      storedCoordinates: null,
      cities: const [],
      governorates: [_baghdad],
    );

    expect(location!.latitude, 33.3152);
    expect(location.longitude, 44.3661);
    expect(
      misrPrayerTimesCacheKey(
          locationIdentity: location.identity, date: '2026-11-16'),
      isNot(misrPrayerTimesCacheKey(
          locationIdentity: location.identity, date: '2026-11-17')),
    );
  });

  test(
      'prayer API cache keys are removed by the existing clear-prayer-cache action',
      () {
    final keys = DataMaintenance.prayerCacheKeys([
      'prayer_misr_governorate_baghdad_2026-11-16',
      'catalog_cache_test',
    ]);

    expect(keys, ['prayer_misr_governorate_baghdad_2026-11-16']);
  });

  test(
      'today provider returns API times and location metadata before local fallback',
      () async {
    SharedPreferences.setMockInitialValues({
      'selected_governorate': 'baghdad',
      'selected_location_source': 'manual',
    });
    final prefs = await SharedPreferences.getInstance();
    final day = _dateKey(_baghdadToday());
    final api = _FakePrayerApi(
      result: _result(day,
          source: PrayerTimesSource.misrQuranApi, hijriDate: '١٥ جمادى الآخرة'),
    );
    var localCalls = 0;
    final container = ProviderContainer(overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      governoratesProvider.overrideWith((ref) async => [_baghdad]),
      prayerCitiesProvider.overrideWith((ref) async => const []),
      locationCoordinatesStoreProvider
          .overrideWithValue(_FakeCoordinatesStore(null)),
      misrQuranPrayerTimesApiSourceProvider.overrideWithValue(api),
      prayerTimesForDateProvider.overrideWith((ref, requestedDay) async {
        localCalls++;
        return _result(_dateKey(requestedDay));
      }),
    ]);
    addTearDown(container.dispose);

    final result = await container.read(prayerTimesProvider.future);

    expect(api.calls, 1);
    expect(api.lastLatitude, 33.3152);
    expect(api.lastLongitude, 44.3661);
    expect(localCalls, 0);
    expect(result!.displayTimes['fajr'], '05:01');
    expect(result.source, PrayerTimesSource.misrQuranApi);
    expect(result.hijriDate, '١٥ جمادى الآخرة');
    expect(result.locationName, 'بغداد');
  });

  test(
      'failed API uses same-day cached API data as stale without overwriting its service date',
      () async {
    SharedPreferences.setMockInitialValues({
      'selected_governorate': 'baghdad',
      'selected_location_source': 'manual',
    });
    final prefs = await SharedPreferences.getInstance();
    final today = _dateKey(_baghdadToday());
    await prefs.setString(
      'prayer_misr_governorate_baghdad_$today',
      jsonEncode(_result('2026-11-15',
              source: PrayerTimesSource.misrQuranApi,
              hijriDate: '١٤ جمادى الآخرة')
          .toJson()),
    );
    final api =
        _FakePrayerApi(result: _result(today), failure: StateError('offline'));
    var localCalls = 0;
    final container = ProviderContainer(overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      governoratesProvider.overrideWith((ref) async => [_baghdad]),
      prayerCitiesProvider.overrideWith((ref) async => const []),
      locationCoordinatesStoreProvider
          .overrideWithValue(_FakeCoordinatesStore(null)),
      misrQuranPrayerTimesApiSourceProvider.overrideWithValue(api),
      prayerTimesForDateProvider.overrideWith((ref, requestedDay) async {
        localCalls++;
        return _result(_dateKey(requestedDay));
      }),
    ]);
    addTearDown(container.dispose);

    final result = await container.read(prayerTimesProvider.future);

    expect(result!.date, '2026-11-15');
    expect(result.isStale, isTrue);
    expect(result.source, PrayerTimesSource.misrQuranApi);
    expect(localCalls, 0);
  });

  test(
      'failed API with no API cache falls back to the existing local calculation',
      () async {
    SharedPreferences.setMockInitialValues({
      'selected_governorate': 'baghdad',
      'selected_location_source': 'manual',
    });
    final prefs = await SharedPreferences.getInstance();
    final today = _dateKey(_baghdadToday());
    final api =
        _FakePrayerApi(result: _result(today), failure: StateError('offline'));
    var localCalls = 0;
    final container = ProviderContainer(overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      governoratesProvider.overrideWith((ref) async => [_baghdad]),
      prayerCitiesProvider.overrideWith((ref) async => const []),
      locationCoordinatesStoreProvider
          .overrideWithValue(_FakeCoordinatesStore(null)),
      misrQuranPrayerTimesApiSourceProvider.overrideWithValue(api),
      prayerTimesForDateProvider.overrideWith((ref, requestedDay) async {
        localCalls++;
        return _result(_dateKey(requestedDay));
      }),
    ]);
    addTearDown(container.dispose);

    final result = await container.read(prayerTimesProvider.future);

    expect(api.calls, 1);
    expect(localCalls, 1);
    expect(result!.source, PrayerTimesSource.localCalculation);
    expect(result.isStale, isFalse);
  });
}
