import 'package:flutter_test/flutter_test.dart';
import 'package:khazain_al_huda/core/app_preferences.dart';
import 'package:khazain_al_huda/core/models.dart';
import 'package:khazain_al_huda/core/prayer_notification_schedule.dart';
import 'package:khazain_al_huda/core/prayer_notification_sync.dart';
import 'package:timezone/data/latest_all.dart' as tz_data;

PrayerTimesResult _day(
  String date, {
  PrayerTimesSource source = PrayerTimesSource.localCalculation,
  bool stale = false,
}) =>
    PrayerTimesResult(
      date: date,
      methodLabel: 'اختبار',
      definitions: const {},
      isStale: stale,
      source: source,
      hijriDate: source == PrayerTimesSource.misrQuranApi
          ? '١٥ جمادى الآخرة ١٤٤٨ هـ'
          : null,
      displayTimes: const {
        'fajr': '04:30',
        'sunrise': '05:55',
        'dhuhr': '11:50',
        'asr': '15:10',
        'sunset': '17:10',
        'maghrib': '17:10',
        'isha': '18:30',
        'islamic_midnight': '23:10',
      },
    );

String _dateKey(DateTime date) =>
    '${date.year.toString().padLeft(4, '0')}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  tz_data.initializeTimeZones();

  test(
      'notification window loads today once from shared provider and the next 11 dates locally',
      () async {
    var todayCalls = 0;
    final futureDays = <String>[];
    final times = await loadPrayerNotificationWindow(
      now: DateTime.utc(2026, 11, 13, 21, 30),
      loadToday: () async {
        todayCalls++;
        return _day('2026-11-14', source: PrayerTimesSource.misrQuranApi);
      },
      loadLocalDate: (date) async {
        futureDays.add(_dateKey(date));
        return _day(_dateKey(date));
      },
    );

    expect(todayCalls, 1);
    expect(times.keys, contains('2026-11-14'));
    expect(times['2026-11-14']!.source, PrayerTimesSource.misrQuranApi);
    expect(futureDays, [
      '2026-11-15',
      '2026-11-16',
      '2026-11-17',
      '2026-11-18',
      '2026-11-19',
      '2026-11-20',
      '2026-11-21',
      '2026-11-22',
      '2026-11-23',
      '2026-11-24',
      '2026-11-25',
    ]);
    expect(
        futureDays.every((date) =>
            times[date]!.source == PrayerTimesSource.localCalculation),
        isTrue);
    expect(times, hasLength(prayerNotificationWindowDays));
  });

  test('today alert uses the same single adjusted Baghdad time shown on screen',
      () {
    final apiToday = _day('2026-11-14', source: PrayerTimesSource.misrQuranApi);
    final offsets = const {'fajr': 5};
    final visible = applyPrayerOffsets(apiToday.displayTimes, offsets);
    final entries = buildPrayerNotificationEntries(
      dailyTimes: {'2026-11-14': apiToday},
      now: DateTime.utc(2026, 11, 13, 19, 0),
      offsets: offsets,
    );
    final fajr = entries.singleWhere((entry) => entry.prayerKey == 'fajr');

    expect(visible['fajr'], '04:35');
    expect(fajr.scheduledAt.hour, 4);
    expect(fajr.scheduledAt.minute, 35);
    expect(fajr.scheduledAt.toUtc(), DateTime.utc(2026, 11, 14, 1, 35));
    expect(fajr.sourceLabel, 'خدمة MisrQuran (غير معتمدة فقهياً)');
    expect(apiToday.displayTimes['fajr'], '04:30');
  });

  test(
      'future alerts retain local source, apply offsets once, and never schedule reference midnight rows',
      () {
    final localTomorrow = _day('2026-11-15');
    final entries = buildPrayerNotificationEntries(
      dailyTimes: {'2026-11-15': localTomorrow},
      now: DateTime.utc(2026, 11, 14, 20, 0),
      offsets: const {'fajr': 5, 'maghrib': -5},
    );

    final fajr = entries.singleWhere((entry) => entry.prayerKey == 'fajr');
    expect(fajr.scheduledAt.hour, 4);
    expect(fajr.scheduledAt.minute, 35);
    expect(fajr.sourceLabel, 'حساب التطبيق المحلي (غير معتمد فقهياً)');
    expect(
        entries.any((entry) => entry.prayerKey.contains('midnight')), isFalse);
    expect(localTomorrow.displayTimes['fajr'], '04:30');
    expect(
        entries.every((entry) => entry.scheduledAt.toUtc().hour < 22), isTrue);
  });
}
