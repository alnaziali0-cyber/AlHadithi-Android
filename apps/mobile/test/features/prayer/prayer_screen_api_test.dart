import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:khazain_al_huda/core/models.dart';
import 'package:khazain_al_huda/core/providers.dart';
import 'package:khazain_al_huda/features/prayer/prayer_screen.dart';

PrayerTimesResult _apiDay() => PrayerTimesResult(
      date: '2026-11-16',
      displayTimes: const {
        'fajr': '05:01',
        'sunrise': '06:34',
        'dhuhr': '11:47',
        'asr': '14:40',
        'sunset': '17:00',
        'maghrib': '17:00',
        'isha': '18:23',
      },
      methodLabel: 'خدمة MisrQuran — غير موثقة فقهياً',
      definitions: const {},
      isStale: false,
      source: PrayerTimesSource.misrQuranApi,
      hijriDate: '١٥ جمادى الآخرة ١٤٤٨ هـ',
      locationName: 'بغداد',
      coordinateAccuracy: 'approximate_governorate_center',
      coordinateSource: 'governorate_center',
      locationVerified: false,
    );

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  testWidgets(
      'prayer screen shows API time/Hijri/source and omits absent midnight rows',
      (tester) async {
    SharedPreferences.setMockInitialValues({
      'selected_governorate': 'baghdad',
      'selected_location_source': 'manual',
    });
    final prefs = await SharedPreferences.getInstance();
    await tester.pumpWidget(ProviderScope(
      overrides: [
        sharedPreferencesProvider.overrideWithValue(prefs),
        prayerTimesProvider.overrideWith((ref) async => _apiDay()),
        prayerCitiesProvider.overrideWith((ref) async => const []),
      ],
      child: const MaterialApp(home: Scaffold(body: PrayerScreen())),
    ));
    await tester.pumpAndSettle();

    expect(find.text('05:01'), findsOneWidget);
    expect(find.textContaining('١٥ جمادى الآخرة ١٤٤٨ هـ'), findsOneWidget);
    expect(find.textContaining('خدمة MisrQuran'), findsWidgets);
    expect(find.text('منتصف الليل (من الغروب)'), findsNothing);
    expect(find.text('منتصف الليل (من المغرب)'), findsNothing);
  });
}
