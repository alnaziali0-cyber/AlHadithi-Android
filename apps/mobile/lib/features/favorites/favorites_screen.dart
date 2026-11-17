import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/models.dart';
import '../../core/providers.dart';

class FavoritesScreen extends ConsumerWidget {
  const FavoritesScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final favorites = ref.watch(favoriteEntriesProvider);
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('المفضلة')),
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: () async { await ref.refresh(favoriteEntriesProvider.future); },
          child: favorites.when(
            loading: () => const Center(child: CircularProgressIndicator()),
            error: (_, __) => ListView(
              padding: const EdgeInsets.all(24),
              children: const [
                Icon(Icons.cloud_off_outlined, size: 42),
                SizedBox(height: 12),
                Text(
                  'تعذر تحميل المفضلة. تحقق من الاتصال ثم اسحب الشاشة لإعادة المحاولة.',
                  textAlign: TextAlign.center,
                ),
              ],
            ),
            data: (items) {
              if (items.isEmpty) {
                return ListView(
                  padding: const EdgeInsets.all(24),
                  children: const [
                    Icon(Icons.favorite_border, size: 48),
                    SizedBox(height: 12),
                    Text('لا توجد مواد محفوظة في المفضلة بعد.', textAlign: TextAlign.center),
                    SizedBox(height: 6),
                    Text(
                      'يمكنك حفظ الكتب والروايات والأدعية والزيارات والمناسبات والدروس والصوتيات الموثقة من صفحاتها.',
                      textAlign: TextAlign.center,
                    ),
                  ],
                );
              }
              final isStale = items.any((item) => item.isStale);
              return ListView.separated(
                padding: const EdgeInsets.fromLTRB(14, 12, 14, 28),
                itemCount: items.length + (isStale ? 1 : 0),
                separatorBuilder: (_, __) => const SizedBox(height: 8),
                itemBuilder: (context, index) {
                  if (isStale && index == 0) {
                    return const Card(
                      child: Padding(
                        padding: EdgeInsets.all(12),
                        child: Row(
                          children: [
                            Icon(Icons.cloud_off_outlined),
                            SizedBox(width: 8),
                            Expanded(child: Text('لا يوجد اتصال؛ هذه آخر نسخة محفوظة من المفضلة. أعد الاتصال لتحديثها.')),
                          ],
                        ),
                      ),
                    );
                  }
                  final itemIndex = index - (isStale ? 1 : 0);
                  return _FavoriteTile(entry: items[itemIndex]);
                },
              );
            },
          ),
        ),
      ),
      floatingActionButton: FloatingActionButton.small(
        tooltip: 'تحديث المفضلة',
        onPressed: () => ref.invalidate(favoriteEntriesProvider),
        child: const Icon(Icons.refresh),
      ),
      backgroundColor: theme.colorScheme.surface,
    );
  }
}

class _FavoriteTile extends ConsumerWidget {
  const _FavoriteTile({required this.entry});
  final FavoriteEntry entry;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Card(
      child: ListTile(
        leading: Icon(
          entry.available ? _icon(entry.entityType) : Icons.inventory_2_outlined,
          color: entry.available ? Theme.of(context).colorScheme.secondary : Theme.of(context).disabledColor,
        ),
        title: Text(entry.title, maxLines: 2, overflow: TextOverflow.ellipsis),
        subtitle: Text(
          entry.available ? entry.subtitle : 'هذه المادة لم تعد متاحة للنشر. يمكنك إزالة سجلها من المفضلة.',
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
        ),
        trailing: IconButton(
          tooltip: 'إزالة من المفضلة',
          icon: const Icon(Icons.favorite, color: Colors.redAccent),
          onPressed: () async {
            try {
              await ref.read(deviceLibraryApiProvider).removeFavorite(entry.entityType, entry.entityId);
              ref.invalidate(favoriteEntriesProvider);
            } catch (_) {
              if (context.mounted) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('تعذر إزالة العنصر. حاول مرة أخرى عند توفر الاتصال.')),
                );
              }
            }
          },
        ),
        onTap: entry.available ? () => context.push(_route(entry.entityType, entry.entityId)) : null,
      ),
    );
  }

  static String _route(String type, String entityId) => switch (type) {
        'book' => '/library',
        'hadith' => '/hadith',
        'dua' => '/duas',
        'visit' => '/visits',
        'event' => '/events',
        'ethics_lesson' => '/ethics',
        'audio' => '/audio',
        'quran_ayah' => _quranRoute(entityId),
        _ => '/quran',
      };


  static String _quranRoute(String entityId) {
    final match = RegExp(r'^([1-9][0-9]{0,2}):([1-9][0-9]{0,2})$').firstMatch(entityId);
    if (match == null) return '/quran';
    return '/quran/${match.group(1)}';
  }

  static IconData _icon(String type) => switch (type) {
        'book' => Icons.menu_book_outlined,
        'hadith' => Icons.auto_stories_outlined,
        'dua' => Icons.volunteer_activism_outlined,
        'visit' => Icons.place_outlined,
        'event' => Icons.event_outlined,
        'ethics_lesson' => Icons.school_outlined,
        'audio' => Icons.headphones_outlined,
        'quran_ayah' => Icons.menu_book_rounded,
        _ => Icons.bookmark_outline,
      };
}
