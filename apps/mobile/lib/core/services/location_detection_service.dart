import 'dart:async';

import 'package:geocoding/geocoding.dart';
import 'package:geolocator/geolocator.dart';

import '../models.dart';

enum LocationDetectionStatus {
  success,
  serviceDisabled,
  permissionDenied,
  permissionDeniedForever,
  outsideIraq,
  lowAccuracy,
  governorateNotFound,
  timedOut,
  unavailable,
}

class LocationDetectionResult {
  const LocationDetectionResult({
    required this.status,
    this.governorate,
    this.latitude,
    this.longitude,
  });

  final LocationDetectionStatus status;
  final Governorate? governorate;
  final double? latitude;
  final double? longitude;

  bool get isSuccess => status == LocationDetectionStatus.success &&
      governorate != null && latitude != null && longitude != null;
}

class LocationDetectionService {
  const LocationDetectionService();

  static const double _minLatitude = 29.0;
  static const double _maxLatitude = 37.5;
  static const double _minLongitude = 38.7;
  static const double _maxLongitude = 48.8;

  Future<LocationDetectionResult> detect(List<Governorate> governorates) async {
    try {
      final serviceEnabled = await Geolocator.isLocationServiceEnabled();
      if (!serviceEnabled) {
        return const LocationDetectionResult(status: LocationDetectionStatus.serviceDisabled);
      }

      var permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
      }
      if (permission == LocationPermission.denied) {
        return const LocationDetectionResult(status: LocationDetectionStatus.permissionDenied);
      }
      if (permission == LocationPermission.deniedForever) {
        return const LocationDetectionResult(status: LocationDetectionStatus.permissionDeniedForever);
      }
      if (permission != LocationPermission.whileInUse && permission != LocationPermission.always) {
        return const LocationDetectionResult(status: LocationDetectionStatus.permissionDenied);
      }

      final position = await Geolocator.getCurrentPosition(
        locationSettings: const LocationSettings(
          accuracy: LocationAccuracy.bestForNavigation,
          timeLimit: Duration(seconds: 30),
          distanceFilter: 0,
        ),
      );
      final latitude = position.latitude;
      final longitude = position.longitude;
      if (!position.accuracy.isFinite || position.accuracy > 500) {
        return LocationDetectionResult(
          status: LocationDetectionStatus.lowAccuracy,
          latitude: latitude,
          longitude: longitude,
        );
      }
      if (!_isPlausibleIraqCoordinate(latitude, longitude)) {
        return LocationDetectionResult(
          status: LocationDetectionStatus.outsideIraq,
          latitude: latitude,
          longitude: longitude,
        );
      }

      final placemarks = await placemarkFromCoordinates(latitude, longitude);
      if (placemarks.isEmpty) {
        return LocationDetectionResult(
          status: LocationDetectionStatus.governorateNotFound,
          latitude: latitude,
          longitude: longitude,
        );
      }

      final iraqPlacemark = placemarks.where(_isIraqPlacemark).firstOrNull;
      if (iraqPlacemark == null) {
        return LocationDetectionResult(
          status: LocationDetectionStatus.outsideIraq,
          latitude: latitude,
          longitude: longitude,
        );
      }

      final governorate = _matchGovernorate(iraqPlacemark, governorates);
      if (governorate == null) {
        return LocationDetectionResult(
          status: LocationDetectionStatus.governorateNotFound,
          latitude: latitude,
          longitude: longitude,
        );
      }

      return LocationDetectionResult(
        status: LocationDetectionStatus.success,
        governorate: governorate,
        latitude: latitude,
        longitude: longitude,
      );
    } on LocationServiceDisabledException {
      return const LocationDetectionResult(status: LocationDetectionStatus.serviceDisabled);
    } on PermissionDeniedException {
      return const LocationDetectionResult(status: LocationDetectionStatus.permissionDenied);
    } on TimeoutException {
      return const LocationDetectionResult(status: LocationDetectionStatus.timedOut);
    } on Exception {
      return const LocationDetectionResult(status: LocationDetectionStatus.unavailable);
    }
  }

  bool isPlausibleIraqCoordinate(double latitude, double longitude) =>
      latitude.isFinite &&
      longitude.isFinite &&
      latitude >= _minLatitude &&
      latitude <= _maxLatitude &&
      longitude >= _minLongitude &&
      longitude <= _maxLongitude;

  Governorate? resolveIraqPlacemark(Placemark placemark, List<Governorate> governorates) {
    if (!_isIraqPlacemark(placemark)) return null;
    return _matchGovernorate(placemark, governorates);
  }

  bool _isPlausibleIraqCoordinate(double latitude, double longitude) =>
      isPlausibleIraqCoordinate(latitude, longitude);

  bool _isIraqPlacemark(Placemark placemark) {
    final iso = (placemark.isoCountryCode ?? '').trim().toUpperCase();
    if (iso == 'IQ') return true;
    final country = _normalise(placemark.country ?? '');
    return country == 'iraq' || country == 'republic of iraq' || country == 'العراق';
  }

  Governorate? _matchGovernorate(Placemark placemark, List<Governorate> governorates) {
    final candidates = <String?>[
      placemark.administrativeArea,
      placemark.subAdministrativeArea,
      placemark.locality,
    ];
    for (final candidate in candidates) {
      if (candidate == null || candidate.trim().isEmpty) continue;
      final normalized = _normalise(candidate);
      for (final governorate in governorates) {
        final names = <String>[
          governorate.nameAr,
          governorate.nameEn,
          ...governorate.aliases,
        ];
        if (names.any((name) => _normalise(name) == normalized)) return governorate;
      }
    }
    return null;
  }

  String _normalise(String value) {
    var result = value.trim().toLowerCase();
    result = result.replaceAll(RegExp(r'[\u064B-\u065F\u0670\u0640]'), '');
    result = result.replaceAll(RegExp(r'^(محافظة\s+|province\s+|governorate\s+)'), '');
    result = result.replaceAll(RegExp(r'\s+(province|governorate)$'), '');
    result = result.replaceAll('أ', 'ا').replaceAll('إ', 'ا').replaceAll('آ', 'ا');
    result = result.replaceAll('ى', 'ي');
    result = result.replaceAll(RegExp(r'[._-]+'), ' ');
    result = result.replaceAll(RegExp(r'\s+'), ' ');
    return result.trim();
  }
}

extension _FirstOrNull<T> on Iterable<T> {
  T? get firstOrNull {
    final iterator = this.iterator;
    return iterator.moveNext() ? iterator.current : null;
  }
}
