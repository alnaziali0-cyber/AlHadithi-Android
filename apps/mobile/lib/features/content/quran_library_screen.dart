import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/quran_text_repository.dart';

class QuranLibraryScreen extends ConsumerStatefulWidget {
  const QuranLibraryScreen({super.key});

  @override
  ConsumerState<QuranLibraryScreen> createState() => _QuranLibraryScreenState();
}

class _QuranLibraryScreenState extends ConsumerState<QuranLibraryScreen> {
  final _searchController = TextEditingController();
  String _query = '';

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final library = ref.watch(quranTextLibraryProvider);
    return library.when(
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (_, __) => Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.error_outline_rounded, size: 42),
              const SizedBox(height: 12),
              const Text(
                'تعذر التحقق من حزمة المصحف المحلية؛ أُوقف عرض النص حتى استعادة الملفات المطابقة.',
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 8),
            ],
          ),
        ),
      ),
      data: (data) => DefaultTabController(
        length: 4,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          'القرآن الكريم',
                          style: Theme.of(context)
                              .textTheme
                              .headlineSmall
                              ?.copyWith(fontWeight: FontWeight.w900),
                        ),
                      ),
                      IconButton(
                        tooltip: 'المصدر والحقوق',
                        onPressed: () => _showQuranSourceDetails(context),
                        icon: const Icon(Icons.info_outline_rounded),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  TextField(
                    controller: _searchController,
                    textDirection: TextDirection.rtl,
                    textInputAction: TextInputAction.search,
                    decoration: InputDecoration(
                      hintText: 'ابحث في الآيات',
                      prefixIcon: const Icon(Icons.search_rounded),
                      suffixIcon: _query.isEmpty
                          ? null
                          : IconButton(
                              tooltip: 'مسح البحث',
                              onPressed: () {
                                _searchController.clear();
                                setState(() => _query = '');
                              },
                              icon: const Icon(Icons.close_rounded),
                            ),
                    ),
                    onChanged: (value) => setState(() => _query = value),
                  ),
                  if (_query.trim().isEmpty) ...[
                    const SizedBox(height: 6),
                    const TabBar(
                      isScrollable: true,
                      tabs: [
                        Tab(text: 'السور'),
                        Tab(text: 'الأجزاء'),
                        Tab(text: 'الأحزاب'),
                        Tab(text: 'المحفوظات'),
                      ],
                    ),
                  ],
                ],
              ),
            ),
            Expanded(
              child: _query.trim().isNotEmpty
                  ? _SearchResults(library: data, query: _query)
                  : Column(
                      children: [
                        _ContinueReading(library: data),
                        Expanded(
                          child: TabBarView(
                            children: [
                              _SurahList(library: data),
                              _DivisionList(
                                  library: data,
                                  divisions: data.juzs,
                                  label: 'الجزء'),
                              _DivisionList(
                                  library: data,
                                  divisions: data.hizbs,
                                  label: 'الحزب'),
                              _BookmarksList(library: data),
                            ],
                          ),
                        ),
                      ],
                    ),
            ),
          ],
        ),
      ),
    );
  }
}

void _showQuranSourceDetails(BuildContext context) {
  showDialog<void>(
    context: context,
    builder: (dialogContext) => AlertDialog(
      title: const Text('المصدر والحقوق'),
      content: const SingleChildScrollView(
        child: Text(
          'Tanzil Quran Text · Copyright (C) 2007–2021 Tanzil Project · CC BY 3.0.\n\n'
          'المصدر: Tanzil Project. الرخصة: CC BY 3.0.\n'
          'إشعار الإسناد الكامل محفوظ مع بيانات المصحف المحلية.\n'
          'يجب إبقاء النص كما ورد دون تغيير وإرفاق إشعار الحقوق عند إعادة نشره.\n\n'
          'الخط القرآني Amiri Quran مرخّص وفق SIL Open Font License 1.1.',
          textDirection: TextDirection.rtl,
          style: TextStyle(height: 1.7),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(dialogContext),
          child: const Text('إغلاق'),
        ),
      ],
    ),
  );
}

class _ContinueReading extends ConsumerWidget {
  const _ContinueReading({required this.library});

  final QuranTextLibrary library;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final lastRead = ref.watch(quranLastReadProvider);
    return lastRead.maybeWhen(
      data: (position) {
        if (position == null) return const SizedBox.shrink();
        final surah = library.surahByNumber(position.surahNumber);
        if (surah == null || position.ayahNumber > surah.ayahCount)
          return const SizedBox.shrink();
        return Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 10),
          child: Card(
            color:
                Theme.of(context).colorScheme.primaryContainer.withOpacity(.45),
            child: ListTile(
              leading: const Icon(Icons.play_circle_outline_rounded),
              title: const Text('متابعة القراءة',
                  style: TextStyle(fontWeight: FontWeight.w800)),
              subtitle: Text('${surah.nameAr} · الآية ${position.ayahNumber}'),
              trailing: const Icon(Icons.chevron_left_rounded),
              onTap: () => _openPosition(
                  context, position.surahNumber, position.ayahNumber),
            ),
          ),
        );
      },
      orElse: () => const SizedBox.shrink(),
    );
  }
}

class _SurahList extends StatelessWidget {
  const _SurahList({required this.library});

  final QuranTextLibrary library;

  @override
  Widget build(BuildContext context) => ListView.separated(
        padding: const EdgeInsets.fromLTRB(16, 4, 16, 24),
        itemCount: library.surahs.length,
        separatorBuilder: (_, __) => const SizedBox(height: 6),
        itemBuilder: (context, index) {
          final surah = library.surahs[index];
          return Card(
            child: ListTile(
              leading: _NumberBadge(number: surah.number),
              title: Text(surah.nameAr,
                  style: const TextStyle(fontWeight: FontWeight.w800)),
              subtitle: Text('${surah.ayahCount} آية'),
              trailing: const Icon(Icons.chevron_left_rounded),
              onTap: () => _openPosition(context, surah.number, 1),
            ),
          );
        },
      );
}

class _DivisionList extends StatelessWidget {
  const _DivisionList(
      {required this.library, required this.divisions, required this.label});

  final QuranTextLibrary library;
  final List<QuranDivision> divisions;
  final String label;

  @override
  Widget build(BuildContext context) => ListView.separated(
        padding: const EdgeInsets.fromLTRB(16, 4, 16, 24),
        itemCount: divisions.length,
        separatorBuilder: (_, __) => const SizedBox(height: 6),
        itemBuilder: (context, index) {
          final division = divisions[index];
          final surah = library.surahByNumber(division.startSurah);
          return Card(
            child: ListTile(
              leading: _NumberBadge(number: division.number),
              title: Text('$label ${division.number}',
                  style: const TextStyle(fontWeight: FontWeight.w800)),
              subtitle:
                  Text('${surah?.nameAr ?? ''} · الآية ${division.startAyah}'),
              trailing: const Icon(Icons.chevron_left_rounded),
              onTap: () => _openPosition(
                  context, division.startSurah, division.startAyah),
            ),
          );
        },
      );
}

class _BookmarksList extends ConsumerWidget {
  const _BookmarksList({required this.library});

  final QuranTextLibrary library;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final bookmarks = ref.watch(quranBookmarksProvider);
    return bookmarks.when(
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (_, __) =>
          const Center(child: Text('تعذر تحميل العلامات المحفوظة.')),
      data: (items) {
        if (items.isEmpty) {
          return const Center(
              child: Text('لم تُحفظ علامات بعد. احفظ آية من شاشة القراءة.'));
        }
        return ListView.separated(
          padding: const EdgeInsets.fromLTRB(16, 4, 16, 24),
          itemCount: items.length,
          separatorBuilder: (_, __) => const SizedBox(height: 6),
          itemBuilder: (context, index) {
            final bookmark = items[index];
            final surah = library.surahByNumber(bookmark.surahNumber);
            if (surah == null || bookmark.ayahNumber > surah.ayahCount)
              return const SizedBox.shrink();
            final ayah =
                library.ayahsFor(bookmark.surahNumber)[bookmark.ayahNumber - 1];
            return Card(
              child: ListTile(
                leading: const Icon(Icons.bookmark_rounded),
                title: Text('${surah.nameAr} · الآية ${bookmark.ayahNumber}',
                    style: const TextStyle(fontWeight: FontWeight.w800)),
                subtitle: Text(ayah.text,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    textDirection: TextDirection.rtl),
                trailing: const Icon(Icons.chevron_left_rounded),
                onTap: () => _openPosition(
                    context, bookmark.surahNumber, bookmark.ayahNumber),
              ),
            );
          },
        );
      },
    );
  }
}

class _SearchResults extends StatelessWidget {
  const _SearchResults({required this.library, required this.query});

  final QuranTextLibrary library;
  final String query;

  @override
  Widget build(BuildContext context) {
    final matches = library.search(query, limit: 100);
    if (matches.isEmpty)
      return const Center(child: Text('لا توجد آيات مطابقة.'));
    return ListView.separated(
      padding: const EdgeInsets.fromLTRB(16, 4, 16, 24),
      itemCount: matches.length,
      separatorBuilder: (_, __) => const SizedBox(height: 6),
      itemBuilder: (context, index) {
        final result = matches[index];
        return Card(
          child: ListTile(
            leading: _NumberBadge(number: result.ayah.ayahNumber),
            title: Text(
              result.ayah.text,
              maxLines: 3,
              overflow: TextOverflow.ellipsis,
              textDirection: TextDirection.rtl,
              style: const TextStyle(
                  fontFamily: 'AmiriQuran', fontSize: 20, height: 1.8),
            ),
            subtitle: Text(
                '${result.surah.nameAr} · الآية ${result.ayah.ayahNumber}'),
            trailing: const Icon(Icons.chevron_left_rounded),
            onTap: () => _openPosition(
                context, result.surah.number, result.ayah.ayahNumber),
          ),
        );
      },
    );
  }
}

class _NumberBadge extends StatelessWidget {
  const _NumberBadge({required this.number});

  final int number;

  @override
  Widget build(BuildContext context) => CircleAvatar(
        radius: 19,
        backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
        child: Text('$number',
            style: const TextStyle(fontWeight: FontWeight.w800)),
      );
}

void _openPosition(BuildContext context, int surahNumber, int ayahNumber) {
  context.push('/quran/$surahNumber?ayah=$ayahNumber');
}
