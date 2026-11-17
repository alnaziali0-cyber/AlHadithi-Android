import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';

const _catalogAssetPath = 'assets/data/visits/catalog.json';

class ZiyaraVolume {
  const ZiyaraVolume({
    required this.id,
    required this.volumeLabel,
    required this.title,
    required this.subtitle,
    required this.outlineAssetPath,
    required this.textAssetPath,
    required this.pages,
  });

  final String id;
  final String volumeLabel;
  final String title;
  final String subtitle;
  final String outlineAssetPath;
  final String textAssetPath;
  final int pages;

  String get textFileName => textAssetPath.split('/').last;

  factory ZiyaraVolume.fromJson(Map<String, dynamic> json) {
    final id = json['id'];
    final pages = json['source_pages'];
    final volumeLabel = json['volume_label'];
    final title = json['title'];
    final subtitle = json['subtitle'];
    final outlineAssetPath = json['outline_asset_path'];
    final textAssetPath = json['text_asset_path'];

    if (id is! String || !RegExp(r'^volume_[0-9]+$').hasMatch(id)) {
      throw const FormatException('Invalid ziyara volume identifier.');
    }
    if (pages is! int || pages < 1) {
      throw const FormatException('Invalid ziyara source page count.');
    }
    if (volumeLabel is! String ||
        title is! String ||
        subtitle is! String ||
        outlineAssetPath is! String ||
        textAssetPath is! String ||
        volumeLabel.trim().isEmpty ||
        title.trim().isEmpty ||
        !RegExp(r'^assets/data/visits/outlines/outline-volume_[0-9]+\.md$')
            .hasMatch(outlineAssetPath) ||
        !RegExp(r'^assets/data/visits/transcribed/volume_[0-9]+\.txt$')
            .hasMatch(textAssetPath)) {
      throw const FormatException('Invalid ziyara volume metadata.');
    }

    return ZiyaraVolume(
      id: id,
      volumeLabel: volumeLabel,
      title: title,
      subtitle: subtitle,
      outlineAssetPath: outlineAssetPath,
      textAssetPath: textAssetPath,
      pages: pages,
    );
  }
}

Future<List<ZiyaraVolume>> loadZiyaraVolumes() async {
  final decoded = jsonDecode(await rootBundle.loadString(_catalogAssetPath));
  if (decoded is! Map<String, dynamic> || decoded['documents'] is! List) {
    throw const FormatException('Invalid ziyara catalog.');
  }

  final documents = decoded['documents'] as List;
  final volumes = documents.map((document) {
    if (document is! Map<String, dynamic>) {
      throw const FormatException('Invalid ziyara catalog entry.');
    }
    return ZiyaraVolume.fromJson(document);
  }).toList(growable: false);
  final identifiers = volumes.map((volume) => volume.id).toSet();
  if (identifiers.length != volumes.length || volumes.isEmpty) {
    throw const FormatException('Duplicate or empty ziyara catalog.');
  }
  return volumes;
}

class ZiyaraTextPage {
  const ZiyaraTextPage({
    required this.title,
    required this.sectionPath,
    required this.text,
  });

  final String title;
  final List<String> sectionPath;
  final String text;
}

class ZiyaraTextSection {
  const ZiyaraTextSection({required this.title, required this.pageIndices});

  final String title;
  final List<int> pageIndices;
}

class ZiyaraTextDocument {
  const ZiyaraTextDocument({required this.pages});

  final List<ZiyaraTextPage> pages;

  List<ZiyaraTextSection> get sections {
    final pageIndicesByTitle = <String, List<int>>{};
    for (var index = 0; index < pages.length; index++) {
      final path = pages[index].sectionPath;
      final title = path.isEmpty ? 'نص الملف' : path.join(' / ');
      pageIndicesByTitle.putIfAbsent(title, () => <int>[]).add(index);
    }
    return pageIndicesByTitle.entries
        .map((entry) => ZiyaraTextSection(
              title: entry.key,
              pageIndices: List<int>.unmodifiable(entry.value),
            ))
        .toList(growable: false);
  }
}

class _SectionHeading {
  const _SectionHeading(this.level, this.title);

  final int level;
  final String title;
}

/// Parses the Markdown page and section markers without changing source assets.
/// OCR wording is kept as text; only Markdown heading markers become UI labels.
ZiyaraTextDocument parseZiyaraTextDocument(String source) {
  final headingPattern = RegExp(r'^(#{1,6})\s+(.+?)\s*$');
  final pagePattern = RegExp(r'^(?:صفحة PDF|الصفحة المصوّرة)\s+\d+$');
  final lines = source.split('\n');
  final headings = <_SectionHeading>[];
  final pages = <ZiyaraTextPage>[];
  final preface = StringBuffer();
  var pageText = StringBuffer();
  String? pageTitle;
  List<String> pageSectionPath = const [];
  var sawDocumentTitle = false;

  void appendText(String line) {
    if (pageTitle == null) {
      preface.writeln(line);
    } else {
      pageText.writeln(line);
    }
  }

  void finishPage() {
    if (pageTitle == null) return;
    final value = pageText.toString();
    pages.add(ZiyaraTextPage(
      title: pageTitle!,
      sectionPath: pageSectionPath.isEmpty
          ? const ['نص الملف']
          : List<String>.unmodifiable(pageSectionPath),
      text: value.endsWith('\n') ? value.substring(0, value.length - 1) : value,
    ));
    pageTitle = null;
    pageText = StringBuffer();
  }

  for (final line in lines) {
    final headingMatch = headingPattern.firstMatch(line);
    if (headingMatch != null) {
      final level = headingMatch.group(1)!.length;
      final title = headingMatch.group(2)!.trim();
      if (pagePattern.hasMatch(title)) {
        finishPage();
        pageTitle = title;
        pageSectionPath = headings.map((heading) => heading.title).toList();
        continue;
      }

      // The first level-one heading is the file title, not a section selector.
      if (level == 1 && !sawDocumentTitle && pages.isEmpty && pageTitle == null) {
        sawDocumentTitle = true;
      } else {
        while (headings.isNotEmpty && headings.last.level >= level) {
          headings.removeLast();
        }
        headings.add(_SectionHeading(level, title));
      }
      appendText(title);
      continue;
    }
    appendText(line);
  }
  finishPage();

  final prefaceText = preface.toString();
  if (prefaceText.trim().isNotEmpty) {
    pages.insert(
      0,
      ZiyaraTextPage(
        title: 'معلومات الملف',
        sectionPath: const ['ملاحظات الملف'],
        text: prefaceText.endsWith('\n')
            ? prefaceText.substring(0, prefaceText.length - 1)
            : prefaceText,
      ),
    );
  }
  if (pages.isEmpty) {
    pages.add(const ZiyaraTextPage(
      title: 'النص الكامل',
      sectionPath: ['نص الملف'],
      text: '',
    ));
  }
  return ZiyaraTextDocument(pages: List<ZiyaraTextPage>.unmodifiable(pages));
}

Future<ZiyaraTextDocument> loadZiyaraTextDocument(ZiyaraVolume volume) async =>
    parseZiyaraTextDocument(await rootBundle.loadString(volume.textAssetPath));

class ZiyaraLibraryScreen extends StatefulWidget {
  const ZiyaraLibraryScreen({super.key, this.initialSearch});

  final String? initialSearch;

  @override
  State<ZiyaraLibraryScreen> createState() => _ZiyaraLibraryScreenState();
}

class _ZiyaraLibraryScreenState extends State<ZiyaraLibraryScreen> {
  late final Future<List<ZiyaraVolume>> _volumesFuture;
  late final TextEditingController _searchController;

  @override
  void initState() {
    super.initState();
    _volumesFuture = loadZiyaraVolumes();
    _searchController = TextEditingController(text: widget.initialSearch ?? '');
    _searchController.addListener(_onSearchChanged);
  }

  void _onSearchChanged() => setState(() {});

  @override
  void dispose() {
    _searchController
      ..removeListener(_onSearchChanged)
      ..dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return FutureBuilder<List<ZiyaraVolume>>(
      future: _volumesFuture,
      builder: (context, snapshot) {
        if (snapshot.hasError) {
          return const _CatalogMessage(
            icon: Icons.error_outline_rounded,
            message: 'تعذر تحميل فهرس موسوعة الزيارات.',
          );
        }
        if (!snapshot.hasData) {
          return const Center(child: CircularProgressIndicator());
        }

        final volumes = snapshot.data!;
        final query = _searchController.text.trim().toLowerCase();
        final filtered = volumes.where((volume) {
          if (query.isEmpty) return true;
          return '${volume.volumeLabel} ${volume.title} ${volume.subtitle} '
                  '${volume.textFileName}'
              .toLowerCase()
              .contains(query);
        }).toList(growable: false);
        final totalPages =
            volumes.fold<int>(0, (sum, volume) => sum + volume.pages);

        return CustomScrollView(
          slivers: [
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(18, 18, 18, 28),
              sliver: SliverList(
                delegate: SliverChildListDelegate([
                  Text(
                    'موسوعة زيارات المعصومين',
                    style: theme.textTheme.headlineSmall
                        ?.copyWith(fontWeight: FontWeight.w900),
                  ),
                  const SizedBox(height: 6),
                  const Text(
                    'النصوص متاحة كتابةً داخل التطبيق، مرتبة بحسب أقسامها وصفحاتها. '
                    'كل ملف مسودة OCR آلية غير مدققة حرفياً بالكامل.',
                    style: TextStyle(height: 1.55),
                  ),
                  const SizedBox(height: 14),
                  _OcrNotice(compact: false),
                  const SizedBox(height: 12),
                  Container(
                    padding: const EdgeInsets.all(14),
                    decoration: BoxDecoration(
                      color: theme.colorScheme.primary.withOpacity(.07),
                      borderRadius: BorderRadius.circular(18),
                      border: Border.all(
                        color: theme.colorScheme.primary.withOpacity(.14),
                      ),
                    ),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Icon(
                          Icons.menu_book_rounded,
                          color: theme.colorScheme.primary,
                        ),
                        const SizedBox(width: 10),
                        const Expanded(
                          child: Text(
                            'المصدر المنسوب: مؤسسة الإمام الهادي. الملفات المتاحة: '
                            '0 و1 و2 و4 و5 و6؛ المجلد 3 غير موجود ضمن الملفات المستلمة. '
                            'لا يفتح هذا القسم ملفات PDF أو روابط خارجية.',
                            style: TextStyle(height: 1.55),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: _searchController,
                    textInputAction: TextInputAction.search,
                    decoration: InputDecoration(
                      prefixIcon: const Icon(Icons.search_rounded),
                      hintText: 'ابحثي في أسماء المجلدات والملفات',
                      suffixIcon: query.isEmpty
                          ? null
                          : IconButton(
                              tooltip: 'مسح البحث',
                              onPressed: _searchController.clear,
                              icon: const Icon(Icons.close_rounded),
                            ),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(16),
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),
                  Text(
                    'الملفات النصية المتاحة: ${volumes.length} مسودات OCR · '
                    'صفحات المصدر: $totalPages',
                    style: theme.textTheme.bodySmall
                        ?.copyWith(fontWeight: FontWeight.w700),
                  ),
                  const SizedBox(height: 8),
                  if (filtered.isEmpty)
                    const _CatalogMessage(
                      icon: Icons.search_off_rounded,
                      message: 'لا توجد مجلدات تطابق عبارة البحث.',
                    )
                  else
                    ...filtered.map((volume) => _VolumeCard(volume: volume)),
                ]),
              ),
            ),
          ],
        );
      },
    );
  }
}

class _VolumeCard extends StatelessWidget {
  const _VolumeCard({required this.volume});

  final ZiyaraVolume volume;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      child: ListTile(
        contentPadding: const EdgeInsetsDirectional.fromSTEB(14, 8, 14, 8),
        leading: CircleAvatar(
          backgroundColor: theme.colorScheme.secondary.withOpacity(.14),
          child: Icon(
            Icons.menu_book_rounded,
            color: theme.colorScheme.primary,
          ),
        ),
        title: Text(
          volume.title,
          style: const TextStyle(fontWeight: FontWeight.w900),
        ),
        subtitle: Padding(
          padding: const EdgeInsets.only(top: 5),
          child: Text(
            '${volume.volumeLabel} · ${volume.pages} صفحة في الأصل\n'
            '${volume.subtitle}\nملف النص: ${volume.textFileName} · مسودة OCR',
            style: theme.textTheme.bodySmall?.copyWith(height: 1.45),
          ),
        ),
        isThreeLine: true,
        trailing: const Icon(Icons.chevron_left_rounded),
        onTap: () => context.push('/visits/${volume.id}'),
      ),
    );
  }
}

class ZiyaraVolumeTextScreen extends StatefulWidget {
  const ZiyaraVolumeTextScreen({super.key, required this.volumeId});

  final String volumeId;

  @override
  State<ZiyaraVolumeTextScreen> createState() =>
      _ZiyaraVolumeTextScreenState();
}

class _ZiyaraVolumeTextScreenState extends State<ZiyaraVolumeTextScreen> {
  late final Future<List<ZiyaraVolume>> _volumesFuture;

  @override
  void initState() {
    super.initState();
    _volumesFuture = loadZiyaraVolumes();
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<List<ZiyaraVolume>>(
      future: _volumesFuture,
      builder: (context, snapshot) {
        if (snapshot.hasError) {
          return const _CatalogMessage(
            icon: Icons.error_outline_rounded,
            message: 'تعذر تحميل بيانات هذا المجلد.',
          );
        }
        if (!snapshot.hasData) {
          return const Center(child: CircularProgressIndicator());
        }

        ZiyaraVolume? volume;
        for (final candidate in snapshot.data!) {
          if (candidate.id == widget.volumeId) {
            volume = candidate;
            break;
          }
        }
        if (volume == null) {
          return const _CatalogMessage(
            icon: Icons.find_in_page_outlined,
            message: 'المجلد المطلوب غير موجود في الفهرس.',
          );
        }

        final selected = volume;
        final theme = Theme.of(context);
        return DefaultTabController(
          length: 2,
          child: Column(
            children: [
              Padding(
                padding: const EdgeInsetsDirectional.fromSTEB(4, 2, 12, 4),
                child: Row(
                  children: [
                    IconButton(
                      tooltip: 'العودة إلى فهرس الزيارات',
                      onPressed: () => context.go('/visits'),
                      icon: const Icon(Icons.arrow_back_rounded),
                    ),
                    const SizedBox(width: 4),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            selected.title,
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              fontWeight: FontWeight.w900,
                              fontSize: 14,
                            ),
                          ),
                          Text(
                            '${selected.volumeLabel} · ${selected.textFileName}',
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: theme.textTheme.bodySmall,
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              TabBar(
                tabs: const [
                  Tab(text: 'النص المكتوب'),
                  Tab(text: 'فهرس المجلد'),
                ],
              ),
              Expanded(
                child: TabBarView(
                  children: [
                    _TranscriptionReader(volume: selected),
                    _OutlineReader(volume: selected),
                  ],
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}

class _TranscriptionReader extends StatefulWidget {
  const _TranscriptionReader({required this.volume});

  final ZiyaraVolume volume;

  @override
  State<_TranscriptionReader> createState() => _TranscriptionReaderState();
}

class _TranscriptionReaderState extends State<_TranscriptionReader> {
  late final Future<ZiyaraTextDocument> _documentFuture;
  int _selectedSectionIndex = 0;
  int _selectedPageIndex = 0;

  @override
  void initState() {
    super.initState();
    _documentFuture = loadZiyaraTextDocument(widget.volume);
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<ZiyaraTextDocument>(
      future: _documentFuture,
      builder: (context, snapshot) {
        if (snapshot.hasError) {
          return const _CatalogMessage(
            icon: Icons.error_outline_rounded,
            message: 'تعذر تحميل ملف النص المفرغ.',
          );
        }
        if (!snapshot.hasData) {
          return const Center(child: CircularProgressIndicator());
        }

        final document = snapshot.data!;
        final sections = document.sections;
        final sectionIndex = _selectedSectionIndex.clamp(0, sections.length - 1).toInt();
        final section = sections[sectionIndex];
        final pageIndex = section.pageIndices.contains(_selectedPageIndex)
            ? _selectedPageIndex
            : section.pageIndices.first;
        final page = document.pages[pageIndex];

        return Column(
          children: [
            Padding(
              padding: const EdgeInsetsDirectional.fromSTEB(14, 8, 14, 4),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const _OcrNotice(compact: true),
                  const SizedBox(height: 8),
                  Text(
                    'اسم الملف: ${widget.volume.textFileName} · المصدر المنسوب: '
                    'مؤسسة الإمام الهادي',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                  const SizedBox(height: 8),
                  DropdownButton<int>(
                    value: sectionIndex,
                    isExpanded: true,
                    underline: const SizedBox.shrink(),
                    items: [
                      for (var index = 0; index < sections.length; index++)
                        DropdownMenuItem<int>(
                          value: index,
                          child: Text(
                            sections[index].title,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                    ],
                    onChanged: (value) {
                      if (value == null) return;
                      setState(() {
                        _selectedSectionIndex = value;
                        _selectedPageIndex = sections[value].pageIndices.first;
                      });
                    },
                  ),
                  Row(
                    children: [
                      IconButton(
                        tooltip: 'الصفحة السابقة',
                        onPressed: section.pageIndices.indexOf(pageIndex) > 0
                            ? () => setState(() {
                                  final position =
                                      section.pageIndices.indexOf(pageIndex);
                                  _selectedPageIndex =
                                      section.pageIndices[position - 1];
                                })
                            : null,
                        icon: const Icon(Icons.chevron_right_rounded),
                      ),
                      Expanded(
                        child: DropdownButton<int>(
                          value: pageIndex,
                          isExpanded: true,
                          underline: const SizedBox.shrink(),
                          items: [
                            for (final index in section.pageIndices)
                              DropdownMenuItem<int>(
                                value: index,
                                child: Text(
                                  document.pages[index].title,
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                          ],
                          onChanged: (value) {
                            if (value != null) {
                              setState(() => _selectedPageIndex = value);
                            }
                          },
                        ),
                      ),
                      IconButton(
                        tooltip: 'الصفحة التالية',
                        onPressed: section.pageIndices.indexOf(pageIndex) <
                                section.pageIndices.length - 1
                            ? () => setState(() {
                                  final position =
                                      section.pageIndices.indexOf(pageIndex);
                                  _selectedPageIndex =
                                      section.pageIndices[position + 1];
                                })
                            : null,
                        icon: const Icon(Icons.chevron_left_rounded),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            Expanded(
              child: ListView(
                key: ValueKey('${widget.volume.id}-$pageIndex'),
                padding: const EdgeInsetsDirectional.fromSTEB(16, 8, 16, 28),
                children: [
                  Text(
                    page.sectionPath.join(' / '),
                    style: Theme.of(context).textTheme.titleSmall?.copyWith(
                          fontWeight: FontWeight.w800,
                          color: Theme.of(context).colorScheme.primary,
                        ),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    page.title,
                    style: Theme.of(context)
                        .textTheme
                        .titleMedium
                        ?.copyWith(fontWeight: FontWeight.w900),
                  ),
                  const SizedBox(height: 12),
                  SelectableText(
                    page.text.isEmpty ? 'لا يوجد نص في هذه الصفحة.' : page.text,
                    textAlign: TextAlign.right,
                    textDirection: TextDirection.rtl,
                    style: const TextStyle(height: 1.75),
                  ),
                ],
              ),
            ),
          ],
        );
      },
    );
  }
}

class _OutlineReader extends StatelessWidget {
  const _OutlineReader({required this.volume});

  final ZiyaraVolume volume;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return FutureBuilder<String>(
      future: rootBundle.loadString(volume.outlineAssetPath),
      builder: (context, snapshot) {
        if (snapshot.hasError) {
          return const _CatalogMessage(
            icon: Icons.error_outline_rounded,
            message: 'تعذر تحميل فهرس هذا المجلد.',
          );
        }
        if (!snapshot.hasData) {
          return const Center(child: CircularProgressIndicator());
        }

        return Column(
          children: [
            Padding(
              padding: const EdgeInsetsDirectional.fromSTEB(16, 8, 16, 8),
              child: Text(
                'هذا فهرس عناوين مستقل من داخل التطبيق. النص المكتوب متاح في '
                'تبويب «النص المكتوب» بوصفه مسودة OCR غير مدققة حرفياً بالكامل.',
                style: theme.textTheme.bodySmall?.copyWith(height: 1.5),
              ),
            ),
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsetsDirectional.fromSTEB(18, 8, 18, 28),
                child: _OutlineDocument(markdown: snapshot.data!),
              ),
            ),
          ],
        );
      },
    );
  }
}

class _OcrNotice extends StatelessWidget {
  const _OcrNotice({required this.compact});

  final bool compact;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      padding: EdgeInsets.all(compact ? 10 : 14),
      decoration: BoxDecoration(
        color: theme.colorScheme.tertiary.withOpacity(.10),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: theme.colorScheme.tertiary.withOpacity(.25)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.warning_amber_rounded, color: theme.colorScheme.tertiary),
          const SizedBox(width: 9),
          const Expanded(
            child: Text(
              'تنبيه: هذه مسودة OCR آلية غير مدققة حرفياً بالكامل. قد تحتوي '
              'أخطاء أو كلمات ساقطة، ولا تُقدَّم كنص موثّق أو دقيق بالكامل. '
              'يلزم تدقيق بشري قبل الاعتماد.',
              style: TextStyle(height: 1.5),
            ),
          ),
        ],
      ),
    );
  }
}

class _OutlineDocument extends StatelessWidget {
  const _OutlineDocument({required this.markdown});

  final String markdown;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final children = <Widget>[];
    for (final rawLine in markdown.split('\n')) {
      final line = rawLine.trim();
      if (line.isEmpty) {
        children.add(const SizedBox(height: 6));
        continue;
      }

      final heading = RegExp(r'^(#{1,6})\s+(.*)$').firstMatch(line);
      if (heading != null) {
        final level = heading.group(1)!.length;
        children.add(
          Padding(
            padding: EdgeInsets.only(top: level <= 2 ? 16 : 10, bottom: 4),
            child: Text(
              _plainMarkdown(heading.group(2)!.trim()),
              style: (level <= 2
                      ? theme.textTheme.titleMedium
                      : theme.textTheme.titleSmall)
                  ?.copyWith(fontWeight: FontWeight.w900),
            ),
          ),
        );
        continue;
      }

      final note = line.startsWith('>');
      final bullet = RegExp(r'^[-*]\s+(.*)$').firstMatch(line);
      final sourceText =
          bullet?.group(1) ?? (note ? line.substring(1).trim() : line);
      final text = _plainMarkdown(sourceText);
      children.add(
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 2),
          child: note
              ? Text(
                  text,
                  style: theme.textTheme.bodySmall?.copyWith(
                    color: theme.colorScheme.onSurfaceVariant,
                    height: 1.55,
                  ),
                )
              : bullet != null
                  ? Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text('•  '),
                        Expanded(
                          child: Text(text, style: const TextStyle(height: 1.5)),
                        ),
                      ],
                    )
                  : Text(text, style: const TextStyle(height: 1.5)),
        ),
      );
    }

    return Directionality(
      textDirection: TextDirection.rtl,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: children,
      ),
    );
  }
}

String _plainMarkdown(String value) => value
    .replaceAll(RegExp(r'\*\*(.*?)\*\*'), r'$1')
    .replaceAll(RegExp(r'\*(.*?)\*'), r'$1')
    .replaceAll('`', '');

class _CatalogMessage extends StatelessWidget {
  const _CatalogMessage({required this.icon, required this.message});

  final IconData icon;
  final String message;

  @override
  Widget build(BuildContext context) => Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(icon, size: 32),
              const SizedBox(height: 10),
              Text(message, textAlign: TextAlign.center),
            ],
          ),
        ),
      );
}
