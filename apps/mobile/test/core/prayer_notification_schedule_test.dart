import 'package:flutter_test/flutter_test.dart';
import 'package:khazain_al_huda/core/models.dart';
import 'package:khazain_al_huda/core/prayer_notification_schedule.dart';

PrayerTimesResult _day(String date, {bool stale = false}) => PrayerTimesResult(
      date: date,
      methodLabel: 'جعفري',
      definitions: const {},
      isStale: stale,
      displayTimes: const {
        'fajr': '04:30',
        'sunrise': '05:55',
        'dhuhr': '11:50',
        'maghrib': '17:10',
        'isha': '18:30',
      },
    );

void main() {
  group('buildPrayerNotificationEntries', () {
    test('creates four prayer alerts per day and excludes reference events', () {
      final now = DateTime(2026, 11, 14, 0, 0);
      final entries = buildPrayerNotificationEntries(
        dailyTimes: {'2026-11-14': _day('2026-11-14')},
        now: now,
        offsets: const {},
      );
      expect(entries.map((entry) => entry.prayerKey).toList(), ['fajr', 'dhuhr', 'maghrib', 'isha']);
      expect(entries, hasLength(4));
    });

    test('applies saved minute offsets and excludes alerts already in the past', () {
      final now = DateTime(2026, 11, 14, 5, 0);
      final entries = buildPrayerNotificationEntries(
        dailyTimes: {'2026-11-14': _day('2026-11-14')},
        now: now,
        offsets: const {'dhuhr': 7, 'maghrib': -5},
      );
      expect(entries.map((entry) => entry.scheduledAt.hour * 60 + entry.scheduledAt.minute), [717, 1025, 1110]);
      expect(entries.any((entry) => entry.prayerKey == 'fajr'), isFalse);
    });

    test('keeps entries within the fourteen-day rolling window and marks stale cache', () {
      final now = DateTime(2026, 11, 14, 0, 0);
      final entries = buildPrayerNotificationEntries(
        dailyTimes: {
          '2026-11-13': _day('2026-11-13'),
          '2026-11-14': _day('2026-11-14', stale: true),
          '2026-11-28': _day('2026-11-28'),
        },
        now: now,
        offsets: const {},
      );
      expect(entries.every((entry) => entry.scheduledAt.day == 14), isTrue);
      expect(entries, hasLength(4));
      expect(entries.every((entry) => entry.isStale), isTrue);
    });
  });
}
