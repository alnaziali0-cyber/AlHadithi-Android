import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/app_preferences.dart';
import '../../core/data_maintenance.dart';
import '../../core/providers.dart';
import '../../core/prayer_notification_service.dart';
import '../../core/prayer_notification_sync.dart';
import 'settings_widgets.dart';

/// Prayer-time display settings: reference times and manual minute adjustments.
/// Adjustments change only what this device displays. The calculation, the
/// backend and the cached source data are never modified.
class PrayerSettingsScreen extends ConsumerWidget {
  const PrayerSettingsScreen({super.key});

  static const _adjustable = [
    ('fajr', 'الفجر', Icons.wb_twilight),
    ('dhuhr', 'الظهر', Icons.wb_sunny_outlined),
    ('asr', 'العصر', Icons.wb_cloudy_outlined),
    ('maghrib', 'المغرب', Icons.nights_stay_outlined),
    ('isha', 'العشاء', Icons.bedtime_outlined),
  ];

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final prefs = ref.watch(appPreferencesProvider);
    final notifier = ref.read(appPreferencesProvider.notifier);
    final times = ref.watch(prayerTimesProvider);
    final todaySource = times.maybeWhen(
      data: (result) => result?.sourceLabel ?? 'لا تتوفر مواقيت اليوم حالياً',
      orElse: () => 'مصدر اليوم غير متاح حالياً',
    );
    final baseTimes = times.maybeWhen(
      data: (result) => result?.displayTimes ?? const <String, String>{},
      orElse: () => const <String, String>{},
    );
    final anyOffset = prefs.prayerOffsets.values.any((value) => value != 0);

    return Scaffold(
      appBar: AppBar(title: const Text('تعديل أوقات الصلاة')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
        children: [
          SettingsNoticeCard(
            icon: Icons.info_outline_rounded,
            text:
                'مواقيت اليوم من $todaySource، ولا يعني ذلك اعتماداً مرجعياً. الأيام المقبلة تُحسب محلياً بالطريقة المختارة. التعديل هنا يغيّر العرض على هذا الجهاز فقط ولا يغيّر المصدر.',
          ),
          const SizedBox(height: 22),
          SettingsGroup(
            title: 'عرض الأوقات',
            children: [
              SettingsSwitchTile(
                icon: Icons.wb_twilight,
                title: 'إظهار الشروق والغروب',
                subtitle: 'أوقات مرجعية، وليست صلوات مفروضة',
                value: prefs.showReferenceTimes,
                onChanged: (value) => notifier.setShowReferenceTimes(value),
              ),
            ],
          ),
          const SizedBox(height: 22),
          SettingsGroup(
            title: 'التنبيهات التلقائية',
            footer:
                'تُجدول مواقيت اليوم من $todaySource، والأيام الـ11 التالية من الحساب المحلي المختار، للأيام الاثني عشر القادمة (حتى 60 إشعاراً). تُحدّث عند فتح التطبيق أو تغيير الموقع أو طريقة الحساب أو فروقات الدقائق. قد تمنع بعض الهواتف تشغيل المهام الخلفية ما لم تسمح للتطبيق بالعمل دون قيود للبطارية.',
            children: [
              SettingsSwitchTile(
                icon: Icons.notifications_active_outlined,
                title: 'تنبيه عند دخول وقت الصلاة',
                subtitle: prefs.prayerNotificationsEnabled
                    ? 'التنبيهات مفعّلة على هذا الجهاز'
                    : 'يتطلب إذن الإشعارات من النظام',
                value: prefs.prayerNotificationsEnabled,
                onChanged: (value) =>
                    _changePrayerNotifications(context, ref, value),
              ),
            ],
          ),
          const SizedBox(height: 12),
          const SettingsNoticeCard(
            icon: Icons.volume_up_outlined,
            text:
                'لا يظهر تنبيه الأذان إلا عند حلول موعد الصلاة المجدول. يستخدم الإشعار صوت النظام الافتراضي؛ ولا يوجد ملف أذان يعمل في الصفحة الرئيسية أو مشغّل أذان يدوي داخل هذا الإصدار.',
          ),
          const SizedBox(height: 22),
          SettingsGroup(
            title: 'تعديل يدوي بالدقائق',
            footer:
                'كل ضغطة تغيّر الوقت دقيقة واحدة، والحد الأقصى 30 دقيقة في الاتجاهين.',
            children: [
              for (final row in _adjustable)
                _PrayerOffsetRow(
                  label: row.$2,
                  icon: row.$3,
                  baseTime: baseTimes[row.$1],
                  offset: prefs.offsetFor(row.$1),
                  onChanged: (minutes) =>
                      notifier.setPrayerOffset(row.$1, minutes),
                ),
            ],
          ),
          const SizedBox(height: 16),
          SettingsTile(
            icon: Icons.restart_alt_rounded,
            title: 'إعادة التعديلات إلى الصفر',
            subtitle: anyOffset
                ? 'تعود الأوقات المعروضة إلى الحساب الأصلي'
                : 'لا توجد تعديلات محفوظة',
            onTap: anyOffset ? () => _resetOffsets(notifier) : null,
          ),
        ],
      ),
    );
  }

  Future<void> _changePrayerNotifications(
      BuildContext context, WidgetRef ref, bool enabled) async {
    final notifier = ref.read(appPreferencesProvider.notifier);
    if (!enabled) {
      await notifier.setPrayerNotificationsEnabled(false);
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('تم إيقاف تنبيهات الصلاة.')));
      }
      return;
    }

    try {
      final granted =
          await PrayerNotificationService.instance.requestPermissions();
      if (!granted) {
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
              content: Text(
                  'لم يُمنح إذن الإشعارات. فعّله من إعدادات الجهاز ثم أعد المحاولة.')));
        }
        return;
      }
      await notifier.setPrayerNotificationsEnabled(true);
      final count = await syncPrayerNotifications(ref);
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
            content: Text(
                'تم تفعيل التنبيهات وجدولة $count تنبيهاً للأيام القادمة.')));
      }
    } catch (error) {
      await notifier.setPrayerNotificationsEnabled(false);
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('تعذرت جدولة التنبيهات: $error')));
      }
    }
  }

  Future<void> _resetOffsets(AppPreferencesController notifier) async {
    for (final row in _adjustable) {
      await notifier.setPrayerOffset(row.$1, 0);
    }
  }
}

class _PrayerOffsetRow extends StatelessWidget {
  const _PrayerOffsetRow({
    required this.label,
    required this.icon,
    required this.baseTime,
    required this.offset,
    required this.onChanged,
  });

  final String label;
  final IconData icon;
  final String? baseTime;
  final int offset;
  final ValueChanged<int> onChanged;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final base = baseTime;
    final shown = base == null ? null : shiftHhmm(base, offset);
    final detail = base == null
        ? 'لم يُحسب الوقت بعد'
        : offset == 0
            ? 'الوقت المحسوب $base'
            : 'المحسوب $base، والمعروض $shown';
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 8),
      child: Row(
        children: [
          SettingsIconBadge(icon: icon),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(label,
                    style: const TextStyle(
                        fontWeight: FontWeight.w800, fontSize: 15)),
                const SizedBox(height: 3),
                Text(detail, style: theme.textTheme.bodySmall),
              ],
            ),
          ),
          IconButton.filledTonal(
            tooltip: 'تقديم الوقت دقيقة',
            onPressed: offset > prayerOffsetMinLimit
                ? () => onChanged(offset - 1)
                : null,
            icon: const Icon(Icons.remove_rounded),
          ),
          SizedBox(
            width: 52,
            child: Text(
              _signed(offset),
              textAlign: TextAlign.center,
              style: theme.textTheme.titleMedium
                  ?.copyWith(fontWeight: FontWeight.w900),
            ),
          ),
          IconButton.filledTonal(
            tooltip: 'تأخير الوقت دقيقة',
            onPressed: offset < prayerOffsetMaxLimit
                ? () => onChanged(offset + 1)
                : null,
            icon: const Icon(Icons.add_rounded),
          ),
        ],
      ),
    );
  }

  static String _signed(int value) =>
      value == 0 ? '0' : (value > 0 ? '+$value' : '$value');
}

/// Privacy and local-data controls. Every destructive action asks first and
/// states exactly what is removed and what is kept.
class PrivacyDataScreen extends ConsumerWidget {
  const PrivacyDataScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final preciseLocation = ref.watch(hasPreciseLocationProvider);
    final hasPrecise =
        preciseLocation.maybeWhen(data: (value) => value, orElse: () => false);
    final preciseStatus = preciseLocation.when(
      loading: () => 'جارٍ الفحص',
      error: (_, __) => 'تعذر الفحص',
      data: (value) => value ? 'محفوظ' : 'غير محفوظ',
    );

    return Scaffold(
      appBar: AppBar(title: const Text('الخصوصية والبيانات')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
        children: [
          const SettingsNoticeCard(
            icon: Icons.verified_user_outlined,
            text:
                'تصف هذه الصفحة ما يحفظه التطبيق فعلياً على هذا الجهاز. نص سياسة الخصوصية الرسمية لم يُنشر بعد في هذا الإصدار.',
          ),
          const SizedBox(height: 22),
          SettingsGroup(
            title: 'ما يُحفظ وأين',
            children: [
              const SettingsStatusRow(
                icon: Icons.favorite_outline,
                title: 'المفضلة',
                status: 'على الخادم',
                subtitle: 'مرتبطة بمعرّف مجهول يُولَّد لهذا الجهاز، دون حساب.',
              ),
              SettingsStatusRow(
                icon: Icons.location_on_outlined,
                title: 'الموقع الدقيق (GPS)',
                status: preciseStatus,
                subtitle:
                    'في التخزين الآمن للجهاز، وتُرسل عند طلب المواقيت، ولا تُحفظ في ذاكرة المواقيت على الخادم.',
              ),
              const SettingsStatusRow(
                icon: Icons.schedule_rounded,
                title: 'مواقيت الصلاة المخزنة',
                status: 'مؤقتة',
                subtitle:
                    'نسخ محلية لآخر مواقيت استُلمت، لاستخدامها دون اتصال.',
              ),
              const SettingsStatusRow(
                icon: Icons.menu_book_outlined,
                title: 'المحتوى دون اتصال',
                status: 'مؤقت',
                subtitle: 'فهارس وصفحات الكتب ونسخ المفضلة من آخر تحميل.',
              ),
              const SettingsStatusRow(
                icon: Icons.touch_app_outlined,
                title: 'التسبيح',
                status: 'على الجهاز',
                subtitle: 'العداد والهدف والذكر، ولا يُرسل إلى الخادم.',
              ),
              const SettingsStatusRow(
                icon: Icons.bookmark_border,
                title: 'موضع القراءة في الكتب',
                status: 'على الخادم',
                subtitle:
                    'يُحفظ على الخادم، ونسخة محلية على الجهاز لاستخدامها دون اتصال.',
              ),
              const SettingsStatusRow(
                icon: Icons.tune_rounded,
                title: 'التفضيلات',
                status: 'على الجهاز',
                subtitle:
                    'المظهر والمحافظة وطريقة الحساب وحجم الخط والاهتزاز وتعديلات الأوقات.',
              ),
            ],
          ),
          const SizedBox(height: 22),
          SettingsGroup(
            title: 'الموقع',
            children: [
              SettingsTile(
                icon: Icons.location_off_outlined,
                title: 'حذف الموقع الدقيق المحفوظ',
                subtitle: preciseLocation.hasError
                    ? 'تعذّر قراءة التخزين الآمن. حاول لاحقاً.'
                    : hasPrecise
                        ? 'تبقى المحافظة المختارة مرجعاً تقريبياً.'
                        : 'لا توجد إحداثيات محفوظة على هذا الجهاز.',
                danger: hasPrecise,
                onTap: hasPrecise
                    ? () => _clearPreciseLocation(context, ref)
                    : null,
              ),
            ],
          ),
          const SizedBox(height: 22),
          SettingsGroup(
            title: 'التخزين المؤقت',
            footer:
                'حذف هذه النسخ لا يمس المفضلة المحفوظة على الخادم، ولا اختيار المحافظة أو طريقة الحساب.',
            children: [
              SettingsTile(
                icon: Icons.schedule_rounded,
                title: 'مسح مواقيت الصلاة المخزنة',
                subtitle: 'تُعاد جلبها من الخادم عند الاتصال',
                onTap: () => _clearPrayerCache(context, ref),
              ),
              SettingsTile(
                icon: Icons.cleaning_services_outlined,
                title: 'مسح المحتوى المخزن دون اتصال',
                subtitle: 'يزيل نسخ الكتب والفهارس المحفوظة محلياً',
                onTap: () => _clearOfflineContent(context, ref),
              ),
            ],
          ),
          const SizedBox(height: 22),
          SettingsGroup(
            title: 'إعادة الضبط',
            children: [
              SettingsTile(
                icon: Icons.settings_backup_restore_rounded,
                title: 'إعادة ضبط العرض والتسبيح والمواقيت',
                subtitle:
                    'حجم الخط، والاهتزاز، وعرض الشروق، والتعديلات اليدوية',
                danger: true,
                onTap: () => _resetDisplay(context, ref),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Future<void> _clearPreciseLocation(
      BuildContext context, WidgetRef ref) async {
    final maintenance = ref.read(dataMaintenanceProvider);
    final confirmed = await confirmSettingsAction(
      context,
      title: 'حذف الموقع الدقيق؟',
      body:
          'ستُحذف إحداثيات GPS من هذا الجهاز مع مواقيتها المخزنة. ستبقى المحافظة المختارة وتُستخدم إحداثيات مركزها التقريبية.',
      confirmLabel: 'حذف',
      destructive: true,
    );
    if (!confirmed || !context.mounted) return;
    try {
      await maintenance.clearPreciseLocation();
    } catch (_) {
      if (context.mounted)
        showSettingsMessage(context, 'تعذر حذف الموقع. حاول مرة أخرى.');
      return;
    }
    // The deletion succeeded. Refresh dependent state only while this screen is still mounted.
    if (!context.mounted) return;
    ref.invalidate(hasPreciseLocationProvider);
    ref.invalidate(prayerTimesProvider);
    ref.invalidate(selectedLocationSourceProvider);
    showSettingsMessage(context, 'تم حذف الموقع الدقيق من هذا الجهاز.');
  }

  Future<void> _clearPrayerCache(BuildContext context, WidgetRef ref) async {
    final maintenance = ref.read(dataMaintenanceProvider);
    final confirmed = await confirmSettingsAction(
      context,
      title: 'مسح مواقيت الصلاة المخزنة؟',
      body:
          'ستُحذف النسخ المحلية لمواقيت الصلاة، وتُجلب من جديد عند الاتصال بالخادم. لن يتأثر اختيار المحافظة أو طريقة الحساب.',
      confirmLabel: 'مسح',
    );
    if (!confirmed || !context.mounted) return;
    try {
      final removed = await maintenance.clearPrayerTimesCache();
      if (!context.mounted) return;
      ref.invalidate(prayerTimesProvider);
      showSettingsMessage(context, 'تم مسح $removed مدخلاً من مواقيت الصلاة.');
    } catch (_) {
      if (context.mounted)
        showSettingsMessage(context, 'تعذر تنفيذ المسح. حاول مرة أخرى.');
    }
  }

  Future<void> _clearOfflineContent(BuildContext context, WidgetRef ref) async {
    final maintenance = ref.read(dataMaintenanceProvider);
    final confirmed = await confirmSettingsAction(
      context,
      title: 'مسح المحتوى المخزن دون اتصال؟',
      body:
          'ستُحذف صفحات الكتب والفهارس المحفوظة لقراءتها دون اتصال، ويمكن تحميلها مجدداً عند الاتصال. المفضلة على الخادم وموضع القراءة لا يتأثران.',
      confirmLabel: 'مسح',
    );
    if (!confirmed || !context.mounted) return;
    try {
      final removed = await maintenance.clearOfflineContent();
      if (context.mounted)
        showSettingsMessage(
            context, 'تم مسح $removed مدخلاً من المحتوى المخزن.');
    } catch (_) {
      if (context.mounted)
        showSettingsMessage(context, 'تعذر تنفيذ المسح. حاول مرة أخرى.');
    }
  }

  Future<void> _resetDisplay(BuildContext context, WidgetRef ref) async {
    final notifier = ref.read(appPreferencesProvider.notifier);
    final confirmed = await confirmSettingsAction(
      context,
      title: 'إعادة ضبط إعدادات العرض؟',
      body:
          'ستعود إعدادات حجم الخط والاهتزاز وعرض الشروق والغروب والتعديلات اليدوية للأوقات إلى قيمها الافتراضية. لن يتغير المظهر أو المحافظة أو طريقة الحساب.',
      confirmLabel: 'إعادة الضبط',
      destructive: true,
    );
    if (!confirmed || !context.mounted) return;
    try {
      await notifier.resetDisplayAndPrayerSettings();
      if (context.mounted)
        showSettingsMessage(
            context, 'أُعيدت إعدادات العرض والمواقيت إلى الافتراضي.');
    } catch (_) {
      if (context.mounted)
        showSettingsMessage(context, 'تعذرت إعادة الضبط. حاول مرة أخرى.');
    }
  }
}
