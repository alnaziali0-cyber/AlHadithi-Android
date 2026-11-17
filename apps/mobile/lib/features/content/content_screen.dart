import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/providers.dart';
import '../../core/safe_display_text.dart';
import 'library_reference_panel.dart';

class QuranStatusScreen extends ConsumerWidget {
  const QuranStatusScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final status = ref.watch(quranStatusProvider);
    return _PageFrame(
      title: 'القرآن الكريم',
      child: status.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (_, __) => const _UnavailableCard(
            message: 'تعذر التحقق من حالة المصحف حالياً.'),
        data: (data) {
          final ready = data['status'] == 'ready';
          final message = safeDisplayText(data['message_ar']);
          final sourceLabel = safeDisplayText(data['source']);
          return Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(18),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Icon(ready ? Icons.verified : Icons.pending_outlined,
                          color: Theme.of(context).colorScheme.secondary, size: 32),
                      const SizedBox(height: 12),
                      Text(
                        ready ? 'المصحف متحقق منه' : 'المصحف قيد تجهيز البيانات',
                        style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        message.isEmpty
                            ? (ready
                                ? 'تم استيراد النص المرجعي.'
                                : 'لم يُستورد بعد نص قرآني موثّق ومراجع.')
                            : message,
                      ),
                      const SizedBox(height: 12),
                      Text('السور: ${data['surah_count'] ?? 0} / 114'),
                      Text('الآيات: ${data['ayah_count'] ?? 0} / 6236'),
                      if (sourceLabel.isNotEmpty)
                        Text('المصدر: $sourceLabel · ${safeDisplayText(data['source_version'])}'),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 12),
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text('مصدر النص والخط القرآني', style: TextStyle(fontWeight: FontWeight.w900)),
                      const SizedBox(height: 8),
                      const Text(
                        'نص المصحف: Tanzil، محفوظ حرفياً دون تعديل مع إشعار CC BY 3.0. الخط المضمّن: Amiri Quran، وفق SIL Open Font License 1.1. تفاصيل الإسناد محفوظة في إشعار الحقوق المرفق بالتطبيق.',
                        style: TextStyle(height: 1.65),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 12),
              if (ready) ...[
                const SizedBox(height: 18),
                const _QuranSurahList(),
              ] else ...[
                const _UnavailableCard(
                  message: 'لن يُعرض نص قرآني تجريبي ولن يُولّد نص الآيات آلياً. يجب استيراد نسخة موثوقة حرفياً، ثم اجتياز فحوص 114 سورة و6,236 آية والتحقق من الرخصة والمراجعة العلمية.',
                ),
                const SizedBox(height: 10),
                FilledButton.tonalIcon(
                  onPressed: () => context.push('/sources'),
                  icon: const Icon(Icons.source_outlined),
                  label: const Text('مصادر القرآن والكتب الشيعية'),
                ),
              ],
            ],
          );
        },
      ),
    );
  }
}

class _QuranSurahList extends ConsumerWidget {
  const _QuranSurahList();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final surahs = ref.watch(quranSurahsProvider);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          'فهرس السور',
          style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900),
        ),
        const SizedBox(height: 8),
        surahs.when(
          loading: () => const Padding(
            padding: EdgeInsets.all(24),
            child: Center(child: CircularProgressIndicator()),
          ),
          error: (_, __) => const _UnavailableCard(
            message: 'تعذر تحميل فهرس السور المتحقق منه. أعد المحاولة عند توفر الاتصال.',
          ),
          data: (items) => Column(
            children: items.map((surah) => Card(
              margin: const EdgeInsets.only(bottom: 8),
              child: ListTile(
                leading: CircleAvatar(child: Text('${surah.number}')),
                title: Text(surah.nameAr, style: const TextStyle(fontWeight: FontWeight.w800)),
                subtitle: Text('${surah.nameEn} · ${surah.ayahCount} آية'),
                trailing: const Icon(Icons.chevron_left),
                onTap: () => context.push('/quran/${surah.number}'),
              ),
            )).toList(growable: false),
          ),
        ),
      ],
    );
  }
}

class ContentStatusScreen extends ConsumerWidget {
  const ContentStatusScreen({
    super.key,
    required this.title,
    required this.description,
    required this.contentType,
    this.search,
  });

  final String title;
  final String description;
  final String contentType;
  final String? search;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final searchTerm = search?.trim() ?? '';
    final catalog = ref.watch(contentItemsProvider((contentType, searchTerm)));
    return _PageFrame(
      title: title,
      child: catalog.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (_, __) => Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            if (contentType == 'book') ...[
              const LibraryReferencePanel(),
              const SizedBox(height: 14),
            ],
            _UnavailableCard(
              message: 'تعذر الاتصال بخدمة المحتوى حالياً. $description',
            ),
          ],
        ),
        data: (result) {
          if (result.items.isEmpty) {
            return Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                if (contentType == 'book') ...[
                  const LibraryReferencePanel(),
                  const SizedBox(height: 14),
                ],
                const _UnavailableCard(
                  message: 'لا توجد حالياً مواد موثقة منشورة في هذا القسم. لن تظهر نصوص أو كتب أو روايات منسوبة دون إدخال المصدر ومراجعته.',
                ),
                if (searchTerm.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: Text('لم تظهر مادة منشورة تطابق: $searchTerm'),
                  ),
                const SizedBox(height: 12),
                Text(description, style: Theme.of(context).textTheme.bodySmall),
                const SizedBox(height: 10),
                FilledButton.tonalIcon(
                  onPressed: () => context.push('/sources'),
                  icon: const Icon(Icons.source_outlined),
                  label: const Text('تصفح المصادر الشيعية الموثقة'),
                ),
              ],
            );
          }
          return Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              if (contentType == 'book') ...[
                const LibraryReferencePanel(),
                const SizedBox(height: 14),
              ],
              if (result.isStale)
                const Padding(
                  padding: EdgeInsets.only(bottom: 10),
                  child: _OfflineBanner(message: 'تعذر الاتصال بالخادم؛ تُعرض آخر نسخة محفوظة على هذا الجهاز.'),
                ),
              Text(
                'عدد المواد: ${result.total}',
                style: Theme.of(context).textTheme.bodySmall,
              ),
              const SizedBox(height: 8),
              ...result.items.map((item) => _CatalogItemCard(
                    item: item,
                    contentType: contentType,
                  )),
            ],
          );
        },
      ),
    );
  }
}

class _CatalogItemCard extends ConsumerWidget {
  const _CatalogItemCard({required this.item, required this.contentType});

  final Map<String, dynamic> item;
  final String contentType;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final title = safeDisplayText(item['title']).isEmpty
        ? 'مادة دون عنوان'
        : safeDisplayText(item['title']);
    final body = safeDisplayText(item['body_markdown']);
    final entityId = item['id']?.toString();
    final favoriteEntries = ref.watch(favoriteEntriesProvider);
    final isFavorite = favoriteEntries.maybeWhen(
      data: (items) => items.any((entry) => entry.entityType == contentType && entry.entityId == entityId),
      orElse: () => false,
    );
    final metadata = safeDisplayText(item['metadata']);
    final grades = item['gradings'] is List ? item['gradings'] as List : const [];
    final canOpenBook = contentType == 'book' && item['id'] is num;

    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      child: ExpansionTile(
        leading: Icon(_iconFor(contentType), color: Theme.of(context).colorScheme.secondary),
        title: Text(title, style: const TextStyle(fontWeight: FontWeight.w800)),
        subtitle: metadata.isEmpty ? null : Text(metadata, maxLines: 2, overflow: TextOverflow.ellipsis),
        trailing: IconButton(
          tooltip: isFavorite ? 'إزالة من المفضلة' : 'إضافة إلى المفضلة',
          icon: Icon(isFavorite ? Icons.favorite : Icons.favorite_border, color: isFavorite ? Colors.redAccent : null),
          onPressed: entityId == null ? null : () async {
            try {
              final libraryApi = ref.read(deviceLibraryApiProvider);
              if (isFavorite) {
                await libraryApi.removeFavorite(contentType, entityId);
              } else {
                await libraryApi.addFavorite(contentType, entityId);
              }
              ref.invalidate(favoriteEntriesProvider);
              if (context.mounted) {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text(isFavorite ? 'أُزيلت المادة من المفضلة.' : 'أُضيفت المادة إلى المفضلة.')),
                );
              }
            } catch (_) {
              if (context.mounted) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('تعذر تحديث المفضلة. قد تكون المادة غير متاحة أو الاتصال منقطعاً.')),
                );
              }
            }
          },
        ),
        childrenPadding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
        expandedCrossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (body.isNotEmpty)
            SelectableText(body, style: const TextStyle(height: 1.8)),
          if (contentType == 'audio')
            const Padding(
              padding: EdgeInsets.only(top: 12),
              child: Text(
                'هذا الصوت غير متوفر كملف محلي داخل التطبيق؛ لا يوجد تشغيل أو رابط خارجي.',
                style: TextStyle(height: 1.6),
              ),
            ),
          if (item['chain_text'] is String && (item['chain_text'] as String).isNotEmpty) ...[
            const SizedBox(height: 12),
            const Text('السند', style: TextStyle(fontWeight: FontWeight.bold)),
            SelectableText(safeDisplayText(item['chain_text']), style: const TextStyle(height: 1.7)),
          ],
          if (item['source_reference'] is String && (item['source_reference'] as String).isNotEmpty) ...[
            const SizedBox(height: 12),
            Text('المصدر: ${safeDisplayText(item['source_reference'])}', style: Theme.of(context).textTheme.bodySmall),
          ],
          if (grades.isNotEmpty) ...[
            const SizedBox(height: 14),
            const Divider(),
            const Text('التقييمات المنقولة بصورة مستقلة', style: TextStyle(fontWeight: FontWeight.w800)),
            const SizedBox(height: 6),
            ...grades.whereType<Map>().map((grade) => Padding(
                  padding: const EdgeInsets.only(bottom: 10),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Text('${safeDisplayText(grade['grading'] ?? 'دون تصنيف')} — ${safeDisplayText(grade['scholar'] ?? 'غير محدد')}',
                          style: const TextStyle(fontWeight: FontWeight.w700)),
                      if (grade['methodology'] != null) Text('المنهجية: ${safeDisplayText(grade['methodology'])}'),
                      if (grade['citation'] != null) Text('الإحالة: ${safeDisplayText(grade['citation'])}'),
                    ],
                  ),
                )),
          ],
          if (item['historical_sources'] is List) ...[
            const SizedBox(height: 10),
            const Text('المصادر التاريخية', style: TextStyle(fontWeight: FontWeight.bold)),
            ...((item['historical_sources'] as List).map((source) => Text(safeDisplayText(source)))),
            if (item['date_certainty'] != null)
              Text('درجة الثبوت: ${safeDisplayText(item['date_certainty'])}'),
          ],
          if (canOpenBook) ...[
            const SizedBox(height: 12),
            Align(
              alignment: AlignmentDirectional.centerStart,
              child: FilledButton.icon(
                onPressed: () => context.push('/book/${(item['id'] as num).toInt()}',
                    extra: {'title': title, 'volumeCount': item['volume_count']}),
                icon: const Icon(Icons.menu_book_outlined),
                label: const Text('فتح صفحات الكتاب'),
              ),
            ),
          ],
        ],
      ),
    );
  }

  static IconData _iconFor(String type) => switch (type) {
        'book' => Icons.menu_book_outlined,
        'hadith' => Icons.auto_stories_outlined,
        'dua' => Icons.volunteer_activism_outlined,
        'visit' => Icons.place_outlined,
        'event' => Icons.event_outlined,
        'ethics_lesson' => Icons.school_outlined,
        'audio' => Icons.headphones_outlined,
        _ => Icons.article_outlined,
      };

}

class _PageFrame extends StatelessWidget {
  const _PageFrame({required this.title, required this.child});
  final String title;
  final Widget child;

  @override
  Widget build(BuildContext context) => CustomScrollView(
        slivers: [
          SliverPadding(
            padding: const EdgeInsets.all(18),
            sliver: SliverList(
              delegate: SliverChildListDelegate([
                Text(title, style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900)),
                const SizedBox(height: 16),
                child,
              ]),
            ),
          ),
        ],
      );
}

class _UnavailableCard extends StatelessWidget {
  const _UnavailableCard({required this.message});
  final String message;

  @override
  Widget build(BuildContext context) => Card(
        child: Padding(
          padding: const EdgeInsets.all(18),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Icon(Icons.info_outline),
              const SizedBox(width: 12),
              Expanded(child: Text(message, style: const TextStyle(height: 1.7))),
            ],
          ),
        ),
      );
}

class _OfflineBanner extends StatelessWidget {
  const _OfflineBanner({required this.message});
  final String message;

  @override
  Widget build(BuildContext context) => Container(
        decoration: BoxDecoration(
          color: Theme.of(context).colorScheme.tertiaryContainer,
          borderRadius: BorderRadius.circular(12),
        ),
        padding: const EdgeInsets.all(12),
        child: Row(
          children: [
            const Icon(Icons.cloud_off_outlined),
            const SizedBox(width: 8),
            Expanded(child: Text(message)),
          ],
        ),
      );
}
