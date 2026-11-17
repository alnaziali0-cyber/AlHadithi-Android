import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:khazain_al_huda/core/models.dart';
import 'package:khazain_al_huda/core/providers.dart';
import 'package:khazain_al_huda/features/home/home_screen.dart';

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

  testWidgets('home prayer summary displays API Hijri date, source, and time',
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
        governoratesProvider.overrideWith((ref) async => [
              Governorate(
                id: 'baghdad',
                nameAr: 'بغداد',
                nameEn: 'Baghdad',
                latitude: 33.3152,
                longitude: 44.3661,
                timezone: 'Asia/Baghdad',
                coordinatesQuality: 'approximate_governorate_center',
              ),
            ]),
      ],
      child: const MaterialApp(home: Scaffold(body: HomeScreen())),
    ));
    await tester.pumpAndSettle();

    expect(find.text('١٥ جمادى الآخرة ١٤٤٨ هـ'), findsOneWidget);
    expect(find.textContaining('خدمة MisrQuran'), findsWidgets);
    expect(find.text('05:01'), findsWidgets);
  });
}
