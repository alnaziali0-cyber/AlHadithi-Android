import 'app_preferences.dart';
import 'models.dart';
import 'misr_quran_prayer_times.dart';
import 'package:timezone/timezone.dart' as tz;

const prayerNotificationWindowDays = 12;
const prayerNotificationIdBase = 41000;

const _notifiablePrayers = <(String, String)>[
  ('fajr', 'الفجر'),
  ('dhuhr', 'الظهر'),
  ('asr', 'العصر'),
  ('maghrib', 'المغرب'),
  ('isha', 'العشاء'),
];

class PrayerNotificationEntry {
  const PrayerNotificationEntry({
    required this.id,
    required this.prayerKey,
    required this.prayerNameAr,
    required this.scheduledAt,
    required this.isStale,
    required this.sourceLabel,
  });

  final int id;
  final String prayerKey;
  final String prayerNameAr;
  final DateTime scheduledAt;
  final bool isStale;
  final String sourceLabel;
}

/// Builds one-shot local alerts for the next [prayerNotificationWindowDays].
/// Only prayer events returned by the selected calculation profile are used;
/// sunrise and sunset are deliberately excluded as they are reference events.
List<PrayerNotificationEntry> buildPrayerNotificationEntries({
  required Map<String, PrayerTimesResult> dailyTimes,
  required DateTime now,
  required Map<String, int> offsets,
}) {
  final baghdadNow = prayerBaghdadDateTime(now);
  final location = tz.getLocation(MisrQuranPrayerTimesRepository.timezoneName);
  final start = DateTime.utc(baghdadNow.year, baghdadNow.month, baghdadNow.day);
  final entries = <PrayerNotificationEntry>[];
  final sortedDays = dailyTimes.keys.toList()..sort();

  for (final dayKey in sortedDays) {
    final date = DateTime.tryParse(dayKey);
    final result = dailyTimes[dayKey];
    if (date == null || result == null || result.date != dayKey) continue;
    final dayOffset =
        DateTime.utc(date.year, date.month, date.day).difference(start).inDays;
    if (dayOffset < 0 || dayOffset >= prayerNotificationWindowDays) continue;

    final adjusted = applyPrayerOffsets(result.displayTimes, offsets);
    for (var prayerIndex = 0;
        prayerIndex < _notifiablePrayers.length;
        prayerIndex++) {
      final item = _notifiablePrayers[prayerIndex];
      final hhmm = adjusted[item.$1];
      if (hhmm == null) continue;
      final parts = hhmm.split(':');
      if (parts.length != 2) continue;
      final hour = int.tryParse(parts[0]);
      final minute = int.tryParse(parts[1]);
      if (hour == null ||
          minute == null ||
          hour < 0 ||
          hour > 23 ||
          minute < 0 ||
          minute > 59) {
        continue;
      }
      final scheduledAt = tz.TZDateTime(
          location, date.year, date.month, date.day, hour, minute);
      if (!scheduledAt.isAfter(baghdadNow)) continue;

      entries.add(PrayerNotificationEntry(
        id: prayerNotificationIdBase + dayOffset * 10 + prayerIndex,
        prayerKey: item.$1,
        prayerNameAr: item.$2,
        scheduledAt: scheduledAt,
        isStale: result.isStale,
        sourceLabel: result.sourceLabel,
      ));
    }
  }

  entries.sort((a, b) => a.scheduledAt.compareTo(b.scheduledAt));
  return entries;
}
