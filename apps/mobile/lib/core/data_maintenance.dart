import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'location_coordinates_store.dart';
import 'providers.dart';

/// Local data controls exposed in the privacy settings.
///
/// Only keys written by this app are matched. Calculation method, governorate,
/// favorites identity, tasbih counters and book reading positions are never
/// removed here.
class DataMaintenance {
  DataMaintenance(this._prefs, this._coordinatesStore);

  final SharedPreferences _prefs;
  final LocationCoordinatesStore _coordinatesStore;

  // Both the local calculation and MisrQuran API keep device-local, location-keyed entries.
  static const _prayerCachePrefixes = [
    'prayer_city_',
    'prayer_gps_',
    'prayer_governorate_',
    'prayer_misr_',
  ];

  // Offline copies of catalogs, book pages and favorites (current and legacy formats).
  static const _offlineCachePrefixes = [
    'catalog_cache_',
    'book_pages_',
    'book_page_v2_'
  ];
  static const _offlineCacheExactKeys = [
    'governorates_catalog_cache_v1',
    'favorite_entries_cache'
  ];

  /// Pure selection helper, kept public for unit tests.
  static List<String> prayerCacheKeys(Iterable<String> keys) {
    return keys
        .where((key) =>
            _prayerCachePrefixes.any((prefix) => key.startsWith(prefix)))
        .toList(growable: false);
  }

  /// Pure selection helper, kept public for unit tests.
  static List<String> offlineCacheKeys(Iterable<String> keys) {
    return keys
        .where((key) =>
            _offlineCacheExactKeys.contains(key) ||
            _offlineCachePrefixes.any((prefix) => key.startsWith(prefix)))
        .toList(growable: false);
  }

  Future<bool> hasPreciseLocation() async {
    return (await _coordinatesStore.read()) != null;
  }

  /// Deletes the GPS coordinates held in secure storage together with any prayer
  /// cache entries derived from them. The app then falls back to the selected
  /// governorate's approximate center, and the source label is updated to match.
  Future<void> clearPreciseLocation() async {
    final previous = await _coordinatesStore.read();
    if (previous != null && previous.source == 'gps') {
      await clearGpsPrayerCache(_prefs, previous.cacheToken);
    }
    await _coordinatesStore.clear();
    if (_prefs.getString('selected_location_source') == 'gps') {
      await _prefs.setString('selected_location_source', 'manual');
    }
  }

  /// Returns the number of cached prayer-time entries removed.
  Future<int> clearPrayerTimesCache() async {
    final keys = prayerCacheKeys(_prefs.getKeys());
    for (final key in keys) {
      await _prefs.remove(key);
    }
    return keys.length;
  }

  /// Returns the number of offline content entries removed.
  Future<int> clearOfflineContent() async {
    final keys = offlineCacheKeys(_prefs.getKeys());
    for (final key in keys) {
      await _prefs.remove(key);
    }
    return keys.length;
  }
}

final dataMaintenanceProvider = Provider<DataMaintenance>(
  (ref) => DataMaintenance(
    ref.watch(sharedPreferencesProvider),
    ref.watch(locationCoordinatesStoreProvider),
  ),
);

final hasPreciseLocationProvider = FutureProvider.autoDispose<bool>(
  (ref) => ref.watch(dataMaintenanceProvider).hasPreciseLocation(),
);

/// Stored location source: 'gps', 'manual', 'city', or null when none is stored.
/// Read synchronously from SharedPreferences, so it must be invalidated after any
/// write made elsewhere (setup flow, city choice on the prayer tab, privacy deletion).
final selectedLocationSourceProvider = Provider.autoDispose<String?>(
  (ref) => ref
      .watch(sharedPreferencesProvider)
      .getString('selected_location_source'),
);
