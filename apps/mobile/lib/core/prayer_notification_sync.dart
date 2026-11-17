import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'app_preferences.dart';
import 'models.dart';
import 'misr_quran_prayer_times.dart';
import 'prayer_notification_schedule.dart';
import 'prayer_notification_service.dart';
import 'providers.dart';

Future<Map<String, PrayerTimesResult>> loadPrayerNotificationWindow({
  required DateTime now,
  required Future<PrayerTimesResult?> Function() loadToday,
  required Future<PrayerTimesResult?> Function(DateTime requestedDay)
      loadLocalDate,
}) async {
  final today = prayerBaghdadCalendarDate(now);
  final dailyTimes = <String, PrayerTimesResult>{};

  String keyFor(DateTime date) =>
      '${date.year.toString().padLeft(4, '0')}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';

  final todayKey = keyFor(today);
  final todayResult = await loadToday();
  if (todayResult != null && todayResult.date == todayKey) {
    dailyTimes[todayKey] = todayResult;
  }

  for (var offset = 1; offset < prayerNotificationWindowDays; offset++) {
    final date = DateTime(today.year, today.month, today.day + offset);
    final result = await loadLocalDate(date);
    final dayKey = keyFor(date);
    if (result != null && result.date == dayKey) dailyTimes[dayKey] = result;
  }
  return dailyTimes;
}

/// Refreshes a rolling 12-day window. Five prayer alerts per day means at
/// most 60 pending notifications, below iOS's 64-notification limit.
Future<int> syncPrayerNotifications(WidgetRef ref) async {
  final preferences = ref.read(appPreferencesProvider);
  final service = PrayerNotificationService.instance;
  if (!preferences.prayerNotificationsEnabled) {
    await service.cancelPrayerAlerts();
    return 0;
  }

  final now = DateTime.now();
  final dailyTimes = await loadPrayerNotificationWindow(
    now: now,
    loadToday: () async {
      ref.invalidate(prayerTimesProvider);
      return ref.read(prayerTimesProvider.future);
    },
    loadLocalDate: (date) async {
      final dayProvider = prayerTimesForDateProvider(date);
      ref.invalidate(dayProvider);
      return ref.read(dayProvider.future);
    },
  );

  if (dailyTimes.isEmpty) {
    await service.cancelPrayerAlerts();
    throw StateError('لا تتوفر مواقيت صالحة لجدولة التنبيهات.');
  }

  final entries = buildPrayerNotificationEntries(
    dailyTimes: dailyTimes,
    now: now,
    offsets: preferences.prayerOffsets,
  );
  final scheduled = await service.schedulePrayerAlerts(
    entries: entries,
    timezoneName: MisrQuranPrayerTimesRepository.timezoneName,
  );
  return scheduled;
}
