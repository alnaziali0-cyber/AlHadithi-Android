import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:khazain_al_huda/core/models.dart';
import 'package:khazain_al_huda/core/providers.dart';
import 'package:khazain_al_huda/core/location_coordinates_store.dart';

class _MemoryLocationCoordinatesStore implements LocationCoordinatesStore {
  StoredLocationCoordinates? value;
  bool failNextWrite = false;
  bool failRead = false;

  @override
  Future<StoredLocationCoordinates?> read() async {
    if (failRead) throw StateError('simulated secure storage unavailable');
    return value;
  }

  @override
  Future<void> write(StoredLocationCoordinates coordinates) async {
    if (failNextWrite) {
      failNextWrite = false;
      throw StateError('simulated secure storage failure');
    }
    value = coordinates;
  }

  @override
  Future<void> clear() async {
    value = null;
  }
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const governorate = Governorate(
    id: 'babil',
    nameAr: 'بابل',
    nameEn: 'Babil',
    latitude: 32.48,
    longitude: 44.43,
    timezone: 'Asia/Baghdad',
    coordinatesQuality: 'approximate_governorate_center',
  );

  setUp(() {
    SharedPreferences.setMockInitialValues(<String, Object>{});
  });

  test('manual selection persists stable id and governorate center', () async {
    final preferences = await SharedPreferences.getInstance();
    final coordinatesStore = _MemoryLocationCoordinatesStore()..failRead = true;
    final controller = InitialSetupController(preferences, coordinatesStore);

    await controller.complete(
      governorate: governorate,
      latitude: governorate.latitude,
      longitude: governorate.longitude,
      source: 'manual',
    );

    expect(controller.state, isTrue);
    expect(preferences.getBool('initial_setup_complete'), isTrue);
    expect(preferences.getString('selected_governorate'), 'babil');
    expect(preferences.getString('selected_governorate_name'), 'بابل');
    expect(preferences.getString('selected_location_source'), 'manual');
    expect(coordinatesStore.value, isNull);
    expect(preferences.getDouble('selected_location_latitude'), isNull);
    expect(preferences.getDouble('selected_location_longitude'), isNull);
  });

  test('GPS selection persists coordinates locally and marks source', () async {
    final preferences = await SharedPreferences.getInstance();
    final coordinatesStore = _MemoryLocationCoordinatesStore();
    final controller = InitialSetupController(preferences, coordinatesStore);

    await controller.complete(
      governorate: governorate,
      latitude: 32.46367,
      longitude: 44.41963,
      source: 'gps',
    );

    expect(preferences.getString('selected_governorate'), 'babil');
    expect(preferences.getString('selected_location_source'), 'gps');
    expect(coordinatesStore.value?.latitude, 32.46367);
    expect(coordinatesStore.value?.longitude, 44.41963);
    expect(coordinatesStore.value?.source, 'gps');
    expect(coordinatesStore.value?.cacheToken, matches(RegExp(r'^[A-Za-z0-9_-]{20,128}$')));
  });

  test('manual replacement clears previous GPS coordinates and cache', () async {
    SharedPreferences.setMockInitialValues(<String, Object>{
      'selected_governorate': 'baghdad',
      'selected_governorate_name': 'بغداد',
      'selected_location_source': 'gps',
      'initial_setup_complete': true,
      'prayer_gps_oldtokenabcdefghijklmnopqr_2026-11-14_jafari_qom': '{"cache":true}',
    });
    final preferences = await SharedPreferences.getInstance();
    final coordinatesStore = _MemoryLocationCoordinatesStore()
      ..value = const StoredLocationCoordinates(
        latitude: 33.3152,
        longitude: 44.3661,
        source: 'gps',
        cacheToken: 'oldtokenabcdefghijklmnopqr',
      );
    final controller = InitialSetupController(preferences, coordinatesStore);

    await controller.complete(
      governorate: governorate,
      latitude: governorate.latitude,
      longitude: governorate.longitude,
      source: 'manual',
    );

    expect(preferences.getString('selected_governorate'), 'babil');
    expect(preferences.getString('selected_location_source'), 'manual');
    expect(coordinatesStore.value, isNull);
    expect(preferences.getKeys().any((key) => key.startsWith('prayer_gps_oldtoken')), isFalse);
  });

  test('failed secure storage write keeps the previous selection intact', () async {
    SharedPreferences.setMockInitialValues(<String, Object>{
      'selected_governorate': 'baghdad',
      'selected_governorate_name': 'بغداد',
      'selected_location_source': 'gps',
      'initial_setup_complete': true,
    });
    final preferences = await SharedPreferences.getInstance();
    final coordinatesStore = _MemoryLocationCoordinatesStore()
      ..value = const StoredLocationCoordinates(
        latitude: 33.3152,
        longitude: 44.3661,
        source: 'gps',
        cacheToken: 'oldtokenabcdefghijklmnopqr',
      )
      ..failNextWrite = true;
    final controller = InitialSetupController(preferences, coordinatesStore);

    await expectLater(
      controller.complete(
        governorate: governorate,
        latitude: governorate.latitude,
        longitude: governorate.longitude,
        source: 'gps',
      ),
      throwsStateError,
    );

    expect(preferences.getString('selected_governorate'), 'baghdad');
    expect(preferences.getString('selected_governorate_name'), 'بغداد');
    expect(coordinatesStore.value?.latitude, 33.3152);
    expect(coordinatesStore.value?.longitude, 44.3661);
  });

  test('unknown selection source is rejected before persistence', () async {
    final preferences = await SharedPreferences.getInstance();
    final coordinatesStore = _MemoryLocationCoordinatesStore();
    final controller = InitialSetupController(preferences, coordinatesStore);

    await expectLater(
      controller.complete(
        governorate: governorate,
        latitude: governorate.latitude,
        longitude: governorate.longitude,
        source: 'unknown',
      ),
      throwsArgumentError,
    );
    expect(preferences.getString('selected_governorate'), isNull);
  });

  test('invalid coordinates are rejected before persistence', () async {
    final preferences = await SharedPreferences.getInstance();
    final coordinatesStore = _MemoryLocationCoordinatesStore();
    final controller = InitialSetupController(preferences, coordinatesStore);

    await expectLater(
      controller.complete(
        governorate: governorate,
        latitude: 95,
        longitude: 44,
        source: 'gps',
      ),
      throwsArgumentError,
    );
    expect(preferences.getString('selected_governorate'), isNull);
    expect(preferences.getBool('initial_setup_complete'), isNull);
  });
}
