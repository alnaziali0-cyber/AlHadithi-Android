import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:khazain_al_huda/core/quran_audio_service.dart';
import 'package:khazain_al_huda/features/radio/audio_library_screen.dart';

void main() {
  testWidgets('shows a clear unavailable state when no local audio exists',
      (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: AudioLibraryScreen(
            loadLocalAudio: () async => const <LocalQuranAudio>[],
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('مكتبة الصوتيات'), findsOneWidget);
    expect(find.textContaining('لا تتوفر حالياً ملفات تلاوة محلية'), findsOneWidget);
    expect(find.widgetWithText(FilledButton, 'فتح المصحف'), findsOneWidget);
    expect(find.textContaining('https://'), findsNothing);
    expect(find.text('فتح صفحة المصدر'), findsNothing);
    expect(find.byIcon(Icons.open_in_new_rounded), findsNothing);
  });

  testWidgets('lists existing local audio files without displaying external URLs',
      (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: AudioLibraryScreen(
            loadLocalAudio: () async => [
              LocalQuranAudio(
                surahNumber: 1,
                file: File('/local/test/surah_001.mp3'),
              ),
            ],
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('تلاوة محفوظة · السورة 1'), findsOneWidget);
    expect(find.text('ملف محلي على هذا الجهاز'), findsOneWidget);
    expect(find.textContaining('https://'), findsNothing);
    expect(find.byIcon(Icons.open_in_new_rounded), findsNothing);
  });
}
