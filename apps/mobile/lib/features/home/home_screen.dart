import 'dart:async';
import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/app_preferences.dart';
import '../../core/governorate_selection_sheet.dart';
import '../../core/hijri_date.dart';
import '../../core/models.dart';
import '../../core/misr_quran_prayer_times.dart';
import '../../core/providers.dart';
import '../settings/support_sheet.dart';

class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  late DateTime _now;
  Timer? _clock;
  bool _showingFirstSelection = false;

  @override
  void initState() {
    super.initState();
    _now = DateTime.now();
    _clock = Timer.periodic(const Duration(seconds: 1), (_) {
      if (mounted) setState(() => _now = DateTime.now());
    });
    WidgetsBinding.instance
        .addPostFrameCallback((_) => _ensureFirstSelection());
  }

  Future<void> _ensureFirstSelection() async {
    if (!mounted ||
        _showingFirstSelection ||
        ref.read(selectedGovernorateProvider) != null) return;
    _showingFirstSelection = true;
    await showGovernorateSelectionSheet(context, requiredSelection: true);
    if (mounted) setState(() => _showingFirstSelection = false);
  }

  @override
  void dispose() {
    _clock?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final selectedId = ref.watch(selectedGovernorateProvider);
    final govs = ref.watch(governoratesProvider);
    final prayer = ref.watch(prayerTimesProvider);
    final offsets = ref
        .watch(appPreferencesProvider.select((prefs) => prefs.prayerOffsets));
    final hijri = toApproximateHijri(_now);
    final governorate = govs.whenOrNull(data: (items) {
      for (final item in items) {
        if (item.id == selectedId) return item;
      }
      return null;
    });

    return CustomScrollView(
      physics: const BouncingScrollPhysics(),
      slivers: [
        SliverPadding(
          padding: const EdgeInsets.fromLTRB(18, 12, 18, 26),
          sliver: SliverList(
            delegate: SliverChildListDelegate([
              Row(
                children: [
                  Expanded(
                    child: FilledButton.tonalIcon(
                      onPressed: () => context.push('/ahl-al-bayt'),
                      icon: const Icon(Icons.account_balance_rounded, size: 19),
                      label: const Text('الزيارات والأدعية'),
                      style: FilledButton.styleFrom(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 10, vertical: 8),
                        visualDensity: VisualDensity.compact,
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  OutlinedButton.icon(
                    onPressed: () => SupportSheet.show(context),
                    icon: const Icon(Icons.support_agent_rounded, size: 20),
                    label: const Text('الدعم'),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: Theme.of(context).colorScheme.primary,
                      side: BorderSide(
                          color: Theme.of(context)
                              .colorScheme
                              .primary
                              .withOpacity(.45)),
                      padding: const EdgeInsets.symmetric(
                          horizontal: 12, vertical: 8),
                      visualDensity: VisualDensity.compact,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 17),
              _HijriDatePill(
                hijri: hijri,
                sourceHijriDate: prayer.maybeWhen(
                  data: (result) => result?.hijriDate,
                  orElse: () => null,
                ),
              ),
              const SizedBox(height: 8),
              _PrayerSummaryCard(
                now: _now,
                governorateName: governorate?.nameAr,
                prayer: prayer,
                offsets: offsets,
                onLocationTap: () => showGovernorateSelectionSheet(context),
              ),
              const SizedBox(height: 14),
              const Text('الأقسام',
                  style: TextStyle(fontWeight: FontWeight.w900, fontSize: 16)),
              const SizedBox(height: 8),
              _HomeSectionsStrip(sections: _homeShortcuts),
              const SizedBox(height: 13),
              _DailyEventCard(
                  hijri: hijri, onTap: () => context.push('/events')),
            ]),
          ),
        ),
      ],
    );
  }
}

class _PrayerSummaryCard extends StatelessWidget {
  const _PrayerSummaryCard({
    required this.now,
    required this.governorateName,
    required this.prayer,
    required this.offsets,
    required this.onLocationTap,
  });

  final DateTime now;
  final String? governorateName;
  final AsyncValue<PrayerTimesResult?> prayer;
  final Map<String, int> offsets;
  final VoidCallback onLocationTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return ClipRRect(
      borderRadius: BorderRadius.circular(24),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 16, sigmaY: 16),
        child: Container(
          padding: const EdgeInsets.fromLTRB(15, 14, 15, 15),
          decoration: BoxDecoration(
            color: theme.brightness == Brightness.dark
                ? const Color(0xD91A211D)
                : const Color(0xDFFFFFFF),
            borderRadius: BorderRadius.circular(24),
            border: Border.all(color: Colors.white.withOpacity(.22)),
            boxShadow: [
              BoxShadow(
                  color: Colors.black.withOpacity(.12),
                  blurRadius: 18,
                  offset: const Offset(0, 8)),
            ],
          ),
          child: prayer.when(
            loading: () => const SizedBox(
                height: 152, child: Center(child: CircularProgressIndicator())),
            error: (_, __) => _PrayerEmptyState(
              onOpen: () => context.go('/prayer'),
              onLocationTap: onLocationTap,
              text:
                  'تعذر تحميل المواقيت حالياً. تحقق من الاتصال ثم أعد المحاولة.',
            ),
            data: (data) {
              if (data == null) {
                return _PrayerEmptyState(
                  onOpen: () => context.go('/prayer'),
                  onLocationTap: onLocationTap,
                  text: 'اختر المحافظة أو حدّد موقعك لعرض مواقيت الصلاة.',
                );
              }
              final times = applyPrayerOffsets(data.displayTimes, offsets);
              final next = _nextPrayer(times, now);
              const entries = [
                ('fajr', 'الفجر'),
                ('dhuhr', 'الظهر'),
                ('asr', 'العصر'),
                ('maghrib', 'المغرب'),
                ('isha', 'العشاء'),
              ];
              final visibleEntries = entries
                  .where((item) => times[item.$1] != null)
                  .toList(growable: false);
              return Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Row(
                    children: [
                      Icon(Icons.access_time_rounded,
                          color: theme.colorScheme.primary, size: 22),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          'مواقيت الصلاة',
                          style: theme.textTheme.titleLarge
                              ?.copyWith(fontWeight: FontWeight.w900),
                        ),
                      ),
                      if (data.isStale)
                        const Text('غير محدّثة',
                            style:
                                TextStyle(fontSize: 10, color: Colors.orange)),
                    ],
                  ),
                  const SizedBox(height: 3),
                  InkWell(
                    onTap: onLocationTap,
                    borderRadius: BorderRadius.circular(10),
                    child: Padding(
                      padding: const EdgeInsets.symmetric(vertical: 3),
                      child: Row(
                        children: [
                          Icon(Icons.location_on_outlined,
                              size: 15,
                              color:
                                  theme.colorScheme.onSurface.withOpacity(.65)),
                          const SizedBox(width: 4),
                          Expanded(
                              child: Text(
                                  data.locationName ??
                                      governorateName ??
                                      'الموقع المحدد',
                                  style: theme.textTheme.bodySmall)),
                          Text('تغيير',
                              style: TextStyle(
                                  color: theme.colorScheme.primary,
                                  fontWeight: FontWeight.w800,
                                  fontSize: 12)),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 9),
                  Text('مصدر مواقيت اليوم: ${data.sourceLabel}',
                      style: theme.textTheme.bodySmall),
                  if (data.hijriDate != null)
                    Text('التاريخ الهجري من المصدر: ${data.hijriDate}',
                        style: theme.textTheme.bodySmall),
                  const SizedBox(height: 7),
                  Container(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                    decoration: BoxDecoration(
                      color: theme.colorScheme.primary.withOpacity(.12),
                      borderRadius: BorderRadius.circular(15),
                      border: Border.all(
                          color: theme.colorScheme.primary.withOpacity(.18)),
                    ),
                    child: Row(
                      children: [
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text('الصلاة القادمة · ${next.$1}',
                                  style: theme.textTheme.bodySmall),
                              Text(next.$2,
                                  style: theme.textTheme.titleLarge?.copyWith(
                                      fontWeight: FontWeight.w900,
                                      color: theme.colorScheme.primary)),
                            ],
                          ),
                        ),
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.end,
                          children: [
                            Text('المتبقي', style: theme.textTheme.bodySmall),
                            Text(next.$3,
                                style: theme.textTheme.titleMedium
                                    ?.copyWith(fontWeight: FontWeight.w900)),
                          ],
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 11),
                  Row(
                    children: [
                      for (var index = 0;
                          index < visibleEntries.length;
                          index++) ...[
                        if (index > 0) const SizedBox(width: 4),
                        Expanded(
                          child: _PrayerChip(
                            label: visibleEntries[index].$2,
                            time: times[visibleEntries[index].$1]!,
                            active: visibleEntries[index].$1 == next.$4,
                          ),
                        ),
                      ],
                    ],
                  ),
                  if (data.isStale)
                    const Padding(
                      padding: EdgeInsets.only(top: 8),
                      child: Text('تعرض أوقاتاً محفوظة من آخر اتصال.',
                          style: TextStyle(fontSize: 10, color: Colors.orange)),
                    ),
                ],
              );
            },
          ),
        ),
      ),
    );
  }

  static (String, String, String, String) _nextPrayer(
      Map<String, String> values, DateTime now) {
    final baghdadNow = prayerBaghdadDateTime(now);
    const keys = [
      ('fajr', 'الفجر'),
      ('dhuhr', 'الظهر'),
      ('asr', 'العصر'),
      ('maghrib', 'المغرب'),
      ('isha', 'العشاء')
    ];
    for (final item in keys) {
      final value = values[item.$1];
      if (value == null) continue;
      final parts = value.split(':');
      if (parts.length != 2) continue;
      final time = prayerBaghdadWallTime(
        baghdadNow.year,
        baghdadNow.month,
        baghdadNow.day,
        int.tryParse(parts[0]) ?? 0,
        int.tryParse(parts[1]) ?? 0,
      );
      if (time.isAfter(baghdadNow))
        return (
          item.$2,
          value,
          _duration(time.difference(baghdadNow)),
          item.$1
        );
    }
    final fajr = values['fajr'];
    if (fajr == null) return ('الفجر القادم', '—', '--:--:--', 'fajr');
    final parts = fajr.split(':');
    final tomorrow = prayerBaghdadWallTime(
      baghdadNow.year,
      baghdadNow.month,
      baghdadNow.day + 1,
      int.parse(parts[0]),
      int.parse(parts[1]),
    );
    return (
      'الفجر القادم',
      fajr,
      _duration(tomorrow.difference(baghdadNow)),
      'fajr'
    );
  }

  static String _duration(Duration diff) =>
      '${diff.inHours.toString().padLeft(2, '0')}:${(diff.inMinutes % 60).toString().padLeft(2, '0')}:${(diff.inSeconds % 60).toString().padLeft(2, '0')}';
}

class _PrayerChip extends StatelessWidget {
  const _PrayerChip(
      {required this.label, required this.time, required this.active});
  final String label;
  final String time;
  final bool active;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 3, vertical: 6),
      decoration: BoxDecoration(
        color: active
            ? theme.colorScheme.primary.withOpacity(.17)
            : theme.colorScheme.surface.withOpacity(.36),
        borderRadius: BorderRadius.circular(13),
        border: Border.all(
            color: active
                ? theme.colorScheme.primary.withOpacity(.36)
                : Colors.white.withOpacity(.12)),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(label,
              maxLines: 1,
              softWrap: false,
              overflow: TextOverflow.clip,
              style: TextStyle(
                  fontSize: 10,
                  color: theme.colorScheme.onSurface.withOpacity(.78))),
          const SizedBox(height: 1),
          FittedBox(
            fit: BoxFit.scaleDown,
            child: Text(time,
                style: TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.w900,
                    color: active
                        ? theme.colorScheme.primary
                        : theme.colorScheme.onSurface)),
          ),
        ],
      ),
    );
  }
}

class _PrayerEmptyState extends StatelessWidget {
  const _PrayerEmptyState(
      {required this.onOpen, required this.onLocationTap, required this.text});
  final VoidCallback onOpen;
  final VoidCallback onLocationTap;
  final String text;

  @override
  Widget build(BuildContext context) => Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(children: [
            Icon(Icons.access_time_rounded),
            SizedBox(width: 8),
            Text('مواقيت الصلاة', style: TextStyle(fontWeight: FontWeight.w900))
          ]),
          const SizedBox(height: 7),
          Text(text),
          const SizedBox(height: 8),
          Wrap(spacing: 8, children: [
            TextButton(
                onPressed: onLocationTap, child: const Text('اختيار الموقع')),
            TextButton(onPressed: onOpen, child: const Text('تفاصيل المواقيت')),
          ]),
        ],
      );
}

class _DailyEventCard extends StatelessWidget {
  const _DailyEventCard({required this.hijri, required this.onTap});
  final ApproximateHijriDate hijri;
  final VoidCallback onTap;

  String? get _event {
    final key = '${hijri.month}-${hijri.day}';
    const events = <String, String>{
      '1-2': 'وصول الإمام الحسين (عليه السلام) وأهل بيته إلى كربلاء',
      '1-7': 'ذكرى منع الماء عن مخيم الإمام الحسين (عليه السلام)',
      '1-9':
          'يوم تاسوعاء — ذكرى نصرة أبي الفضل العباس (عليه السلام) وأصحاب الحسين',
      '1-10':
          'يوم عاشوراء — ذكرى استشهاد الإمام الحسين وأهل بيته وأصحابه (عليهم السلام)',
      '2-20':
          'زيارة الأربعين — أربعون يوماً على استشهاد الإمام الحسين (عليه السلام)',
      '2-28':
          'ذكرى وفاة النبي محمد (صلى الله عليه وآله) واستشهاد الإمام الحسن المجتبى (عليه السلام)',
      '2-29': 'ذكرى استشهاد الإمام علي بن موسى الرضا (عليه السلام)',
      '3-8': 'ذكرى استشهاد الإمام الحسن العسكري (عليه السلام)',
      '3-9': 'عيد الزهراء — مناسبة معروفة في بعض التقاليد الشيعية',
      '3-17':
          'ولادة النبي محمد (صلى الله عليه وآله) والإمام جعفر الصادق (عليه السلام)',
      '5-5': 'ولادة السيدة زينب بنت أمير المؤمنين (عليهما السلام)',
      '5-13':
          'استشهاد السيدة فاطمة الزهراء (عليها السلام) — رواية الخمسة والسبعين يوماً',
      '6-3':
          'استشهاد السيدة فاطمة الزهراء (عليها السلام) — رواية الخمسة والتسعين يوماً',
      '6-20': 'ولادة السيدة فاطمة الزهراء (عليها السلام)',
      '7-13': 'ولادة أمير المؤمنين الإمام علي بن أبي طالب (عليه السلام)',
      '8-3': 'ولادة الإمام الحسين (عليه السلام)',
      '8-15': 'ولادة الإمام المهدي (عجل الله فرجه)',
      '9-15': 'ولادة الإمام الحسن المجتبى (عليه السلام)',
      '9-19': 'ذكرى جرح أمير المؤمنين الإمام علي (عليه السلام)',
      '9-21': 'استشهاد أمير المؤمنين الإمام علي (عليه السلام)',
      '12-18': 'عيد الغدير الأغر — إعلان ولاية أمير المؤمنين (عليه السلام)',
      '12-24': 'يوم المباهلة',
    };
    return events[key];
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final event = _event;
    return ClipRRect(
      borderRadius: BorderRadius.circular(20),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 12, sigmaY: 12),
        child: Material(
          color: theme.brightness == Brightness.dark
              ? const Color(0xC91A211D)
              : const Color(0xDFFFFFFF),
          child: InkWell(
            onTap: onTap,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 15, vertical: 12),
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(20),
                border: Border.all(color: Colors.white.withOpacity(.19)),
              ),
              child: Row(
                children: [
                  Icon(Icons.event_note_rounded,
                      size: 25, color: theme.colorScheme.primary),
                  const SizedBox(width: 11),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                            event == null
                                ? 'المناسبات والذكريات'
                                : 'مناسبة اليوم · ${hijri.label}',
                            style: theme.textTheme.titleSmall?.copyWith(
                                fontWeight: FontWeight.w900,
                                color: theme.colorScheme.primary)),
                        const SizedBox(height: 3),
                        Text(
                          event ??
                              'تابع مناسبات أهل البيت (عليهم السلام) والتواريخ الواردة في المصادر.',
                          style:
                              theme.textTheme.bodySmall?.copyWith(height: 1.5),
                        ),
                        const SizedBox(height: 3),
                        Text('التاريخ الهجري تقريبي وقد يختلف مع ثبوت الهلال.',
                            style: theme.textTheme.bodySmall?.copyWith(
                                fontSize: 10,
                                color: theme.colorScheme.onSurface
                                    .withOpacity(.62))),
                      ],
                    ),
                  ),
                  const SizedBox(width: 4),
                  Icon(Icons.chevron_left_rounded,
                      color: theme.colorScheme.onSurface.withOpacity(.55)),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _HomeSectionShortcut {
  const _HomeSectionShortcut({
    required this.title,
    required this.icon,
    required this.route,
  });

  final String title;
  final IconData icon;
  final String route;
}

const _homeShortcuts = <_HomeSectionShortcut>[
  _HomeSectionShortcut(
      title: 'القرآن الكريم', icon: Icons.menu_book_rounded, route: '/quran'),
  _HomeSectionShortcut(
      title: 'مواقيت الصلاة',
      icon: Icons.access_time_rounded,
      route: '/prayer'),
  _HomeSectionShortcut(
      title: 'القبلة', icon: Icons.explore_rounded, route: '/qibla'),
  _HomeSectionShortcut(
      title: 'المسبحة',
      icon: Icons.radio_button_checked_rounded,
      route: '/tasbih'),
  _HomeSectionShortcut(
      title: 'المكتبة', icon: Icons.local_library_rounded, route: '/library'),
  _HomeSectionShortcut(
      title: 'الأدعية', icon: Icons.volunteer_activism_rounded, route: '/duas'),
  _HomeSectionShortcut(
      title: 'موسوعة الزيارات',
      icon: Icons.account_balance_rounded,
      route: '/visits'),
  _HomeSectionShortcut(
      title: 'زيارات وأدعية أهل البيت',
      icon: Icons.groups_rounded,
      route: '/ahl-al-bayt'),
  _HomeSectionShortcut(
      title: 'الروايات والأحاديث',
      icon: Icons.auto_stories_rounded,
      route: '/hadith'),
  _HomeSectionShortcut(
      title: 'المناسبات', icon: Icons.calendar_month_rounded, route: '/events'),
  _HomeSectionShortcut(
      title: 'الصوتيات والمجالس',
      icon: Icons.headphones_rounded,
      route: '/audio'),
  _HomeSectionShortcut(
      title: 'الأخلاق والتعلم', icon: Icons.school_rounded, route: '/ethics'),
  _HomeSectionShortcut(
      title: 'المفضلة', icon: Icons.favorite_rounded, route: '/favorites'),
  _HomeSectionShortcut(
      title: 'الإذاعة', icon: Icons.radio_rounded, route: '/radio'),
  _HomeSectionShortcut(
      title: 'المصادر', icon: Icons.source_outlined, route: '/sources'),
];

class _HijriDatePill extends StatelessWidget {
  const _HijriDatePill({required this.hijri, this.sourceHijriDate});

  final ApproximateHijriDate hijri;
  final String? sourceHijriDate;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return ClipRRect(
      borderRadius: BorderRadius.circular(16),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 11, sigmaY: 11),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 9),
          decoration: BoxDecoration(
            color: theme.brightness == Brightness.dark
                ? const Color(0xB91A211D)
                : const Color(0xBFFFFFFF),
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: Colors.white.withOpacity(.24)),
          ),
          child: Row(
            children: [
              Icon(Icons.calendar_month_rounded,
                  size: 20, color: theme.colorScheme.primary),
              const SizedBox(width: 9),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      sourceHijriDate == null
                          ? 'التاريخ الهجري · تقريبي'
                          : 'التاريخ الهجري · بحسب المصدر',
                      style: theme.textTheme.labelSmall,
                    ),
                    Text(
                      sourceHijriDate ?? hijri.monthYearLabel,
                      style: theme.textTheme.titleSmall
                          ?.copyWith(fontWeight: FontWeight.w900),
                    ),
                  ],
                ),
              ),
              if (sourceHijriDate == null)
                Text('اليوم ${hijri.day}',
                    style: theme.textTheme.bodySmall
                        ?.copyWith(fontWeight: FontWeight.w800)),
            ],
          ),
        ),
      ),
    );
  }
}

class _HomeSectionsStrip extends StatelessWidget {
  const _HomeSectionsStrip({required this.sections});

  final List<_HomeSectionShortcut> sections;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return SizedBox(
      height: 84,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        physics: const BouncingScrollPhysics(),
        itemCount: sections.length,
        separatorBuilder: (_, __) => const SizedBox(width: 8),
        itemBuilder: (context, index) {
          final section = sections[index];
          return SizedBox(
            width: 98,
            child: ClipRRect(
              borderRadius: BorderRadius.circular(16),
              child: BackdropFilter(
                filter: ImageFilter.blur(sigmaX: 10, sigmaY: 10),
                child: Material(
                  color: theme.brightness == Brightness.dark
                      ? const Color(0x9E1A211D)
                      : const Color(0xAFFFFFFF),
                  child: InkWell(
                    onTap: () => context.push(section.route),
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 6, vertical: 6),
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(16),
                        border:
                            Border.all(color: Colors.white.withOpacity(.24)),
                      ),
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(section.icon,
                              size: 20, color: theme.colorScheme.primary),
                          const SizedBox(height: 4),
                          Text(
                            section.title,
                            maxLines: 2,
                            textAlign: TextAlign.center,
                            overflow: TextOverflow.ellipsis,
                            style: theme.textTheme.labelSmall?.copyWith(
                                fontWeight: FontWeight.w800, fontSize: 10.5),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}
