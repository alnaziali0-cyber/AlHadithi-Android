import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter/services.dart';
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';

const wallpaperCatalogAsset = 'assets/data/wallpapers.json';
const wallpaperPublishedCacheKey = 'wallpaper_published_ids_v1';
const _wallpaperAssetPrefix = 'assets/images/wallpapers/';

class WallpaperEntry {
  const WallpaperEntry({
    required this.id,
    required this.title,
    required this.assetPath,
    required this.creator,
    required this.license,
    required this.sha256,
    required this.defaultPublished,
    required this.published,
  });

  final String id;
  final String title;
  final String assetPath;
  final String creator;
  final String license;
  final String sha256;
  final bool defaultPublished;
  final bool published;

  WallpaperEntry withPublished(bool value) => WallpaperEntry(
        id: id,
        title: title,
        assetPath: assetPath,
        creator: creator,
        license: license,
        sha256: sha256,
        defaultPublished: defaultPublished,
        published: value,
      );

  static List<WallpaperEntry> parseCatalog(String source) {
    final decoded = jsonDecode(source);
    if (decoded is! Map || decoded['schema_version'] != 1 || decoded['items'] is! List) {
      throw const FormatException('Invalid bundled wallpaper catalogue.');
    }
    final ids = <String>{};
    return (decoded['items'] as List).map((raw) {
      if (raw is! Map) throw const FormatException('Invalid wallpaper record.');
      final item = Map<String, dynamic>.from(raw);
      final id = item['id'];
      final assetPath = item['asset_path'];
      final sha256 = item['sha256'];
      if (id is! String || !RegExp(r'^[a-z0-9-]{1,80}$').hasMatch(id) ||
          !ids.add(id) || assetPath is! String ||
          !assetPath.startsWith(_wallpaperAssetPrefix) ||
          !assetPath.endsWith('.jpg') || sha256 is! String ||
          !RegExp(r'^[a-f0-9]{64}$').hasMatch(sha256) ||
          item['title'] is! String || item['creator'] is! String ||
          item['license'] is! String || item['default_published'] is! bool) {
        throw const FormatException('Invalid wallpaper metadata.');
      }
      return WallpaperEntry(
        id: id,
        title: item['title'] as String,
        assetPath: assetPath,
        creator: item['creator'] as String,
        license: item['license'] as String,
        sha256: sha256,
        defaultPublished: item['default_published'] as bool,
        published: item['default_published'] as bool,
      );
    }).toList(growable: false);
  }
}

class WallpaperRepository {
  WallpaperRepository(this._dio, this._preferences, {AssetBundle? bundle})
      : _bundle = bundle ?? rootBundle;

  final Dio _dio;
  final SharedPreferences _preferences;
  final AssetBundle _bundle;

  Future<List<WallpaperEntry>> listPublished() async {
    final catalog = WallpaperEntry.parseCatalog(
      await _bundle.loadString(wallpaperCatalogAsset),
    );
    final knownIds = catalog.map((entry) => entry.id).toSet();
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/api/v1/public/wallpapers',
        options: Options(
          connectTimeout: const Duration(seconds: 3),
          receiveTimeout: const Duration(seconds: 4),
        ),
      );
      final rawItems = response.data?['items'];
      if (rawItems is! List) {
        throw const FormatException('Invalid published wallpaper response.');
      }
      final ids = rawItems
          .whereType<Map>()
          .map((item) => item['id'])
          .whereType<String>()
          .where(knownIds.contains)
          .toSet();
      await _preferences.setString(wallpaperPublishedCacheKey, jsonEncode(ids.toList()));
      return catalog
          .where((entry) => ids.contains(entry.id))
          .map((entry) => entry.withPublished(true))
          .toList(growable: false);
    } on DioException {
      return _offlineCatalog(catalog, knownIds);
    } on FormatException {
      return _offlineCatalog(catalog, knownIds);
    }
  }

  List<WallpaperEntry> _offlineCatalog(
    List<WallpaperEntry> catalog,
    Set<String> knownIds,
  ) {
    final cached = _preferences.getString(wallpaperPublishedCacheKey);
    if (cached == null) return catalog.where((entry) => entry.defaultPublished).toList(growable: false);
    try {
      final decoded = jsonDecode(cached);
      if (decoded is! List) throw const FormatException('Invalid wallpaper cache.');
      final ids = decoded.whereType<String>().where(knownIds.contains).toSet();
      return catalog
          .where((entry) => ids.contains(entry.id))
          .map((entry) => entry.withPublished(true))
          .toList(growable: false);
    } on FormatException {
      return catalog.where((entry) => entry.defaultPublished).toList(growable: false);
    }
  }
}

class WallpaperStorageService {
  WallpaperStorageService({
    Future<Directory> Function()? documentsDirectory,
    Future<Uint8List> Function(String assetPath)? readAssetBytes,
  })  : _documentsDirectory = documentsDirectory ?? getApplicationDocumentsDirectory,
        _readAssetBytes = readAssetBytes ?? _loadAssetBytes;

  final Future<Directory> Function() _documentsDirectory;
  final Future<Uint8List> Function(String assetPath) _readAssetBytes;

  static Future<Uint8List> _loadAssetBytes(String assetPath) async {
    final data = await rootBundle.load(assetPath);
    return data.buffer.asUint8List(data.offsetInBytes, data.lengthInBytes);
  }

  Future<File> save(WallpaperEntry wallpaper) async {
    if (!RegExp(r'^[a-z0-9-]{1,80}$').hasMatch(wallpaper.id) ||
        !wallpaper.assetPath.startsWith(_wallpaperAssetPrefix) ||
        !wallpaper.assetPath.endsWith('.jpg')) {
      throw ArgumentError('Invalid local wallpaper asset.');
    }
    final directory = await _documentsDirectory();
    final targetDirectory = Directory('${directory.path}/wallpapers');
    await targetDirectory.create(recursive: true);
    final target = File('${targetDirectory.path}/${wallpaper.id}.jpg');
    final bytes = await _readAssetBytes(wallpaper.assetPath);
    await target.writeAsBytes(bytes, flush: true);
    return target;
  }

  Future<bool> isSaved(WallpaperEntry wallpaper) async {
    if (!RegExp(r'^[a-z0-9-]{1,80}$').hasMatch(wallpaper.id)) return false;
    final directory = await _documentsDirectory();
    return File('${directory.path}/wallpapers/${wallpaper.id}.jpg').exists();
  }
}
