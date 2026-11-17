import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers.dart';
import '../../core/wallpapers.dart';

class WallpapersScreen extends ConsumerStatefulWidget {
  const WallpapersScreen({super.key});

  @override
  ConsumerState<WallpapersScreen> createState() => _WallpapersScreenState();
}

class _WallpapersScreenState extends ConsumerState<WallpapersScreen> {
  late Future<List<WallpaperEntry>> _wallpapers;
  final Set<String> _savedIds = <String>{};

  @override
  void initState() {
    super.initState();
    _wallpapers = _loadWallpapers();
  }

  Future<List<WallpaperEntry>> _loadWallpapers() async {
    final items = await ref.read(wallpaperRepositoryProvider).listPublished();
    final saved = <String>{};
    final storage = ref.read(wallpaperStorageServiceProvider);
    for (final item in items) {
      if (await storage.isSaved(item)) saved.add(item.id);
    }
    if (mounted) {
      setState(() {
        _savedIds
          ..clear()
          ..addAll(saved);
      });
    }
    return items;
  }

  Future<void> _refresh() async {
    final future = _loadWallpapers();
    setState(() => _wallpapers = future);
    await future;
  }

  Future<File?> _save(WallpaperEntry wallpaper) async {
    try {
      final file = await ref.read(wallpaperStorageServiceProvider).save(wallpaper);
      if (mounted) {
        setState(() => _savedIds.add(wallpaper.id));
        ScaffoldMessenger.of(context)
          ..hideCurrentSnackBar()
          ..showSnackBar(const SnackBar(content: Text('حُفظت الصورة محلياً على جهازك.')));
      }
      return file;
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context)
          ..hideCurrentSnackBar()
          ..showSnackBar(const SnackBar(content: Text('تعذر حفظ الصورة على هذا الجهاز.')));
      }
      return null;
    }
  }

  void _preview(WallpaperEntry wallpaper) {
    showDialog<void>(
      context: context,
      builder: (dialogContext) => Dialog(
        insetPadding: const EdgeInsets.all(18),
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Align(
                alignment: AlignmentDirectional.centerStart,
                child: IconButton(
                  tooltip: 'إغلاق المعاينة',
                  onPressed: () => Navigator.of(dialogContext).pop(),
                  icon: const Icon(Icons.close_rounded),
                ),
              ),
              ConstrainedBox(
                constraints: BoxConstraints(
                  maxHeight: MediaQuery.sizeOf(context).height * .55,
                ),
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(14),
                  child: Image.asset(
                    wallpaper.assetPath,
                    fit: BoxFit.contain,
                    semanticLabel: wallpaper.title,
                    errorBuilder: (_, __, ___) => const Center(
                      child: Icon(Icons.broken_image_outlined, size: 48),
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 12),
              Text(wallpaper.title, textAlign: TextAlign.center,
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800)),
              const SizedBox(height: 4),
              Text('تصوير ${wallpaper.creator} · ${wallpaper.license}',
                  textAlign: TextAlign.center,
                  style: Theme.of(context).textTheme.bodySmall),
              const SizedBox(height: 12),
              FilledButton.icon(
                onPressed: () async {
                  final saved = await _save(wallpaper);
                  if (saved != null && dialogContext.mounted) {
                    Navigator.of(dialogContext).pop();
                  }
                },
                icon: Icon(_savedIds.contains(wallpaper.id)
                    ? Icons.download_done_rounded
                    : Icons.download_rounded),
                label: Text(_savedIds.contains(wallpaper.id)
                    ? 'حُفظت على الجهاز'
                    : 'حفظ الصورة على الجهاز'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return FutureBuilder<List<WallpaperEntry>>(
      future: _wallpapers,
      builder: (context, snapshot) {
        if (snapshot.connectionState != ConnectionState.done && !snapshot.hasData) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snapshot.hasError) {
          return Center(
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Column(mainAxisSize: MainAxisSize.min, children: [
                const Icon(Icons.image_not_supported_outlined, size: 42),
                const SizedBox(height: 10),
                const Text('تعذر فتح سجل الخلفيات المحلي.'),
                const SizedBox(height: 10),
                FilledButton(onPressed: () => setState(() {
                  _wallpapers = _loadWallpapers();
                }), child: const Text('إعادة المحاولة')),
              ]),
            ),
          );
        }
        final wallpapers = snapshot.data ?? const <WallpaperEntry>[];
        return RefreshIndicator(
          onRefresh: _refresh,
          child: CustomScrollView(slivers: [
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(18, 18, 18, 8),
              sliver: SliverToBoxAdapter(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text('الخلفيات', style: theme.textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900)),
                  const SizedBox(height: 5),
                  const Text('صور للمراقد المقدسة، محفوظة داخل التطبيق وتعمل دون اتصال.'),
                  const SizedBox(height: 12),
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: theme.colorScheme.primary.withOpacity(.07),
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: const Row(children: [
                      Icon(Icons.offline_pin_rounded),
                      SizedBox(width: 9),
                      Expanded(child: Text('المعاينة والحفظ من ملفات التطبيق المحلية؛ لا يجري تنزيل الصور من الإنترنت.')),
                    ]),
                  ),
                  const SizedBox(height: 12),
                ]),
              ),
            ),
            if (wallpapers.isEmpty)
              const SliverFillRemaining(
                hasScrollBody: false,
                child: Center(child: Text('لا توجد خلفيات منشورة حالياً.')),
              )
            else
              SliverPadding(
                padding: const EdgeInsets.fromLTRB(14, 0, 14, 28),
                sliver: SliverGrid.builder(
                  itemCount: wallpapers.length,
                  gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: MediaQuery.sizeOf(context).width > 700 ? 3 : 2,
                    crossAxisSpacing: 12,
                    mainAxisSpacing: 12,
                    childAspectRatio: .66,
                  ),
                  itemBuilder: (context, index) => _WallpaperCard(
                    wallpaper: wallpapers[index],
                    saved: _savedIds.contains(wallpapers[index].id),
                    onPreview: () => _preview(wallpapers[index]),
                    onSave: () => _save(wallpapers[index]),
                  ),
                ),
              ),
          ]),
        );
      },
    );
  }
}

class _WallpaperCard extends StatelessWidget {
  const _WallpaperCard({
    required this.wallpaper,
    required this.saved,
    required this.onPreview,
    required this.onSave,
  });

  final WallpaperEntry wallpaper;
  final bool saved;
  final VoidCallback onPreview;
  final VoidCallback onSave;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      clipBehavior: Clip.antiAlias,
      margin: EdgeInsets.zero,
      child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Expanded(
          child: Semantics(
            button: true,
            label: 'معاينة ${wallpaper.title}',
            child: InkWell(
              onTap: onPreview,
              child: Stack(fit: StackFit.expand, children: [
                Image.asset(
                  wallpaper.assetPath,
                  fit: BoxFit.cover,
                  errorBuilder: (_, __, ___) => const Center(child: Icon(Icons.broken_image_outlined)),
                ),
                Positioned(
                  left: 8,
                  top: 8,
                  child: DecoratedBox(
                    decoration: BoxDecoration(color: Colors.black.withOpacity(.55), borderRadius: BorderRadius.circular(20)),
                    child: const Padding(
                      padding: EdgeInsets.all(6),
                      child: Icon(Icons.zoom_in_rounded, color: Colors.white, size: 18),
                    ),
                  ),
                ),
              ]),
            ),
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(10, 8, 10, 2),
          child: Text(wallpaper.title, maxLines: 2, overflow: TextOverflow.ellipsis,
              style: theme.textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.w800)),
        ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 10),
          child: Text('تصوير ${wallpaper.creator} · ${wallpaper.license}',
              maxLines: 1, overflow: TextOverflow.ellipsis,
              style: theme.textTheme.bodySmall?.copyWith(fontSize: 10)),
        ),
        TextButton.icon(
          onPressed: onSave,
          icon: Icon(saved ? Icons.download_done_rounded : Icons.download_rounded, size: 18),
          label: Text(saved ? 'محفوظة' : 'حفظ على الجهاز', maxLines: 1),
        ),
      ]),
    );
  }
}
