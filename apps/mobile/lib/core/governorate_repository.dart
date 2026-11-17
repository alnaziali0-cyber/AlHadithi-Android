import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'models.dart';

class GovernorateRepository {
  GovernorateRepository(this._dio, this._preferences);

  final Dio _dio;
  final SharedPreferences _preferences;
  static const _cacheKey = 'governorates_catalog_cache_v1';
  static const _assetPath = 'assets/data/iraq_governorates.json';

  Future<List<Governorate>> load() async {
    final bundled = await _loadBundled();
    try {
      final response = await _dio.get<List<dynamic>>(
        '/api/v1/public/governorates',
        options: Options(
          connectTimeout: const Duration(seconds: 4),
          receiveTimeout: const Duration(seconds: 4),
        ),
      );
      final records = response.data ?? const <dynamic>[];
      final aliasesById = <String, List<String>>{
        for (final item in bundled) item.id: item.aliases,
      };
      final parsed = records
          .whereType<Map>()
          .map((item) {
            final json = Map<String, dynamic>.from(item);
            json['aliases'] ??= aliasesById[json['id']] ?? const <String>[];
            return Governorate.fromJson(json);
          })
          .toList(growable: false);
      final parsedIds = parsed.map((item) => item.id).toSet();
      if (parsed.length < bundled.length ||
          parsedIds.length != parsed.length ||
          !bundled.every((item) => parsedIds.contains(item.id))) {
        return _readCachedOrBundled(bundled);
      }
      await _preferences.setString(
        _cacheKey,
        jsonEncode(parsed.map(_toJson).toList(growable: false)),
      );
      return parsed;
    } on DioException {
      return _readCachedOrBundled(bundled);
    } on FormatException {
      return _readCachedOrBundled(bundled);
    } on TypeError {
      return _readCachedOrBundled(bundled);
    }
  }

  Future<List<Governorate>> _readCachedOrBundled(List<Governorate> bundled) async {
    final cached = _preferences.getString(_cacheKey);
    if (cached == null) return bundled;
    try {
      final decoded = jsonDecode(cached);
      if (decoded is! List) return bundled;
      final values = decoded
          .whereType<Map>()
          .map((item) => Governorate.fromJson(Map<String, dynamic>.from(item)))
          .toList(growable: false);
      final cachedIds = values.map((item) => item.id).toSet();
      if (values.length < bundled.length ||
          cachedIds.length != values.length ||
          !bundled.every((item) => cachedIds.contains(item.id))) {
        await _preferences.remove(_cacheKey);
        return bundled;
      }
      return values;
    } on FormatException {
      await _preferences.remove(_cacheKey);
      return bundled;
    } on TypeError {
      await _preferences.remove(_cacheKey);
      return bundled;
    }
  }

  Future<List<Governorate>> _loadBundled() async {
    final raw = await rootBundle.loadString(_assetPath);
    final decoded = jsonDecode(raw);
    if (decoded is! List) {
      throw const FormatException('Bundled governorates catalogue must be a JSON array.');
    }
    final values = decoded
        .whereType<Map>()
        .map((item) => Governorate.fromJson(Map<String, dynamic>.from(item)))
        .toList(growable: false);
    if (values.length != 19 || values.map((item) => item.id).toSet().length != values.length) {
      throw const FormatException('Bundled governorates catalogue is incomplete or contains duplicate identifiers.');
    }
    return values;
  }

  Map<String, dynamic> _toJson(Governorate item) => {
        'id': item.id,
        'name_ar': item.nameAr,
        'name_en': item.nameEn,
        'latitude': item.latitude,
        'longitude': item.longitude,
        'timezone': item.timezone,
        'coordinates_quality': item.coordinatesQuality,
        'aliases': item.aliases,
      };
}
