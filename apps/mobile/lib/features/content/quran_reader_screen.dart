import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/models.dart';
import '../../core/providers.dart';

class QuranReaderScreen extends ConsumerWidget {
  const QuranReaderScreen({super.key, required this.surahNumber});

  final int surahNumber;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final ayahPage = ref.watch(quranSurahAyahsProvider(surahNumber));
    final favorites = ref.watch(favoriteEntriesProvider);

    return Scaffold(
      appBar: AppBar(
        title: ayahPage.maybeWhen(
          data: (page) => Text(page.surah.nameAr),
          orElse: () => const Text('المصحف الشريف'),
        ),
        actions: [
          IconButton(
            tooltip: 'إعادة تحميل السورة',
            onPressed: () => ref.invalidate(quranSurahAyahsProvider(surahNumber)),
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: SafeArea(
        child: ayahPage.when(
          loading: () => const Center(child: CircularProgressIndicator()),
          error: (error, _) => _ReaderMessage(
            icon: Icons.lock_outline,
            message: 'تعذر عرض السورة. لا يعرض التطبيق نص المصحف إلا بعد اكتمال الاستيراد واجتياز التحقق والمراجعة.\n$error',
            action: FilledButton.icon(
              onPressed: () => ref.invalidate(quranSurahAyahsProvider(surahNumber)),
              icon: const Icon(Icons.refresh),
              label: const Text('إعادة المحاولة'),
            ),
          ),
          data: (page) {
            if (page.items.length != page.total || page.items.isEmpty) {
              return const _ReaderMessage(
                icon: Icons.warning_amber_outlined,
                message: 'بيانات السورة غير مكتملة. تم إيقاف عرض النص حتى مراجعة مصدر البيانات.',
              );
            }
            return ListView.builder(
              padding: const EdgeInsets.fromLTRB(14, 12, 14, 28),
              itemCount: page.items.length + 1,
              itemBuilder: (context, index) {
                if (index == 0) {
                  return Card(
                    margin: const EdgeInsets.only(bottom: 14),
                    child: Padding(
                      padding: const EdgeInsets.all(14),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          Text(
                            '${page.surah.nameAr} · ${page.total} آية',
                            style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800),
                            textAlign: TextAlign.center,
                          ),
                          const SizedBox(height: 6),
                          Text(
                            'المصدر: ${page.source} · الإصدار: ${page.sourceVersion}',
                            style: Theme.of(context).textTheme.bodySmall,
                            textAlign: TextAlign.center,
                          ),
                          const SizedBox(height: 4),
                          Text(
                            'بصمة النص: ${page.textSha256}',
                            style: Theme.of(context).textTheme.labelSmall,
                            textDirection: TextDirection.ltr,
                            textAlign: TextAlign.center,
                          ),
                        ],
                      ),
                    ),
                  );
                }

                final ayah = page.items[index - 1];
                final favoriteId = '${ayah.surahNumber}:${ayah.ayahNumber}';
                final isFavorite = favorites.maybeWhen(
                  data: (items) => items.any(
                    (entry) => entry.entityType == 'quran_ayah' && entry.entityId == favoriteId,
                  ),
                  orElse: () => false,
                );
                return Card(
                  margin: const EdgeInsets.only(bottom: 10),
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(16, 10, 16, 16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Row(
                          children: [
                            Container(
                              constraints: const BoxConstraints(minWidth: 34, minHeight: 34),
                              alignment: Alignment.center,
                              decoration: BoxDecoration(
                                shape: BoxShape.circle,
                                color: Theme.of(context).colorScheme.secondaryContainer,
                              ),
                              child: Text('${ayah.ayahNumber}', style: const TextStyle(fontWeight: FontWeight.w800)),
                            ),
                            const Spacer(),
                            if (ayah.sajdah)
                              const Padding(
                                padding: EdgeInsetsDirectional.only(end: 8),
                                child: Chip(label: Text('سجدة')),
                              ),
                            IconButton(
                              tooltip: isFavorite ? 'إزالة علامة الحفظ' : 'حفظ الآية',
                              onPressed: () => _toggleBookmark(context, ref, ayah, isFavorite),
                              icon: Icon(
                                isFavorite ? Icons.bookmark : Icons.bookmark_border,
                                color: isFavorite ? Theme.of(context).colorScheme.primary : null,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 8),
                        SelectableText(
                          ayah.textUthmani,
                          textAlign: TextAlign.center,
                          textDirection: TextDirection.rtl,
                          style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                            fontFamily: 'AmiriQuran',
                            fontSize: 29,
                            height: 2.35,
                            fontWeight: FontWeight.normal,
                          ),
                        ),
                        const SizedBox(height: 10),
                        Text(
                          [
                            if (ayah.juzNumber != null) 'الجزء ${ayah.juzNumber}',
                            if (ayah.hizbNumber != null) 'الحزب ${ayah.hizbNumber}',
                            if (ayah.pageNumber != null) 'الصفحة ${ayah.pageNumber}',
                          ].join(' · '),
                          style: Theme.of(context).textTheme.labelSmall,
                          textAlign: TextAlign.center,
                        ),
                      ],
                    ),
                  ),
                );
              },
            );
          },
        ),
      ),
    );
  }

  Future<void> _toggleBookmark(
    BuildContext context,
    WidgetRef ref,
    QuranAyahEntry ayah,
    bool isFavorite,
  ) async {
    final entityId = '${ayah.surahNumber}:${ayah.ayahNumber}';
    try {
      final libraryApi = ref.read(deviceLibraryApiProvider);
      if (isFavorite) {
        await libraryApi.removeFavorite('quran_ayah', entityId);
      } else {
        await libraryApi.addFavorite('quran_ayah', entityId);
      }
      ref.invalidate(favoriteEntriesProvider);
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(isFavorite ? 'أُزيلت علامة الحفظ.' : 'حُفظت الآية في المفضلة.')),
        );
      }
    } catch (_) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('تعذر حفظ الآية. تحقق من الاتصال وحاول مجدداً.')),
        );
      }
    }
  }
}

class _ReaderMessage extends StatelessWidget {
  const _ReaderMessage({required this.icon, required this.message, this.action});

  final IconData icon;
  final String message;
  final Widget? action;

  @override
  Widget build(BuildContext context) => Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(icon, size: 42),
              const SizedBox(height: 14),
              Text(message, textAlign: TextAlign.center),
              if (action != null) ...[
                const SizedBox(height: 14),
                action!,
              ],
            ],
          ),
        ),
      );
}
