import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:khazain_al_huda/core/app_preferences.dart';
import 'package:khazain_al_huda/core/data_maintenance.dart';

// Authored in Round 8. Flutter/Dart were not available in the execution
// environment, so these tests have NOT been executed.

void main() {
  group('time shifting', () {
    test('shifts within the same day and keeps HH:mm format', () {
      expect(shiftHhmm('05:04', 5), '05:09');
      expect(shiftHhmm('05:04', -4), '05:00');
      expect(shiftHhmm('12:00', 0), '12:00');
    });

    test('clamps at day boundaries instead of wrapping to another day', () {
      expect(shiftHhmm('00:10', -30), '00:00');
      expect(shiftHhmm('23:50', 30), '23:59');
    });

    test('leaves malformed values untouched', () {
      expect(shiftHhmm('--:--', 5), '--:--');
      expect(shiftHhmm('25:00', 5), '25:00');
    });
  });

  group('applyPrayerOffsets', () {
    test('adjusts only adjustable prayers and never mutates the source map', () {
      final times = {'fajr': '04:50', 'sunrise': '06:10', 'dhuhr': '11:46', 'isha': '18:20'};
      final adjusted = applyPrayerOffsets(times, {'fajr': 10, 'isha': -5});

      expect(adjusted['fajr'], '05:00');
      expect(adjusted['sunrise'], '06:10');
      expect(adjusted['dhuhr'], '11:46');
      expect(adjusted['isha'], '18:15');
      expect(times['fajr'], '04:50');
    });
  });

  group('AppTextSize', () {
    test('falls back to standard for missing or unknown storage values', () {
      expect(AppTextSize.fromStorage(null), AppTextSize.standard);
      expect(AppTextSize.fromStorage('unknown'), AppTextSize.standard);
      expect(AppTextSize.fromStorage('extra_large'), AppTextSize.extraLarge);
    });
  });

  group('AppFontFamily', () {
    test('falls back to the bundled Arabic UI font for missing or unknown values', () {
      expect(AppFontFamily.fromStorage(null), AppFontFamily.notoNaskh);
      expect(AppFontFamily.fromStorage('missing-font'), AppFontFamily.notoNaskh);
      expect(AppFontFamily.fromStorage('geeza'), AppFontFamily.geeza);
    });
  });

  group('cache key selection', () {
    test('prayer-cache selection never includes method or offset preferences', () {
      final keys = [
        'prayer_method',
        'prayer_offset_fajr',
        'prayer_city_basra_2026-11-14_jafari_qom',
        'prayer_gps_abc_2026-11-14_jafari_qom',
        'prayer_governorate_baghdad_2026-11-14_tehran',
        'selected_governorate',
      ];
      expect(DataMaintenance.prayerCacheKeys(keys), [
        'prayer_city_basra_2026-11-14_jafari_qom',
        'prayer_gps_abc_2026-11-14_jafari_qom',
        'prayer_governorate_baghdad_2026-11-14_tehran',
      ]);
    });

    test('offline-content selection keeps reading positions and tasbih state', () {
      final keys = [
        'catalog_cache_book',
        'book_pages_v2_3_1_0_0',
        'book_page_v2_3_1_5',
        'book_pages_7_1_0_0',
        'governorates_catalog_cache_v1',
        'favorite_entries_cache',
        'book_progress_local_3',
        'tasbih_count',
      ];
      final selected = DataMaintenance.offlineCacheKeys(keys);

      expect(selected, containsAll([
        'catalog_cache_book',
        'book_pages_v2_3_1_0_0',
        'book_page_v2_3_1_5',
        'book_pages_7_1_0_0',
        'governorates_catalog_cache_v1',
        'favorite_entries_cache',
      ]));
      expect(selected, isNot(contains('book_progress_local_3')));
      expect(selected, isNot(contains('tasbih_count')));
    });
  });

  group('AppPreferencesController', () {
    test('persists and clamps prayer offsets, then resets display settings only', () async {
      SharedPreferences.setMockInitialValues({'theme_mode': 'dark'});
      final prefs = await SharedPreferences.getInstance();
      final controller = AppPreferencesController(prefs);

      await controller.setPrayerOffset('fajr', 99);
      expect(controller.state.offsetFor('fajr'), 30);
      expect(prefs.getInt('prayer_offset_fajr'), 30);

      await controller.setTextSize(AppTextSize.large);
      await controller.setAppFontFamily(AppFontFamily.beinArabic);
      await controller.setHapticsEnabled(false);
      await controller.resetDisplayAndPrayerSettings();

      expect(controller.state.textSize, AppTextSize.standard);
      expect(controller.state.appFontFamily, AppFontFamily.notoNaskh);
      expect(controller.state.hapticsEnabled, isTrue);
      expect(controller.state.offsetFor('fajr'), 0);
      expect(prefs.getString('theme_mode'), 'dark');
      expect(prefs.getString('app_font_family'), isNull);
    });

    test('persists the selected interface font across controller recreation', () async {
      SharedPreferences.setMockInitialValues({});
      final prefs = await SharedPreferences.getInstance();
      final controller = AppPreferencesController(prefs);
      await controller.setAppFontFamily(AppFontFamily.geeza);

      expect(prefs.getString('app_font_family'), 'geeza');
      expect(AppPreferencesController(prefs).state.appFontFamily, AppFontFamily.geeza);
    });

    test('persists automatic prayer notification preference', () async {
      SharedPreferences.setMockInitialValues({});
      final prefs = await SharedPreferences.getInstance();
      final controller = AppPreferencesController(prefs);
      await controller.setPrayerNotificationsEnabled(true);
      expect(controller.state.prayerNotificationsEnabled, isTrue);
      expect(prefs.getBool('prayer_notifications_enabled'), isTrue);
      await controller.setPrayerNotificationsEnabled(false);
      expect(prefs.getBool('prayer_notifications_enabled'), isFalse);
    });

    test('rejects offsets for prayers that are not adjustable', () async {
      SharedPreferences.setMockInitialValues({});
      final prefs = await SharedPreferences.getInstance();
      final controller = AppPreferencesController(prefs);

      await expectLater(controller.setPrayerOffset('asr', 5), throwsArgumentError);
    });
  });
}
