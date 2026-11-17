import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:khazain_al_huda/core/providers.dart';
import 'package:khazain_al_huda/core/wallpapers.dart';
import 'package:khazain_al_huda/features/content/wallpapers_screen.dart';

class _WallpaperRepository implements WallpaperRepository {
  _WallpaperRepository(this.items);
  final List<WallpaperEntry> items;

  @override
  Future<List<WallpaperEntry>> listPublished() async => items;
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const item = WallpaperEntry(
    id: 'karbala-shrine-night',
    title: 'مرقد الإمام الحسين عليه السلام ليلاً',
    assetPath: 'assets/images/wallpapers/karbala-shrine-night.jpg',
    creator: 'Bahr Alwatifi',
    license: 'CC BY-SA 4.0',
    sha256: '0a770423b324c859275d5f825cc14f675a4c0e77f9a7ef52560d2091f4b7d301',
    defaultPublished: true,
    published: true,
  );

  testWidgets('shows local preview and saves without a network request', (tester) async {
    final directory = await Directory.systemTemp.createTemp('wallpaper-widget-test-');
    addTearDown(() => directory.delete(recursive: true));
    final storage = WallpaperStorageService(
      documentsDirectory: () async => directory,
      readAssetBytes: (_) async => Uint8List.fromList([0xff, 0xd8, 0xff, 0xd9]),
    );

    await tester.pumpWidget(ProviderScope(
      overrides: [
        wallpaperRepositoryProvider.overrideWithValue(_WallpaperRepository([item])),
        wallpaperStorageServiceProvider.overrideWithValue(storage),
      ],
      child: const MaterialApp(home: WallpapersScreen()),
    ));
    await tester.pumpAndSettle();

    expect(find.text('الخلفيات'), findsOneWidget);
    expect(find.text(item.title), findsOneWidget);
    expect(find.text('تصوير Bahr Alwatifi · CC BY-SA 4.0'), findsOneWidget);
    expect(find.textContaining('لا يجري تنزيل الصور من الإنترنت'), findsOneWidget);
    expect(find.byType(Image), findsOneWidget);

    await tester.tap(find.text('حفظ على الجهاز'));
    await tester.pumpAndSettle();

    final saved = File('${directory.path}/wallpapers/${item.id}.jpg');
    expect(await saved.exists(), isTrue);
    expect(find.text('حُفظت الصورة محلياً على جهازك.'), findsOneWidget);
    expect(find.text('محفوظة'), findsOneWidget);
  });

  testWidgets('opens a full local image preview', (tester) async {
    await tester.pumpWidget(ProviderScope(
      overrides: [
        wallpaperRepositoryProvider.overrideWithValue(_WallpaperRepository([item])),
      ],
      child: const MaterialApp(home: WallpapersScreen()),
    ));
    await tester.pumpAndSettle();

    await tester.tap(find.byWidgetPredicate(
      (widget) => widget is InkWell && widget.onTap != null,
    ).first);
    await tester.pumpAndSettle();

    expect(find.text('إغلاق المعاينة'), findsOneWidget);
    expect(find.text('حفظ الصورة على الجهاز'), findsOneWidget);
    expect(find.text('تصوير Bahr Alwatifi · CC BY-SA 4.0'), findsOneWidget);
  });
}
