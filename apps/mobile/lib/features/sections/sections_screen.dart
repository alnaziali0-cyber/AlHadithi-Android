import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

class _AppSection {
  const _AppSection({required this.title, required this.subtitle, required this.icon, required this.route, this.featured = false});
  final String title;
  final String subtitle;
  final IconData icon;
  final String route;
  final bool featured;
}

class SectionsScreen extends StatelessWidget {
  const SectionsScreen({super.key});

  static const _sections = <_AppSection>[
    _AppSection(title: 'القرآن الكريم', subtitle: 'المصحف والقراءة والتلاوات', icon: Icons.menu_book_rounded, route: '/quran', featured: true),
    _AppSection(title: 'مواقيت الصلاة', subtitle: 'أوقات الصلاة وضبط التنبيهات', icon: Icons.access_time_rounded, route: '/prayer', featured: true),
    _AppSection(title: 'اتجاه القبلة', subtitle: 'تحديد اتجاه الكعبة بالبوصلة', icon: Icons.explore_rounded, route: '/qibla'),
    _AppSection(title: 'المسبحة الإلكترونية', subtitle: 'التسبيح المرتب والعدّ المفتوح', icon: Icons.radio_button_checked_rounded, route: '/tasbih'),
    _AppSection(title: 'المكتبة الإسلامية', subtitle: 'الكتب والعقائد والفقه والسيرة', icon: Icons.local_library_rounded, route: '/library'),
    _AppSection(title: 'زيارات وأدعية أهل البيت', subtitle: 'النصوص المكتوبة ومصادرها', icon: Icons.account_balance_rounded, route: '/ahl-al-bayt', featured: true),
    _AppSection(title: 'موسوعة زيارات المعصومين', subtitle: 'فهرس المجلدات؛ النصوص قيد التفريغ والمراجعة', icon: Icons.menu_book_rounded, route: '/visits', featured: true),
    _AppSection(title: 'الخلفيات', subtitle: 'صور المراقد المقدسة محفوظة محلياً', icon: Icons.wallpaper_rounded, route: '/wallpapers', featured: true),
    _AppSection(title: 'الروايات والأحاديث', subtitle: 'المصادر والمراجع والتخريج', icon: Icons.auto_stories_rounded, route: '/hadith'),
    _AppSection(title: 'المناسبات والتقويم', subtitle: 'الأشهر والمناسبات الهجرية', icon: Icons.calendar_month_rounded, route: '/events'),
    _AppSection(title: 'الصوتيات والمجالس', subtitle: 'مصادر التلاوات والمجالس', icon: Icons.headphones_rounded, route: '/audio', featured: true),
    _AppSection(title: 'الأخلاق والتعلم', subtitle: 'الدروس والسيرة والآداب', icon: Icons.volunteer_activism_outlined, route: '/ethics'),
    _AppSection(title: 'المفضلة', subtitle: 'كل ما حفظته في مكان واحد', icon: Icons.favorite_rounded, route: '/favorites'),
    _AppSection(title: 'إذاعة العتبة الحسينية', subtitle: 'صفحة الإذاعة والمصدر الرسمي', icon: Icons.radio_rounded, route: '/radio'),
    _AppSection(title: 'المصادر والتوثيق', subtitle: 'مصادر شيعية وشروط الاستخدام', icon: Icons.source_outlined, route: '/sources'),
  ];

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return CustomScrollView(slivers: [
      SliverPadding(padding: const EdgeInsets.fromLTRB(18, 18, 18, 28), sliver: SliverList(delegate: SliverChildListDelegate([
        Text('الأقسام', style: theme.textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900)),
        const SizedBox(height: 6),
        const Text('محتوى إسلامي شيعي مرتب بحسب موضوعه، مع إظهار مصادر المواد وتوثيقها.'),
        const SizedBox(height: 16),
        Container(padding: const EdgeInsets.all(14), decoration: BoxDecoration(color: theme.colorScheme.primary.withOpacity(.07), borderRadius: BorderRadius.circular(18), border: Border.all(color: theme.colorScheme.primary.withOpacity(.12))), child: const Row(children: [Icon(Icons.verified_outlined), SizedBox(width: 10), Expanded(child: Text('تُعرض النصوص المنقولة مع مراجعها المتاحة. لا يُعرض حكم على رواية من دون مرجع علمي محدد.', style: TextStyle(height: 1.5)))])),
        const SizedBox(height: 16),
        GridView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          itemCount: _sections.length,
          gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: MediaQuery.sizeOf(context).width > 700 ? 3 : 2,
            crossAxisSpacing: 12,
            mainAxisSpacing: 12,
            mainAxisExtent: 142,
          ),
          itemBuilder: (context, index) {
            final section = _sections[index];
            return _SectionButton(section: section);
          },
        ),
      ]))),
    ]);
  }
}

class _SectionButton extends StatelessWidget {
  const _SectionButton({required this.section});
  final _AppSection section;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Material(
      color: theme.colorScheme.surface.withOpacity(.91),
      borderRadius: BorderRadius.circular(20),
      child: InkWell(
        onTap: () => context.push(section.route),
        borderRadius: BorderRadius.circular(20),
        child: Container(
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(borderRadius: BorderRadius.circular(20), border: Border.all(color: section.featured ? theme.colorScheme.secondary.withOpacity(.58) : theme.dividerColor.withOpacity(.65))),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
            Row(children: [Container(width: 40, height: 40, decoration: BoxDecoration(color: theme.colorScheme.secondary.withOpacity(.14), borderRadius: BorderRadius.circular(13)), child: Icon(section.icon, color: theme.colorScheme.primary, size: 22)), const Spacer(), Icon(Icons.arrow_outward_rounded, size: 17, color: theme.colorScheme.onSurface.withOpacity(.38))]),
            Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(section.title, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 14)), const SizedBox(height: 4), Text(section.subtitle, maxLines: 2, overflow: TextOverflow.ellipsis, style: theme.textTheme.bodySmall?.copyWith(height: 1.3))]),
          ]),
        ),
      ),
    );
  }
}
