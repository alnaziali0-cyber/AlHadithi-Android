import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/app_preferences.dart';
import '../../core/data_maintenance.dart' show selectedLocationSourceProvider;
import '../../core/governorate_selection_sheet.dart';
import '../../core/providers.dart';
import '../qibla/qibla_screen.dart' show qiblaLocationProvider;
import 'settings_widgets.dart';
import 'support_sheet.dart';

const _appVersionLabel = '0.2.0';

class SettingsScreen extends ConsumerWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final themeMode = ref.watch(themeModeProvider);
    final prefs = ref.watch(appPreferencesProvider);
    final selectedGovernorate = ref.watch(selectedGovernorateProvider);
    final governorates = ref.watch(governoratesProvider);
    final governorateName = governorates.whenOrNull(data: (items) {
          for (final item in items) {
            if (item.id == selectedGovernorate) return item.nameAr;
          }
          return 'لم تُحدد';
        }) ??
        (selectedGovernorate == null ? 'لم تُحدد' : 'جارٍ التحميل');

    return CustomScrollView(
      slivers: [
        SliverPadding(
          padding: const EdgeInsets.fromLTRB(17, 18, 17, 34),
          sliver: SliverList(
            delegate: SliverChildListDelegate([
              Row(
                children: [
                  Expanded(
                      child: Text('الإعدادات',
                          style: theme.textTheme.headlineSmall
                              ?.copyWith(fontWeight: FontWeight.w900))),
                  IconButton.filledTonal(
                    tooltip: 'الدعم والتواصل',
                    onPressed: () => SupportSheet.show(context),
                    icon: const Icon(Icons.headset_mic_rounded),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              SettingsGroup(
                title: 'الإشعارات والأذان',
                children: [
                  SettingsTile(
                    icon: Icons.notifications_none_rounded,
                    title: 'الإشعارات',
                    subtitle: prefs.prayerNotificationsEnabled
                        ? 'تنبيهات الصلاة مفعّلة'
                        : 'إعداد تنبيه دخول وقت الصلاة',
                    value: prefs.prayerNotificationsEnabled ? 'مفعّل' : 'متوقف',
                    onTap: () => context.push('/settings/prayer'),
                  ),
                  SettingsTile(
                    icon: Icons.campaign_outlined,
                    title: 'الأذان',
                    subtitle: 'يظهر تنبيه الأذان عند دخول وقت الصلاة فقط',
                    onTap: () => context.push('/settings/prayer'),
                  ),
                  SettingsTile(
                    icon: Icons.access_time_rounded,
                    title: 'أوقات الصلاة',
                    subtitle: 'عرض المواقيت وطريقة الحساب والتعديلات',
                    onTap: () => context.go('/prayer'),
                  ),
                  SettingsTile(
                    icon: Icons.location_on_outlined,
                    title: 'الموقع',
                    subtitle: 'تحديد المحافظة أو استخدام GPS بدقة عالية',
                    value: governorateName,
                    onTap: () => _changeGovernorate(context, ref),
                  ),
                ],
              ),
              const SizedBox(height: 18),
              SettingsGroup(
                title: 'التخصيص',
                children: [
                  const SettingsTile(
                    icon: Icons.language_rounded,
                    title: 'لغة التطبيق',
                    value: 'العربية',
                  ),
                  SettingsTile(
                    icon: Icons.dark_mode_outlined,
                    title: 'مظهر التطبيق',
                    value: _themeLabel(themeMode),
                    onTap: () => _pickTheme(context, ref, themeMode),
                  ),
                  SettingsTile(
                    icon: Icons.format_size_rounded,
                    title: 'حجم الخط',
                    value: prefs.textSize.labelAr,
                    onTap: () => _pickTextSize(context, ref, prefs.textSize),
                  ),
                  SettingsTile(
                    icon: Icons.text_fields_rounded,
                    title: 'نوع الخط',
                    subtitle: 'خط الواجهة فقط؛ خط القرآن يبقى مستقلاً',
                    value: prefs.appFontFamily.labelAr,
                    onTap: () =>
                        _pickAppFont(context, ref, prefs.appFontFamily),
                  ),
                  SettingsSwitchTile(
                    icon: Icons.vibration_rounded,
                    title: 'الاهتزاز عند التسبيح',
                    subtitle: 'اهتزاز خفيف عند كل ضغطة في المسبحة',
                    value: prefs.hapticsEnabled,
                    onChanged: (value) => ref
                        .read(appPreferencesProvider.notifier)
                        .setHapticsEnabled(value),
                  ),
                ],
              ),
              const SizedBox(height: 18),
              SettingsGroup(
                title: 'المكتبة والبيانات',
                children: [
                  SettingsTile(
                    icon: Icons.menu_book_outlined,
                    title: 'المكتبة الإسلامية',
                    subtitle: 'الكتب والمراجع المتاحة والموثقة',
                    onTap: () => context.go('/library'),
                  ),
                  SettingsTile(
                    icon: Icons.source_outlined,
                    title: 'المراجع والمصادر',
                    subtitle: 'المصادر وشروط التراخيص والنشر',
                    onTap: () => context.push('/sources'),
                  ),
                  SettingsTile(
                    icon: Icons.favorite_border_rounded,
                    title: 'المفضلة',
                    subtitle: 'الكتب والمواد التي حفظتها',
                    onTap: () => context.push('/favorites'),
                  ),
                  SettingsTile(
                    icon: Icons.download_for_offline_outlined,
                    title: 'إدارة البيانات والتنزيلات',
                    subtitle: 'إدارة النسخ المحلية وبيانات الخصوصية',
                    onTap: () => context.push('/settings/privacy'),
                  ),
                ],
              ),
              const SizedBox(height: 18),
              SettingsGroup(
                title: 'ادعم التطبيق',
                children: [
                  SettingsTile(
                    icon: Icons.share_outlined,
                    title: 'مشاركة التطبيق',
                    subtitle: 'نسخ تعريف التطبيق لمشاركته',
                    onTap: () => _shareApp(context),
                  ),
                  SettingsTile(
                    icon: Icons.star_outline_rounded,
                    title: 'تقييم التطبيق',
                    subtitle: 'يتوفر رابط التقييم بعد نشر التطبيق في المتجر',
                    onTap: () => _showRateNotice(context),
                  ),
                  SettingsTile(
                    icon: Icons.info_outline_rounded,
                    title: 'عن التطبيق',
                    subtitle: 'الإصدار $_appVersionLabel',
                    onTap: () => _showAbout(context),
                  ),
                  SettingsTile(
                    icon: Icons.headset_mic_outlined,
                    title: 'تواصل معنا',
                    subtitle: 'واتساب وإنستغرام وتيك توك',
                    onTap: () => SupportSheet.show(context),
                  ),
                ],
              ),
              const SizedBox(height: 14),
              Center(
                  child: Text('الإصدار $_appVersionLabel',
                      style: theme.textTheme.bodySmall)),
            ]),
          ),
        ),
      ],
    );
  }

  static String _themeLabel(ThemeMode mode) => switch (mode) {
        ThemeMode.light => 'الوضع العادي',
        ThemeMode.dark => 'الوضع المظلم',
        _ => 'تلقائي',
      };

  static Future<void> _pickTheme(
      BuildContext context, WidgetRef ref, ThemeMode current) async {
    final chosen = await showSettingsOptionSheet<ThemeMode>(
      context: context,
      title: 'مظهر التطبيق',
      current: current,
      description: 'اختر المظهر الذي يناسبك.',
      options: const [
        SettingsOption(
            value: ThemeMode.system,
            label: 'تلقائي',
            description: 'يتبع إعداد الجهاز'),
        SettingsOption(
            value: ThemeMode.light,
            label: 'الوضع العادي',
            description: 'ألوان فاتحة مع الخلفية المرفقة'),
        SettingsOption(
            value: ThemeMode.dark,
            label: 'الوضع المظلم',
            description: 'ألوان داكنة شفافة'),
      ],
    );
    if (chosen == null || chosen == current || !context.mounted) return;
    await ref.read(themeModeProvider.notifier).setMode(chosen);
  }

  static Future<void> _pickTextSize(
      BuildContext context, WidgetRef ref, AppTextSize current) async {
    final chosen = await showSettingsOptionSheet<AppTextSize>(
      context: context,
      title: 'حجم الخط',
      current: current,
      description:
          'يتغير الحجم فقط؛ ويمكن اختيار خط الواجهة بشكل منفصل، ويبقى خط القرآن مستقلاً.',
      options: [
        for (final size in AppTextSize.values)
          SettingsOption(value: size, label: size.labelAr),
      ],
    );
    if (chosen == null || chosen == current || !context.mounted) return;
    await ref.read(appPreferencesProvider.notifier).setTextSize(chosen);
  }

  static Future<void> _pickAppFont(
      BuildContext context, WidgetRef ref, AppFontFamily current) async {
    final chosen = await showSettingsOptionSheet<AppFontFamily>(
      context: context,
      title: 'نوع خط التطبيق',
      current: current,
      description:
          'اختر خطاً لواجهات التطبيق. لا يؤثر الاختيار في خط المصحف الشريف.',
      options: [
        for (final font in AppFontFamily.values)
          SettingsOption(
            value: font,
            label: font.labelAr,
            description: font == AppFontFamily.notoNaskh
                ? 'واضح ومناسب افتراضياً للقراءة العربية'
                : 'يُستخدم في واجهة التطبيق فقط',
          ),
      ],
    );
    if (chosen == null || chosen == current || !context.mounted) return;
    await ref.read(appPreferencesProvider.notifier).setAppFontFamily(chosen);
  }

  static Future<void> _changeGovernorate(
      BuildContext context, WidgetRef ref) async {
    await showGovernorateSelectionSheet(context);
    if (!context.mounted) return;
    ref.invalidate(selectedLocationSourceProvider);
    ref.invalidate(qiblaLocationProvider);
    ref.invalidate(prayerTimesProvider);
  }

  static Future<void> _shareApp(BuildContext context) async {
    await Clipboard.setData(const ClipboardData(
      text:
          'خزائن الهدى — تطبيق للقرآن الكريم ومواقيت الصلاة والأدعية والزيارات والمكتبة الإسلامية.',
    ));
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
          content: Text('تم نسخ تعريف التطبيق. يمكنك مشاركته الآن.')));
    }
  }

  static Future<void> _showRateNotice(BuildContext context) async {
    await showDialog<void>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('تقييم التطبيق'),
        content: const Text(
            'سيُضاف رابط التقييم المباشر بعد نشر النسخة الرسمية على متجر التطبيقات.'),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(dialogContext),
              child: const Text('حسناً'))
        ],
      ),
    );
  }

  static void _showAbout(BuildContext context) {
    showAboutDialog(
      context: context,
      applicationName: 'خَزَائِنُ الهُدَى',
      applicationVersion: _appVersionLabel,
      applicationLegalese:
          'تطبيق معرفي وعبادي. تُعرض المواد بحسب مصادرها وشروط استخدامها.',
      children: const [
        Text(
            'القرآن الكريم، مواقيت الصلاة، القبلة، المسبحة، الأدعية، الزيارات والمكتبة الإسلامية.')
      ],
    );
  }
}
