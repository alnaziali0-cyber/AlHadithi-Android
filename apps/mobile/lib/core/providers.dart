import 'dart:convert';
import 'dart:math';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import 'models.dart';
import 'governorate_repository.dart';
import 'location_coordinates_store.dart';
import 'misr_quran_prayer_times.dart';
import 'wallpapers.dart';

final sharedPreferencesProvider = Provider<SharedPreferences>((ref) =>
    throw UnimplementedError(
        'SharedPreferences must be injected in main.dart'));
String? _ephemeralDeviceKey;

Future<String> _deviceKey(SharedPreferences prefs) async {
  final stored = prefs.getString('anonymous_device_key');
  if (stored != null && RegExp(r'^[A-Za-z0-9_-]{32,128}$').hasMatch(stored)) {
    return stored;
  }
  if (_ephemeralDeviceKey != null) return _ephemeralDeviceKey!;
  final random = Random.secure();
  final bytes =
      List<int>.generate(32, (_) => random.nextInt(256), growable: false);
  final generated = base64UrlEncode(bytes).replaceAll('=', '');
  _ephemeralDeviceKey = generated;
  try {
    await prefs.setString('anonymous_device_key', generated);
  } catch (_) {
    // Keep the identity for this process lifetime if local preference storage fails.
  }
  return generated;
}

final apiProvider = Provider<Dio>((ref) {
  const baseUrl = String.fromEnvironment('API_BASE_URL',
      defaultValue: 'http://10.0.2.2:8000');
  final prefs = ref.watch(sharedPreferencesProvider);
  final dio = Dio(BaseOptions(
    baseUrl: baseUrl,
    connectTimeout: const Duration(seconds: 10),
    receiveTimeout: const Duration(seconds: 15),
    sendTimeout: const Duration(seconds: 15),
    headers: const {'Accept': 'application/json'},
  ));
  dio.interceptors.add(InterceptorsWrapper(
    onRequest: (options, handler) async {
      try {
        options.headers['X-Device-Key'] = await _deviceKey(prefs);
        handler.next(options);
      } catch (error) {
        handler.reject(DioException(requestOptions: options, error: error));
      }
    },
  ));
  return dio;
});

final wallpaperRepositoryProvider = Provider<WallpaperRepository>(
  (ref) => WallpaperRepository(
    ref.watch(apiProvider),
    ref.watch(sharedPreferencesProvider),
  ),
);

final wallpaperStorageServiceProvider = Provider<WallpaperStorageService>(
  (ref) => WallpaperStorageService(),
);

class DeviceLibraryApi {
  const DeviceLibraryApi(this._dio, this._prefs);
  final Dio _dio;
  final SharedPreferences _prefs;

  Future<List<FavoriteEntry>> listFavorites() async {
    try {
      final response =
          await _dio.get<Map<String, dynamic>>('/api/v1/user/favorites');
      final rawItems = response.data?['items'];
      if (rawItems is! List) return const <FavoriteEntry>[];
      final entries = rawItems
          .whereType<Map>()
          .map(
              (item) => FavoriteEntry.fromJson(Map<String, dynamic>.from(item)))
          .toList(growable: false);
      await _prefs.setString('favorite_entries_cache',
          jsonEncode(entries.map((entry) => entry.toJson()).toList()));
      return entries;
    } on DioException {
      final cached = _prefs.getString('favorite_entries_cache');
      if (cached == null) rethrow;
      try {
        final rawItems = jsonDecode(cached);
        if (rawItems is! List) return const <FavoriteEntry>[];
        return rawItems
            .whereType<Map>()
            .map((item) => FavoriteEntry.fromJson(
                Map<String, dynamic>.from(item),
                isStale: true))
            .toList(growable: false);
      } catch (_) {
        await _prefs.remove('favorite_entries_cache');
        rethrow;
      }
    }
  }

  Future<void> addFavorite(String entityType, String entityId) async {
    await _dio.post<void>('/api/v1/user/favorites', data: {
      'entity_type': entityType,
      'entity_id': entityId,
    });
  }

  Future<void> removeFavorite(String entityType, String entityId) async {
    await _dio.delete<void>(
      '/api/v1/user/favorites/${Uri.encodeComponent(entityType)}/${Uri.encodeComponent(entityId)}',
    );
  }

  Future<Map<String, dynamic>?> getBookProgress(int bookId) async {
    final response = await _dio.get<Map<String, dynamic>>(
        '/api/v1/user/books/$bookId/reading-progress');
    final progress = response.data?['progress'];
    return progress is Map ? Map<String, dynamic>.from(progress) : null;
  }

  Future<Map<String, dynamic>> saveBookProgress({
    required int bookId,
    required int volumeNumber,
    required int pageNumber,
  }) async {
    final response = await _dio.put<Map<String, dynamic>>(
      '/api/v1/user/books/$bookId/reading-progress',
      data: {'volume_number': volumeNumber, 'page_number': pageNumber},
    );
    return Map<String, dynamic>.from(response.data ?? const {});
  }
}

final deviceLibraryApiProvider = Provider<DeviceLibraryApi>(
  (ref) => DeviceLibraryApi(
      ref.watch(apiProvider), ref.watch(sharedPreferencesProvider)),
);

final favoriteEntriesProvider = FutureProvider<List<FavoriteEntry>>(
  (ref) => ref.watch(deviceLibraryApiProvider).listFavorites(),
);

class ThemeModeController extends StateNotifier<ThemeMode> {
  ThemeModeController(this._prefs) : super(_read(_prefs));
  final SharedPreferences _prefs;
  static ThemeMode _read(SharedPreferences prefs) {
    switch (prefs.getString('theme_mode')) {
      case 'light':
        return ThemeMode.light;
      case 'dark':
        return ThemeMode.dark;
      default:
        return ThemeMode.system;
    }
  }

  Future<void> setMode(ThemeMode mode) async {
    state = mode;
    await _prefs.setString(
        'theme_mode',
        switch (mode) {
          ThemeMode.light => 'light',
          ThemeMode.dark => 'dark',
          _ => 'system'
        });
  }
}

final themeModeProvider = StateNotifierProvider<ThemeModeController, ThemeMode>(
    (ref) => ThemeModeController(ref.watch(sharedPreferencesProvider)));

class SelectionController extends StateNotifier<String?> {
  SelectionController(this._prefs, String key)
      : _key = key,
        super(_prefs.getString(key));
  final SharedPreferences _prefs;
  final String _key;
  Future<void> select(String? value) async {
    state = value;
    if (value == null) {
      await _prefs.remove(_key);
    } else {
      await _prefs.setString(_key, value);
    }
  }
}

final selectedGovernorateProvider =
    StateNotifierProvider<SelectionController, String?>((ref) =>
        SelectionController(
            ref.watch(sharedPreferencesProvider), 'selected_governorate'));
final selectedPrayerMethodProvider =
    StateNotifierProvider<SelectionController, String?>((ref) =>
        SelectionController(
            ref.watch(sharedPreferencesProvider), 'prayer_method'));
final selectedPrayerCityProvider =
    StateNotifierProvider<SelectionController, String?>((ref) =>
        SelectionController(
            ref.watch(sharedPreferencesProvider), 'selected_prayer_city'));

final misrQuranPrayerTimesApiSourceProvider = Provider<PrayerTimesApiSource>(
  (ref) => MisrQuranPrayerTimesRepository(),
);

Future<void> clearGpsPrayerCache(
    SharedPreferences preferences, String cacheToken) async {
  final prefixes = [
    'prayer_gps_${cacheToken}_',
    'prayer_misr_gps_${cacheToken}_'
  ];
  final keys = preferences
      .getKeys()
      .where((key) => prefixes.any(key.startsWith))
      .toList(growable: false);
  for (final key in keys) {
    await preferences.remove(key);
  }
}

class GovernorateStorage {
  GovernorateStorage(this._preferences, this._coordinatesStore);
  final SharedPreferences _preferences;
  final LocationCoordinatesStore _coordinatesStore;

  Future<void> save({
    required Governorate governorate,
    required double latitude,
    required double longitude,
    required String source,
  }) async {
    if (governorate.id.trim().isEmpty) {
      throw ArgumentError('Governorate identifier is required.');
    }
    if (source != 'gps' && source != 'manual') {
      throw ArgumentError(
          'Governorate selection source must be gps or manual.');
    }
    if (!latitude.isFinite ||
        !longitude.isFinite ||
        latitude < -90 ||
        latitude > 90 ||
        longitude < -180 ||
        longitude > 180) {
      throw ArgumentError('Invalid governorate coordinates.');
    }
    StoredLocationCoordinates? previousCoordinates;
    try {
      previousCoordinates = await _coordinatesStore.read();
    } catch (_) {
      if (source == 'gps') rethrow;
    }
    final previous = <String, Object?>{
      'selected_governorate': _preferences.getString('selected_governorate'),
      'selected_governorate_name':
          _preferences.getString('selected_governorate_name'),
      'selected_location_source':
          _preferences.getString('selected_location_source'),
      'initial_setup_complete': _preferences.getBool('initial_setup_complete'),
    };
    var coordinatesStoreChanged = false;
    String? newCacheToken;
    try {
      if (source == 'gps') {
        final tokenBytes = List<int>.generate(
            24, (_) => Random.secure().nextInt(256),
            growable: false);
        final cacheToken = base64UrlEncode(tokenBytes).replaceAll('=', '');
        newCacheToken = cacheToken;
        coordinatesStoreChanged = true;
        await _coordinatesStore.write(StoredLocationCoordinates(
          latitude: latitude,
          longitude: longitude,
          source: source,
          cacheToken: cacheToken,
        ));
      } else if (previousCoordinates != null) {
        try {
          await _coordinatesStore.clear();
          coordinatesStoreChanged = true;
        } catch (_) {
          // The manual governorate id takes precedence; an inaccessible secure value is never used.
        }
      }

      final writes = <Future<bool>>[
        _preferences.setString('selected_governorate_name', governorate.nameAr),
        _preferences.setString('selected_location_source', source),
      ];
      final results = await Future.wait(writes);
      if (results.any((saved) => !saved)) {
        throw StateError('Unable to persist governorate location metadata.');
      }
      if (!await _preferences.setString(
          'selected_governorate', governorate.id)) {
        throw StateError('Unable to persist governorate identifier.');
      }
      if (!await _preferences.setBool('initial_setup_complete', true)) {
        throw StateError('Unable to persist initial setup completion.');
      }
      if (previousCoordinates?.source == 'gps' &&
          (source != 'gps' ||
              previousCoordinates!.cacheToken != newCacheToken)) {
        await clearGpsPrayerCache(
            _preferences, previousCoordinates!.cacheToken);
      }
    } catch (_) {
      await _restoreValue(
          'selected_governorate', previous['selected_governorate']);
      await _restoreValue(
          'selected_governorate_name', previous['selected_governorate_name']);
      if (coordinatesStoreChanged) {
        if (previousCoordinates == null) {
          await _coordinatesStore.clear();
        } else {
          await _coordinatesStore.write(previousCoordinates);
        }
      }
      await _restoreValue(
          'selected_location_source', previous['selected_location_source']);
      await _restoreValue(
          'initial_setup_complete', previous['initial_setup_complete']);
      rethrow;
    }
  }

  Future<void> _restoreValue(String key, Object? value) async {
    if (value == null) {
      await _preferences.remove(key);
    } else if (value is String) {
      await _preferences.setString(key, value);
    } else if (value is bool) {
      await _preferences.setBool(key, value);
    }
  }
}

final locationCoordinatesStoreProvider = Provider<LocationCoordinatesStore>(
  (ref) => SecureLocationCoordinatesStore(const FlutterSecureStorage()),
);

final governorateStorageProvider = Provider<GovernorateStorage>(
  (ref) => GovernorateStorage(
    ref.watch(sharedPreferencesProvider),
    ref.watch(locationCoordinatesStoreProvider),
  ),
);

class InitialSetupController extends StateNotifier<bool> {
  InitialSetupController(this._preferences, this._coordinatesStore)
      : super(
          _preferences.getBool('initial_setup_complete') == true ||
              (_preferences.getString('selected_governorate')?.isNotEmpty ??
                  false),
        );

  final SharedPreferences _preferences;
  final LocationCoordinatesStore _coordinatesStore;

  Future<void> complete({
    required Governorate governorate,
    required double latitude,
    required double longitude,
    required String source,
  }) async {
    await GovernorateStorage(_preferences, _coordinatesStore).save(
      governorate: governorate,
      latitude: latitude,
      longitude: longitude,
      source: source,
    );
    state = true;
  }
}

final initialSetupProvider =
    StateNotifierProvider<InitialSetupController, bool>(
  (ref) => InitialSetupController(
    ref.watch(sharedPreferencesProvider),
    ref.watch(locationCoordinatesStoreProvider),
  ),
);

final governoratesProvider = FutureProvider<List<Governorate>>((ref) async {
  return ref.watch(governorateRepositoryProvider).load();
});

final governorateRepositoryProvider = Provider<GovernorateRepository>(
  (ref) => GovernorateRepository(
      ref.watch(apiProvider), ref.watch(sharedPreferencesProvider)),
);

final prayerCitiesProvider = FutureProvider<List<PrayerCityEntry>>((ref) async {
  final response = await ref
      .watch(apiProvider)
      .get<List<dynamic>>('/api/v1/public/prayer/cities');
  return (response.data ?? const [])
      .whereType<Map>()
      .map((item) => PrayerCityEntry.fromJson(Map<String, dynamic>.from(item)))
      .toList(growable: false);
});

final selectedPrayerTimezoneProvider = FutureProvider<String>((ref) async {
  final cityId = ref.watch(selectedPrayerCityProvider);
  if (cityId != null) {
    final cities = await ref.watch(prayerCitiesProvider.future);
    for (final city in cities) {
      if (city.cityId == cityId && city.timezone.isNotEmpty)
        return city.timezone;
    }
  }
  final governorateId = ref.watch(selectedGovernorateProvider);
  if (governorateId != null) {
    final governorates = await ref.watch(governoratesProvider.future);
    for (final governorate in governorates) {
      if (governorate.id == governorateId && governorate.timezone.isNotEmpty) {
        return governorate.timezone;
      }
    }
  }
  return 'Asia/Baghdad';
});

/// Requests prayer times for an arbitrary local calendar date. This family is
/// also used by the notification scheduler to prepare a rolling fourteen-day plan.
final prayerTimesForDateProvider = FutureProvider.autoDispose
    .family<PrayerTimesResult?, DateTime>((ref, requestedDay) async {
  final governorateId = ref.watch(selectedGovernorateProvider);
  final cityId = ref.watch(selectedPrayerCityProvider);
  final method = ref.watch(selectedPrayerMethodProvider) ?? 'jafari_qom';
  if (cityId == null && governorateId == null) return null;
  final targetDay =
      DateTime(requestedDay.year, requestedDay.month, requestedDay.day);
  final day =
      '${targetDay.year.toString().padLeft(4, '0')}-${targetDay.month.toString().padLeft(2, '0')}-${targetDay.day.toString().padLeft(2, '0')}';
  final prefs = ref.watch(sharedPreferencesProvider);
  final dio = ref.watch(apiProvider);
  StoredLocationCoordinates? storedLocation;
  if (prefs.getString('selected_location_source') == 'gps') {
    try {
      storedLocation = await ref.watch(locationCoordinatesStoreProvider).read();
    } catch (_) {
      storedLocation = null;
    }
  }
  final gpsLocation = storedLocation?.source == 'gps' ? storedLocation : null;
  final useCoordinates = cityId == null && gpsLocation != null;
  final locationKey = cityId != null
      ? 'city_$cityId'
      : gpsLocation != null
          ? '${gpsLocation.source}_${gpsLocation.cacheToken}'
          : 'governorate_$governorateId';
  final key = 'prayer_${locationKey}_${day}_$method';
  Map<String, dynamic>? cached;
  final cachedString = prefs.getString(key);
  if (cachedString != null) {
    try {
      cached = Map<String, dynamic>.from(jsonDecode(cachedString) as Map);
    } catch (_) {
      await prefs.remove(key);
    }
  }
  final query = <String, dynamic>{'day': day, 'method': method};
  if (cityId != null) {
    query['city_id'] = cityId;
  } else if (!useCoordinates) {
    query['governorate_id'] = governorateId;
  }
  try {
    final response = cityId == null && gpsLocation != null
        ? await dio.post<Map<String, dynamic>>(
            '/api/v1/public/prayer/times/coordinates',
            data: {
              'latitude': gpsLocation.latitude,
              'longitude': gpsLocation.longitude,
              'day': day,
              'method': method,
            },
          )
        : await dio.get<Map<String, dynamic>>(
            '/api/v1/public/prayer/times',
            queryParameters: query,
          );
    final data =
        Map<String, dynamic>.from(response.data ?? const <String, dynamic>{});
    if (useCoordinates && data['location'] is Map) {
      final location = Map<String, dynamic>.from(data['location'] as Map);
      location['name_ar'] ??= prefs.getString('selected_governorate_name');
      location['coordinates_quality'] = 'gps_device_estimate';
      location['coordinate_source'] = 'device_location_permission';
      data['location'] = location;
    }
    final cacheData = Map<String, dynamic>.from(data);
    if (useCoordinates && cacheData['location'] is Map) {
      final cachedLocation =
          Map<String, dynamic>.from(cacheData['location'] as Map);
      cachedLocation.remove('latitude');
      cachedLocation.remove('longitude');
      cacheData['location'] = cachedLocation;
    }
    await prefs.setString(key, jsonEncode(cacheData));
    return PrayerTimesResult.fromJson(data);
  } on DioException {
    if (cached != null) {
      if (useCoordinates &&
          storedLocation != null &&
          cached['location'] is Map) {
        final cachedLocation =
            Map<String, dynamic>.from(cached['location'] as Map);
        cachedLocation['latitude'] = storedLocation.latitude;
        cachedLocation['longitude'] = storedLocation.longitude;
        cachedLocation['name_ar'] ??=
            prefs.getString('selected_governorate_name');
        cachedLocation['coordinates_quality'] = 'gps_device_estimate';
        cachedLocation['coordinate_source'] = 'device_location_permission';
        cached['location'] = cachedLocation;
      }
      return PrayerTimesResult.fromJson(cached, isStale: true);
    }
    rethrow;
  }
});

final prayerTimesProvider = FutureProvider<PrayerTimesResult?>((ref) async {
  final today = prayerBaghdadCalendarDate();
  final day =
      '${today.year.toString().padLeft(4, '0')}-${today.month.toString().padLeft(2, '0')}-${today.day.toString().padLeft(2, '0')}';
  final cityId = ref.watch(selectedPrayerCityProvider);
  final governorateId = ref.watch(selectedGovernorateProvider);
  if (cityId == null && governorateId == null) return null;

  final prefs = ref.watch(sharedPreferencesProvider);
  final selectedLocationSource = prefs.getString('selected_location_source');
  StoredLocationCoordinates? storedCoordinates;
  if (selectedLocationSource == 'gps') {
    try {
      storedCoordinates =
          await ref.watch(locationCoordinatesStoreProvider).read();
    } catch (_) {
      storedCoordinates = null;
    }
  }

  final locationIdentity = cityId != null
      ? 'city_$cityId'
      : selectedLocationSource == 'gps' && storedCoordinates?.source == 'gps'
          ? 'gps_${storedCoordinates!.cacheToken}'
          : 'governorate_$governorateId';
  final cacheKey =
      misrPrayerTimesCacheKey(locationIdentity: locationIdentity, date: day);
  PrayerTimesResult? cachedApiResult;
  final cachedString = prefs.getString(cacheKey);
  if (cachedString != null) {
    try {
      final decoded = jsonDecode(cachedString);
      if (decoded is! Map)
        throw const FormatException('Invalid cached prayer-time object.');
      final candidate =
          PrayerTimesResult.fromJson(Map<String, dynamic>.from(decoded));
      if (candidate.source != PrayerTimesSource.misrQuranApi) {
        throw const FormatException('Cached result is not a MisrQuran result.');
      }
      cachedApiResult = candidate;
    } catch (_) {
      await prefs.remove(cacheKey);
    }
  }

  List<PrayerCityEntry> cities = const [];
  if (cityId != null) {
    try {
      cities = await ref.watch(prayerCitiesProvider.future);
    } catch (_) {
      cities = const [];
    }
  }
  List<Governorate> governorates = const [];
  if (governorateId != null) {
    try {
      governorates = await ref.watch(governoratesProvider.future);
    } catch (_) {
      governorates = const [];
    }
  }
  final location = resolvePrayerApiLocation(
    selectedPrayerCityId: cityId,
    selectedGovernorateId: governorateId,
    selectedLocationSource: selectedLocationSource,
    storedCoordinates: storedCoordinates,
    cities: cities,
    governorates: governorates,
  );

  if (location != null) {
    try {
      final fetched =
          await ref.watch(misrQuranPrayerTimesApiSourceProvider).fetch(
                latitude: location.latitude,
                longitude: location.longitude,
              );
      if (fetched.date != day) {
        throw FormatException(
            'MisrQuran service date did not match Baghdad date $day.');
      }
      final result = fetched.copyWith(
        source: PrayerTimesSource.misrQuranApi,
        locationName: location.locationName,
        coordinateAccuracy: location.coordinateAccuracy,
        coordinateSource: location.coordinateSource,
        locationVerified: location.verified,
      );
      await prefs.setString(cacheKey, jsonEncode(result.toJson()));
      return result;
    } catch (_) {
      // Keep the provider response, exact coordinates, and request errors out of logs.
    }
  }

  if (cachedApiResult != null) return cachedApiResult.copyWith(isStale: true);
  return ref.watch(prayerTimesForDateProvider(today).future);
});

final quranStatusProvider = FutureProvider<Map<String, dynamic>>((ref) async {
  final response = await ref
      .watch(apiProvider)
      .get<Map<String, dynamic>>('/api/v1/public/quran/status');
  return response.data ?? {};
});

final quranSurahsProvider = FutureProvider<List<QuranSurahEntry>>((ref) async {
  final response = await ref
      .watch(apiProvider)
      .get<List<dynamic>>('/api/v1/public/quran/surahs');
  return (response.data ?? const [])
      .whereType<Map>()
      .map((item) => QuranSurahEntry.fromJson(Map<String, dynamic>.from(item)))
      .toList(growable: false);
});

final quranSurahAyahsProvider =
    FutureProvider.family<QuranAyahPage, int>((ref, surahNumber) async {
  final response = await ref.watch(apiProvider).get<Map<String, dynamic>>(
    '/api/v1/public/quran/surahs/$surahNumber/ayahs',
    queryParameters: const {'limit': 300, 'offset': 0},
  );
  return QuranAyahPage.fromJson(
      Map<String, dynamic>.from(response.data ?? const {}));
});
final radioStatusProvider = FutureProvider<Map<String, dynamic>>((ref) async {
  final response = await ref
      .watch(apiProvider)
      .get<Map<String, dynamic>>('/api/v1/public/radio');
  return response.data ?? {};
});

final contentItemsProvider =
    FutureProvider.family<CatalogResult, (String, String)>((ref, query) async {
  final contentType = query.$1;
  final search = query.$2.trim();
  final dio = ref.watch(apiProvider);
  final prefs = ref.watch(sharedPreferencesProvider);
  final cacheKey = search.isEmpty
      ? 'catalog_cache_$contentType'
      : 'catalog_cache_${contentType}_${Uri.encodeComponent(search)}';
  try {
    final response = await dio.get<Map<String, dynamic>>(
      '/api/v1/public/catalog/$contentType',
      queryParameters: {
        'limit': 50,
        'offset': 0,
        if (search.isNotEmpty) 'search': search
      },
    );
    final payload = Map<String, dynamic>.from(response.data ?? const {});
    final result = CatalogResult.fromJson(payload);
    await prefs.setString(cacheKey, jsonEncode(result.toJson()));
    return result;
  } on DioException {
    final cached = prefs.getString(cacheKey);
    if (cached != null) {
      try {
        return CatalogResult.fromJson(
          Map<String, dynamic>.from(jsonDecode(cached) as Map),
          isStale: true,
        );
      } catch (_) {
        await prefs.remove(cacheKey);
      }
    }
    rethrow;
  }
});

final sourceCatalogProvider =
    FutureProvider<List<SourceCatalogEntry>>((ref) async {
  final response = await ref
      .watch(apiProvider)
      .get<List<dynamic>>('/api/v1/public/reference/sources');
  return (response.data ?? const [])
      .whereType<Map>()
      .map((item) =>
          SourceCatalogEntry.fromJson(Map<String, dynamic>.from(item)))
      .toList(growable: false);
});
