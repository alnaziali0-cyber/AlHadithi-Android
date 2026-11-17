import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:khazain_al_huda/features/content/ziyara_library_screen.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test('loads six named OCR files and groups their headings and pages', () async {
    final volumes = await loadZiyaraVolumes();

    expect(
      volumes.map((volume) => volume.id).toList(),
      ['volume_0', 'volume_1', 'volume_2', 'volume_4', 'volume_5', 'volume_6'],
    );
    expect(volumes.fold<int>(0, (sum, volume) => sum + volume.pages), 2468);
    expect(
      volumes.map((volume) => volume.textFileName).toList(),
      [
        'volume_0.txt',
        'volume_1.txt',
        'volume_2.txt',
        'volume_4.txt',
        'volume_5.txt',
        'volume_6.txt',
      ],
    );
    expect(
      volumes.every((volume) => volume.outlineAssetPath.endsWith('.md')),
      isTrue,
    );

    final documents = await Future.wait(volumes.map(loadZiyaraTextDocument));
    expect(documents.every((document) => document.pages.isNotEmpty), isTrue);
    expect(
      documents.first.pages.any((page) => page.title == 'صفحة PDF 001'),
      isTrue,
    );
    expect(
      documents.first.sections.any((section) =>
          section.title.contains('الصفحات التمهيدية')),
      isTrue,
    );
    expect(documents.first.pages.first.text, contains('مسودة OCR'));
    expect(
      documents.every((document) => document.pages.any(
            (page) => page.sectionPath.isNotEmpty,
          )),
      isTrue,
    );
  });

  testWidgets('volume screen shows filename, OCR warning, and in-app text',
      (tester) async {
    final router = _testRouter();
    addTearDown(router.dispose);

    await tester.pumpWidget(MaterialApp.router(routerConfig: router));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(
      find.text('المقدمة'),
      360,
      scrollable: find.byType(Scrollable).first,
    );
    await tester.tap(find.text('المقدمة'));
    await tester.pumpAndSettle();

    expect(find.textContaining('volume_0.txt'), findsOneWidget);
    expect(find.textContaining('مسودة OCR آلية غير مدققة حرفياً بالكامل'),
        findsOneWidget);
    expect(find.text('النص المكتوب'), findsOneWidget);
    expect(find.text('فهرس المجلد'), findsOneWidget);
    expect(find.byType(SelectableText), findsOneWidget);
    expect(find.textContaining('الصفحات التمهيدية'), findsOneWidget);

    await tester.tap(find.text('فهرس المجلد'));
    await tester.pumpAndSettle();
    expect(find.textContaining('فهرس عناوين مستقل'), findsOneWidget);
  });

  testWidgets('volume five keeps the existing text-only outline tab',
      (tester) async {
    final router = _testRouter();
    addTearDown(router.dispose);

    await tester.pumpWidget(MaterialApp.router(routerConfig: router));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(
      find.text('الزيارات الجامعة للأئمة (عليهم السلام)'),
      360,
      scrollable: find.byType(Scrollable).first,
    );
    await tester.tap(find.text('الزيارات الجامعة للأئمة (عليهم السلام)'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('فهرس المجلد'));
    await tester.pumpAndSettle();

    expect(find.textContaining('الزيارات المطلقة'), findsOneWidget);
    expect(find.textContaining('ملف أو رابط'), findsNothing);
  });
}

GoRouter _testRouter() => GoRouter(
      initialLocation: '/visits',
      routes: [
        GoRoute(
          path: '/visits',
          builder: (context, state) => const ZiyaraLibraryScreen(),
        ),
        GoRoute(
          path: '/visits/:volumeId',
          builder: (context, state) => ZiyaraVolumeTextScreen(
            volumeId: state.pathParameters['volumeId'] ?? '',
          ),
        ),
      ],
    );
