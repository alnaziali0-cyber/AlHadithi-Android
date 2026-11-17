import 'package:dio/dio.dart';
import 'package:timezone/data/latest_all.dart' as tz_data;
import 'package:timezone/timezone.dart' as tz;

import 'location_coordinates_store.dart';
import 'models.dart';

abstract interface class PrayerTimesApiSource {
  Future<PrayerTimesResult> fetch({
    required double latitude,
    required double longitude,
  });
}

class PrayerApiLocation {
  const PrayerApiLocation({
    required this.identity,
    required this.latitude,
    required this.longitude,
    required this.coordinateAccuracy,
    required this.coordinateSource,
    required this.verified,
    this.locationName,
  });

  final String identity;
  final double latitude;
  final double longitude;
  final String? locationName;
  final String coordinateAccuracy;
  final String coordinateSource;
  final bool verified;
}

PrayerApiLocation? resolvePrayerApiLocation({
  required String? selectedPrayerCityId,
  required String? selectedGovernorateId,
  required String? selectedLocationSource,
  required StoredLocationCoordinates? storedCoordinates,
  required Iterable<PrayerCityEntry> cities,
  required Iterable<Governorate> governorates,
}) {
  bool valid(double latitude, double longitude) =>
      latitude.isFinite &&
      longitude.isFinite &&
      latitude >= -90 &&
      latitude <= 90 &&
      longitude >= -180 &&
      longitude <= 180;

  if (selectedPrayerCityId != null) {
    for (final city in cities) {
      if (city.cityId != selectedPrayerCityId) continue;
      if (!valid(city.latitude, city.longitude)) return null;
      return PrayerApiLocation(
        identity: 'city_${city.cityId}',
        latitude: city.latitude,
        longitude: city.longitude,
        locationName: city.cityNameAr,
        coordinateAccuracy: city.coordinateAccuracy,
        coordinateSource: city.coordinateSource,
        verified: city.verified,
      );
    }
    return null;
  }

  if (selectedLocationSource == 'gps' && storedCoordinates?.source == 'gps') {
    final coordinates = storedCoordinates!;
    if (!valid(coordinates.latitude, coordinates.longitude)) return null;
    final governorate = governorates
        .where((item) => item.id == selectedGovernorateId)
        .firstOrNull;
    return PrayerApiLocation(
      identity: 'gps_${coordinates.cacheToken}',
      latitude: coordinates.latitude,
      longitude: coordinates.longitude,
      locationName: governorate?.nameAr,
      coordinateAccuracy: 'gps_device_estimate',
      coordinateSource: 'device_location_permission',
      verified: true,
    );
  }

  for (final governorate in governorates) {
    if (governorate.id != selectedGovernorateId) continue;
    if (!valid(governorate.latitude, governorate.longitude)) return null;
    return PrayerApiLocation(
      identity: 'governorate_${governorate.id}',
      latitude: governorate.latitude,
      longitude: governorate.longitude,
      locationName: governorate.nameAr,
      coordinateAccuracy: governorate.coordinatesQuality,
      coordinateSource: 'governorate_center',
      verified: governorate.coordinatesQuality == 'verified',
    );
  }
  return null;
}

String misrPrayerTimesCacheKey(
    {required String locationIdentity, required String date}) {
  if (!RegExp(r'^(city|gps|governorate)_[A-Za-z0-9_-]{1,160}$')
          .hasMatch(locationIdentity) ||
      !RegExp(r'^\d{4}-\d{2}-\d{2}$').hasMatch(date)) {
    throw ArgumentError('Invalid prayer-time cache identity or date.');
  }
  return 'prayer_misr_${locationIdentity}_$date';
}

late final tz.Location _baghdadTimezone = _createBaghdadTimezone();

tz.Location _createBaghdadTimezone() {
  tz_data.initializeTimeZones();
  return tz.getLocation(MisrQuranPrayerTimesRepository.timezoneName);
}

tz.TZDateTime prayerBaghdadDateTime([DateTime? instant]) =>
    tz.TZDateTime.from((instant ?? DateTime.now()).toUtc(), _baghdadTimezone);

tz.TZDateTime prayerBaghdadWallTime(
        int year, int month, int day, int hour, int minute) =>
    tz.TZDateTime(_baghdadTimezone, year, month, day, hour, minute);

DateTime prayerBaghdadCalendarDate([DateTime? instant]) {
  final local = prayerBaghdadDateTime(instant);
  return DateTime(local.year, local.month, local.day);
}

/// Read-only client for today's Iraqi prayer times from the user-selected API.
/// This Dio instance intentionally has no app interceptors or device headers.
class MisrQuranPrayerTimesRepository implements PrayerTimesApiSource {
  MisrQuranPrayerTimesRepository({Dio? dio, tz.Location? location})
      : _dio = dio ?? createDio(),
        _location = location ?? _loadBaghdadLocation();

  static const baseUrl = 'https://api.misrquran.gov.eg';
  static const endpoint = '/api/PrayerTimes/GetTodaysTimes';
  static const timezoneName = 'Asia/Baghdad';

  final Dio _dio;
  final tz.Location _location;

  static Dio createDio() => Dio(BaseOptions(
        baseUrl: baseUrl,
        connectTimeout: const Duration(seconds: 8),
        receiveTimeout: const Duration(seconds: 12),
        sendTimeout: const Duration(seconds: 8),
        headers: const {'Accept': 'application/json'},
      ));

  static tz.Location _loadBaghdadLocation() {
    return _baghdadTimezone;
  }

  @override
  Future<PrayerTimesResult> fetch({
    required double latitude,
    required double longitude,
  }) async {
    if (!latitude.isFinite ||
        !longitude.isFinite ||
        latitude < -90 ||
        latitude > 90 ||
        longitude < -180 ||
        longitude > 180) {
      throw ArgumentError('Invalid prayer-time coordinates.');
    }

    final response = await _dio.get<dynamic>(
      endpoint,
      queryParameters: {
        'lat': latitude,
        'lng': longitude,
        'dayLightSaving': false,
      },
    );
    final payload = response.data;
    if (payload is! Map || payload.keys.any((key) => key is! String)) {
      throw const FormatException('MisrQuran returned an invalid JSON object.');
    }
    return _parse(Map<String, dynamic>.from(payload));
  }

  PrayerTimesResult _parse(Map<String, dynamic> json) {
    final serverUtc = _requiredUtcDateTime(json, 'currentServerUTCTime');
    final serviceDateTime = tz.TZDateTime.from(serverUtc, _location);
    final serviceDate = _dateKey(
        serviceDateTime.year, serviceDateTime.month, serviceDateTime.day);
    final fields = const {
      'fajr': 'fajr',
      'sunrise': 'sunrise',
      'dhuhr': 'dhuhr',
      'asr': 'asr',
      'sunset': 'sunset',
      'maghrib': 'maghrib',
      'isha': 'isha',
    };
    final displayTimes = <String, String>{};
    for (final entry in fields.entries) {
      final clock = _requiredClock(json, entry.value);
      displayTimes[entry.key] = _toBaghdadClock(clock, serviceDateTime);
    }
    final hijriDate = _requiredText(json, 'hijriDate');

    return PrayerTimesResult(
      date: serviceDate,
      displayTimes: displayTimes,
      methodLabel: 'خدمة MisrQuran — غير موثقة فقهياً',
      definitions: const {
        'provider': 'MisrQuran',
        'source_url':
            'https://api.misrquran.gov.eg/api/PrayerTimes/GetTodaysTimes',
        'timezone': timezoneName,
        'clock_basis': 'UTC fields converted to Asia/Baghdad',
        'authority_approved': false,
      },
      isStale: false,
      source: PrayerTimesSource.misrQuranApi,
      hijriDate: hijriDate,
    );
  }

  DateTime _requiredUtcDateTime(Map<String, dynamic> json, String key) {
    final raw = json[key];
    if (raw is! String || raw.trim().isEmpty) {
      throw FormatException('MisrQuran response is missing $key.');
    }
    final parsed = DateTime.tryParse(raw.trim());
    if (parsed == null || !parsed.isUtc) {
      throw FormatException(
          'MisrQuran $key must be an ISO-8601 UTC timestamp.');
    }
    return parsed.toUtc();
  }

  String _requiredText(Map<String, dynamic> json, String key) {
    final value = json[key];
    if (value is! String || value.trim().isEmpty) {
      throw FormatException('MisrQuran response is missing $key.');
    }
    return value.trim();
  }

  ({int hour, int minute, int second}) _requiredClock(
      Map<String, dynamic> json, String key) {
    final value = json[key];
    if (value is! String) {
      throw FormatException('MisrQuran response is missing the $key time.');
    }
    final match =
        RegExp(r'^([01]\d|2[0-3]):([0-5]\d)(?::([0-5]\d)(?:\.\d+)?)?$')
            .firstMatch(value.trim());
    if (match == null) {
      throw FormatException('MisrQuran returned an invalid $key time.');
    }
    return (
      hour: int.parse(match.group(1)!),
      minute: int.parse(match.group(2)!),
      second: int.parse(match.group(3) ?? '0'),
    );
  }

  String _toBaghdadClock(
      ({int hour, int minute, int second}) clock, DateTime serviceDate) {
    for (var dayOffset = -1; dayOffset <= 1; dayOffset++) {
      final utc = DateTime.utc(
        serviceDate.year,
        serviceDate.month,
        serviceDate.day + dayOffset,
        clock.hour,
        clock.minute,
        clock.second,
      );
      final local = tz.TZDateTime.from(utc, _location);
      if (local.year == serviceDate.year &&
          local.month == serviceDate.month &&
          local.day == serviceDate.day) {
        return '${local.hour.toString().padLeft(2, '0')}:${local.minute.toString().padLeft(2, '0')}';
      }
    }
    throw const FormatException(
        'MisrQuran time cannot be placed on the Baghdad service day.');
  }

  String _dateKey(int year, int month, int day) =>
      '${year.toString().padLeft(4, '0')}-${month.toString().padLeft(2, '0')}-${day.toString().padLeft(2, '0')}';
}
