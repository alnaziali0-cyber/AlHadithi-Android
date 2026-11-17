import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:timezone/data/latest_all.dart' as tz_data;
import 'package:timezone/timezone.dart' as tz;

import 'prayer_notification_schedule.dart';

class PrayerNotificationService {
  PrayerNotificationService._();

  static final PrayerNotificationService instance =
      PrayerNotificationService._();

  static const _channelId = 'khazain_prayer_times';
  static const _channelName = 'تنبيهات مواقيت الصلاة';
  static const _channelDescription =
      'تنبيه عند دخول وقت الصلاة وفق الموقع وطريقة الحساب المختارين.';

  final FlutterLocalNotificationsPlugin _plugin =
      FlutterLocalNotificationsPlugin();
  bool _initialized = false;
  bool _exactAlarmAllowed = false;

  Future<void> initialize() async {
    if (_initialized) return;
    tz_data.initializeTimeZones();
    tz.setLocalLocation(tz.getLocation('Asia/Baghdad'));

    const settings = InitializationSettings(
      android: AndroidInitializationSettings('@mipmap/ic_launcher'),
      iOS: DarwinInitializationSettings(
        requestAlertPermission: false,
        requestBadgePermission: false,
        requestSoundPermission: false,
      ),
    );
    await _plugin.initialize(settings);
    final android = _plugin.resolvePlatformSpecificImplementation<
        AndroidFlutterLocalNotificationsPlugin>();
    if (android != null) {
      await android.createNotificationChannel(const AndroidNotificationChannel(
        _channelId,
        _channelName,
        description: _channelDescription,
        importance: Importance.max,
        playSound: true,
        enableVibration: true,
        audioAttributesUsage: AudioAttributesUsage.alarm,
      ));
      try {
        _exactAlarmAllowed =
            await android.canScheduleExactNotifications() ?? false;
      } catch (_) {
        _exactAlarmAllowed = false;
      }
    }
    _initialized = true;
  }

  /// Requests user-visible notification permission. Exact-alarm permission is
  /// requested separately; when declined, the caller can still use inexact
  /// alerts rather than silently disabling the schedule.
  Future<bool> requestPermissions() async {
    await initialize();
    final android = _plugin.resolvePlatformSpecificImplementation<
        AndroidFlutterLocalNotificationsPlugin>();
    if (android != null) {
      final requested = await android.requestNotificationsPermission();
      final notificationsAllowed =
          requested ?? await android.areNotificationsEnabled() ?? true;
      if (!notificationsAllowed) return false;
      var exactAllowed = false;
      try {
        exactAllowed = await android.canScheduleExactNotifications() ?? false;
        if (!exactAllowed) {
          await android.requestExactAlarmsPermission();
          exactAllowed = await android.canScheduleExactNotifications() ?? false;
        }
      } catch (_) {
        // A missing native permission declaration or OEM-specific restriction
        // must not prevent the inexact notification fallback from being used.
        exactAllowed = false;
      }
      _exactAlarmAllowed = exactAllowed;
      return true;
    }

    final ios = _plugin.resolvePlatformSpecificImplementation<
        IOSFlutterLocalNotificationsPlugin>();
    if (ios != null) {
      final allowed =
          await ios.requestPermissions(alert: true, badge: false, sound: true);
      return allowed ?? false;
    }
    return true;
  }

  Future<int> schedulePrayerAlerts({
    required List<PrayerNotificationEntry> entries,
    required String timezoneName,
  }) async {
    await initialize();
    await cancelPrayerAlerts();
    if (entries.isEmpty) return 0;

    tz.Location location;
    try {
      location = tz.getLocation(timezoneName);
    } catch (_) {
      location = tz.getLocation('Asia/Baghdad');
    }

    final details = NotificationDetails(
      android: const AndroidNotificationDetails(
        _channelId,
        _channelName,
        channelDescription: _channelDescription,
        importance: Importance.max,
        priority: Priority.high,
        category: AndroidNotificationCategory.alarm,
        playSound: true,
        enableVibration: true,
        visibility: NotificationVisibility.public,
        audioAttributesUsage: AudioAttributesUsage.alarm,
      ),
      iOS: const DarwinNotificationDetails(
        presentAlert: true,
        presentBadge: false,
        presentSound: true,
      ),
    );

    var scheduledCount = 0;
    for (final entry in entries) {
      final local = entry.scheduledAt;
      final scheduled = tz.TZDateTime(
        location,
        local.year,
        local.month,
        local.day,
        local.hour,
        local.minute,
      );
      if (!scheduled.isAfter(tz.TZDateTime.now(location))) continue;

      final staleNote = entry.isStale ? ' (بيانات محفوظة من آخر اتصال)' : '';
      await _plugin.zonedSchedule(
        entry.id,
        'حان وقت الأذان · صلاة ${entry.prayerNameAr}',
        'دخل وقت الصلاة بحسب ${entry.sourceLabel} والموقع المحدد$staleNote.',
        scheduled,
        details,
        androidScheduleMode: _exactAlarmAllowed
            ? AndroidScheduleMode.exactAllowWhileIdle
            : AndroidScheduleMode.inexactAllowWhileIdle,
        payload:
            'prayer:${entry.prayerKey}:${local.year.toString().padLeft(4, '0')}-${local.month.toString().padLeft(2, '0')}-${local.day.toString().padLeft(2, '0')}',
      );
      scheduledCount++;
    }
    return scheduledCount;
  }

  Future<void> cancelPrayerAlerts() async {
    await initialize();
    for (var id = prayerNotificationIdBase;
        id < prayerNotificationIdBase + prayerNotificationWindowDays * 10;
        id++) {
      await _plugin.cancel(id);
    }
  }

  Future<int> pendingPrayerAlertsCount() async {
    await initialize();
    final pending = await _plugin.pendingNotificationRequests();
    return pending
        .where((item) =>
            item.id >= prayerNotificationIdBase &&
            item.id <
                prayerNotificationIdBase + prayerNotificationWindowDays * 10)
        .length;
  }

  @visibleForTesting
  bool get exactAlarmAllowed => _exactAlarmAllowed;
}
