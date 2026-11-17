import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:crypto/crypto.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:khazain_al_huda/core/wallpapers.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test('bundled wallpaper catalogue points only to intact local assets', () async {
    final source = await rootBundle.loadString(wallpaperCatalogAsset);
    final entries = WallpaperEntry.parseCatalog(source);
    expect(entries, hasLength(2));
    expect(entries.every((entry) => entry.defaultPublished), isTrue);
    for (final entry in entries) {
      final bytes = await rootBundle.load(entry.assetPath);
      final actualHash = sha256.convert(
        bytes.buffer.asUint8List(bytes.offsetInBytes, bytes.lengthInBytes),
      ).toString();
      expect(actualHash, entry.sha256, reason: entry.id);
      expect(entry.assetPath, startsWith('assets/images/wallpapers/'));
    }
  });

  test('catalogue rejects remote image paths and duplicate ids', () {
    final badPath = jsonEncode({
      'schema_version': 1,
      'items': [
        {
          'id': 'bad-image',
          'title': 'صورة',
          'asset_path': 'https://example.invalid/image.jpg',
          'creator': 'اختبار',
          'license': 'اختبار',
          'sha256': List.filled(64, 'a').join(),
          'default_published': true,
        }
      ],
    });
    expect(() => WallpaperEntry.parseCatalog(badPath), throwsFormatException);
  });

  test('save copies bytes to the persistent app wallpaper folder', () async {
    final directory = await Directory.systemTemp.createTemp('wallpapers-test-');
    addTearDown(() => directory.delete(recursive: true));
    final expectedBytes = Uint8List.fromList([0xff, 0xd8, 0xff, 0xd9]);
    final wallpaper = WallpaperEntry(
      id: 'local-wallpaper',
      title: 'خلفية محلية',
      assetPath: 'assets/images/wallpapers/local-wallpaper.jpg',
      creator: 'اختبار',
      license: 'CC BY 4.0',
      sha256: List.filled(64, 'a').join(),
      defaultPublished: true,
      published: true,
    );
    final storage = WallpaperStorageService(
      documentsDirectory: () async => directory,
      readAssetBytes: (_) async => expectedBytes,
    );

    final saved = await storage.save(wallpaper);

    expect(saved.path, '${directory.path}/wallpapers/local-wallpaper.jpg');
    expect(await saved.readAsBytes(), expectedBytes);
    expect(await storage.isSaved(wallpaper), isTrue);
  });

  test('save rejects a path escaping the local wallpaper asset directory', () async {
    final storage = WallpaperStorageService(
      documentsDirectory: () async => Directory.systemTemp,
      readAssetBytes: (_) async => Uint8List(0),
    );
    final invalid = WallpaperEntry(
      id: '../outside',
      title: 'غير صالح',
      assetPath: 'assets/images/wallpapers/one.jpg',
      creator: 'اختبار',
      license: 'CC BY 4.0',
      sha256: List.filled(64, 'a').join(),
      defaultPublished: true,
      published: true,
    );
    await expectLater(storage.save(invalid), throwsArgumentError);
  });
}
