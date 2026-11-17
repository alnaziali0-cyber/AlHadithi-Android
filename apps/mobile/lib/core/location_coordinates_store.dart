import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class StoredLocationCoordinates {
  const StoredLocationCoordinates({
    required this.latitude,
    required this.longitude,
    required this.source,
    required this.cacheToken,
  });

  final double latitude;
  final double longitude;
  final String source;
  final String cacheToken;
}

abstract interface class LocationCoordinatesStore {
  Future<StoredLocationCoordinates?> read();
  Future<void> write(StoredLocationCoordinates coordinates);
  Future<void> clear();
}

class SecureLocationCoordinatesStore implements LocationCoordinatesStore {
  SecureLocationCoordinatesStore(this._storage);

  final FlutterSecureStorage _storage;
  static const _latitudeKey = 'khazain_location_latitude_v1';
  static const _longitudeKey = 'khazain_location_longitude_v1';
  static const _sourceKey = 'khazain_location_source_v1';
  static const _cacheTokenKey = 'khazain_location_cache_token_v1';

  @override
  Future<StoredLocationCoordinates?> read() async {
    final values = await Future.wait<String?>([
      _storage.read(key: _latitudeKey),
      _storage.read(key: _longitudeKey),
      _storage.read(key: _sourceKey),
      _storage.read(key: _cacheTokenKey),
    ]);
    final latitude = double.tryParse(values[0] ?? '');
    final longitude = double.tryParse(values[1] ?? '');
    final source = values[2];
    final cacheToken = values[3];
    if (values.every((value) => value == null)) return null;
    if (latitude == null ||
        longitude == null ||
        source == null ||
        (source != 'gps' && source != 'manual') ||
        cacheToken == null ||
        !RegExp(r'^[A-Za-z0-9_-]{20,128}$').hasMatch(cacheToken)) {
      await clear();
      return null;
    }
    if (!latitude.isFinite ||
        !longitude.isFinite ||
        latitude < -90 ||
        latitude > 90 ||
        longitude < -180 ||
        longitude > 180) {
      await clear();
      return null;
    }
    return StoredLocationCoordinates(
      latitude: latitude,
      longitude: longitude,
      source: source,
      cacheToken: cacheToken,
    );
  }

  @override
  Future<void> write(StoredLocationCoordinates coordinates) async {
    if (!coordinates.latitude.isFinite ||
        !coordinates.longitude.isFinite ||
        coordinates.latitude < -90 ||
        coordinates.latitude > 90 ||
        coordinates.longitude < -180 ||
        coordinates.longitude > 180 ||
        (coordinates.source != 'gps' && coordinates.source != 'manual') ||
        !RegExp(r'^[A-Za-z0-9_-]{20,128}$').hasMatch(coordinates.cacheToken)) {
      throw ArgumentError('Invalid location coordinates or source.');
    }
    await _storage.write(
        key: _latitudeKey, value: coordinates.latitude.toString());
    await _storage.write(
        key: _longitudeKey, value: coordinates.longitude.toString());
    await _storage.write(key: _sourceKey, value: coordinates.source);
    await _storage.write(key: _cacheTokenKey, value: coordinates.cacheToken);
  }

  @override
  Future<void> clear() async {
    await _storage.delete(key: _latitudeKey);
    await _storage.delete(key: _longitudeKey);
    await _storage.delete(key: _sourceKey);
    await _storage.delete(key: _cacheTokenKey);
  }
}
