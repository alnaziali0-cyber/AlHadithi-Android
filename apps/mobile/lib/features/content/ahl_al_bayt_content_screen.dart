import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

class AhlAlBaytContentScreen extends StatelessWidget {
  const AhlAlBaytContentScreen({super.key});

  static String _catalogSearchRoute(String path, String term) =>
      '$path?search=${Uri.encodeQueryComponent(term)}';

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return CustomScrollView(
      slivers: [
        SliverPadding(
          padding: const EdgeInsets.fromLTRB(18, 18, 18, 28),
          sliver: SliverList(
            delegate: SliverChildListDelegate([
              Text(
                'زيارات وأدعية أهل البيت',
                style: theme.textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900),
              ),
              const SizedBox(height: 6),
              const Text('تظهر هنا النصوص المحلية التي اكتملت مراجعة مصدرها وحقوق استخدامها.'),
              const SizedBox(height: 16),
              Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: theme.colorScheme.primary.withOpacity(.07),
                  borderRadius: BorderRadius.circular(18),
                  border: Border.all(color: theme.colorScheme.primary.withOpacity(.14)),
                ),
                child: const Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Icon(Icons.verified_outlined),
                    SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        'لا نفتح روابط خارجية من هذه الصفحة. لا يُضاف نص إلى المكتبة قبل تحديد نسخته ومرجعه، وإثبات حق إعادة نشر النسخة الرقمية، وإكمال المراجعة.',
                        style: TextStyle(height: 1.6),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
              Row(
                children: [
                  Expanded(
                    child: _CategoryCard(
                      icon: Icons.account_balance_rounded,
                      title: 'الزيارات',
                      subtitle: 'نصوص محلية موثقة عند توفرها',
                      onTap: () => context.push('/visits'),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: _CategoryCard(
                      icon: Icons.volunteer_activism_rounded,
                      title: 'الأدعية',
                      subtitle: 'نصوص محلية موثقة عند توفرها',
                      onTap: () => context.push('/duas'),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 22),
              Text('موسوعة زيارات المعصومين',
                  style: theme.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900)),
              const SizedBox(height: 10),
              Card(
                child: ListTile(
                  contentPadding: const EdgeInsets.all(14),
                  leading: const Icon(Icons.menu_book_rounded, size: 30),
                  title: const Text('موسوعة زيارات المعصومين',
                      style: TextStyle(fontWeight: FontWeight.w900)),
                  subtitle: const Padding(
                    padding: EdgeInsets.only(top: 6),
                    child: Text(
                      'فهرس المجلدات المتاحة · النصوص قيد التفريغ والمراجعة · 2,468 صفحة في الأصل',
                      style: TextStyle(height: 1.5),
                    ),
                  ),
                  trailing: const Icon(Icons.chevron_left_rounded),
                  onTap: () => context.push('/visits'),
                ),
              ),
              const SizedBox(height: 22),
              Text('أدعية غير متوفرة محلياً',
                  style: theme.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900)),
              const SizedBox(height: 10),
              _SourceTextCard(
                title: 'دعاء الصباح',
                subtitle: 'منسوب إلى أمير المؤمنين (عليه السلام)',
                catalogRoute: _catalogSearchRoute('/duas', 'دعاء الصباح'),
                textStatus:
                    'لا يتوفر نص محلي محدد الطبعة مع إذن موثق لإعادة نشره.',
              ),
              const SizedBox(height: 10),
              _SourceTextCard(
                title: 'دعاء كميل بن زياد',
                subtitle: 'دعاء منسوب إلى كميل بن زياد',
                catalogRoute: _catalogSearchRoute('/duas', 'دعاء كميل'),
                textStatus:
                    'لا توجد نسخة محلية مختارة ومراجعة؛ لذلك لم يُضف نص أو تسجيل صوتي.',
              ),
              const SizedBox(height: 18),
              OutlinedButton.icon(
                onPressed: () => context.push('/sources'),
                icon: const Icon(Icons.source_outlined),
                label: const Text('مصادر المحتوى وضوابط التوثيق'),
              ),
            ]),
          ),
        ),
      ],
    );
  }
}

class _CategoryCard extends StatelessWidget {
  const _CategoryCard(
      {required this.icon,
      required this.title,
      required this.subtitle,
      required this.onTap});

  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Material(
      color: theme.colorScheme.surface,
      borderRadius: BorderRadius.circular(18),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(18),
        child: Container(
          constraints: const BoxConstraints(minHeight: 142),
          padding: const EdgeInsets.all(13),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(18),
            border: Border.all(color: theme.dividerColor.withOpacity(.7)),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Icon(icon, color: theme.colorScheme.primary, size: 24),
              const SizedBox(height: 10),
              Text(title, style: const TextStyle(fontWeight: FontWeight.w900)),
              const SizedBox(height: 4),
              Text(subtitle,
                  style: theme.textTheme.bodySmall?.copyWith(height: 1.35)),
            ],
          ),
        ),
      ),
    );
  }
}

class _SourceTextCard extends StatelessWidget {
  const _SourceTextCard({
    required this.title,
    required this.subtitle,
    required this.catalogRoute,
    required this.textStatus,
  });

  final String title;
  final String subtitle;
  final String catalogRoute;
  final String textStatus;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(title,
                style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 16)),
            const SizedBox(height: 5),
            Text(subtitle,
                style: theme.textTheme.bodySmall?.copyWith(height: 1.45)),
            const SizedBox(height: 8),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
              decoration: BoxDecoration(
                color: theme.colorScheme.surfaceContainerHighest.withOpacity(.55),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Icon(Icons.info_outline, size: 18,
                      color: theme.colorScheme.secondary),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      'غير متوفر حالياً داخل التطبيق · $textStatus',
                      style: theme.textTheme.bodySmall?.copyWith(height: 1.5),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 10),
            Align(
              alignment: AlignmentDirectional.centerStart,
              child: TextButton.icon(
                onPressed: () => context.push(catalogRoute),
                icon: const Icon(Icons.menu_book_outlined, size: 18),
                label: const Text('البحث داخل التطبيق'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
