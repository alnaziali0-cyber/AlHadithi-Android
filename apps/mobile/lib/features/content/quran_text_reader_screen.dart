import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:just_audio/just_audio.dart';
import 'package:scrollable_positioned_list/scrollable_positioned_list.dart';
import 'package:share_plus/share_plus.dart';

import '../../core/providers.dart';
import '../../core/quran_audio_service.dart';
import '../../core/quran_text_repository.dart';

class QuranTextReaderScreen extends ConsumerStatefulWidget {
  const QuranTextReaderScreen({
    super.key,
    required this.surahNumber,
    this.initialAyahNumber,
  });

  final int surahNumber;
  final int? initialAyahNumber;

  @override
  ConsumerState<QuranTextReaderScreen> createState() =>
      _QuranTextReaderScreenState();
}

class _QuranTextReaderScreenState extends ConsumerState<QuranTextReaderScreen> {
  final ItemScrollController _itemScrollController = ItemScrollController();
  final ItemPositionsListener _itemPositionsListener =
      ItemPositionsListener.create();
  late final AudioPlayer _player;
  late final QuranAudioService _audioService;
  StreamSubscription<PlayerState>? _playerStateSubscription;
  Timer? _lastReadTimer;
  double _fontSize = 30;
  bool _repeatAyah = false;
  bool _playingAyah = false;
  String? _audioLabel;
  int? _lastSavedAyah;

  @override
  void initState() {
    super.initState();
    final savedFontSize =
        ref.read(sharedPreferencesProvider).getDouble('quran_font_size');
    _fontSize = (savedFontSize ?? 30).clamp(22, 44).toDouble();
    _audioService = QuranAudioService();
    _player = AudioPlayer();
    _playerStateSubscription = _player.playerStateStream.listen((state) {
      if (!mounted) return;
      setState(() {
        if (state.processingState == ProcessingState.completed &&
            !(_playingAyah && _repeatAyah)) {
          _audioLabel = null;
          _playingAyah = false;
        }
      });
    });
    _itemPositionsListener.itemPositions.addListener(_onVisibleItemsChanged);
  }

  @override
  void dispose() {
    _lastReadTimer?.cancel();
    _itemPositionsListener.itemPositions.removeListener(_onVisibleItemsChanged);
    _playerStateSubscription?.cancel();
    _player.dispose();
    super.dispose();
  }

  void _onVisibleItemsChanged() {
    final positions = _itemPositionsListener.itemPositions.value
        .where((item) => item.index >= 0 && item.itemTrailingEdge > 0)
        .toList()
      ..sort((a, b) => a.itemLeadingEdge.compareTo(b.itemLeadingEdge));
    if (positions.isEmpty) return;
    final ayahNumber = positions.first.index + 1;
    final library = ref.read(quranTextLibraryProvider).maybeWhen(
          data: (value) => value,
          orElse: () => null,
        );
    final surah = library?.surahByNumber(widget.surahNumber);
    if (surah == null ||
        ayahNumber > surah.ayahCount ||
        ayahNumber == _lastSavedAyah) return;
    _lastSavedAyah = ayahNumber;
    _lastReadTimer?.cancel();
    _lastReadTimer = Timer(const Duration(milliseconds: 350), () async {
      await ref.read(quranLocalStoreProvider).saveLastRead(
            QuranPosition(
                surahNumber: widget.surahNumber, ayahNumber: ayahNumber),
          );
      if (mounted) ref.invalidate(quranLastReadProvider);
    });
  }

  Future<void> _saveFontSize(double size) async {
    setState(() => _fontSize = size);
    await ref
        .read(sharedPreferencesProvider)
        .setDouble('quran_font_size', size);
  }

  Future<void> _toggleTheme() async {
    final next = Theme.of(context).brightness == Brightness.dark
        ? ThemeMode.light
        : ThemeMode.dark;
    await ref.read(themeModeProvider.notifier).setMode(next);
  }

  Future<void> _playSurah(QuranTextSurah surah) async {
    try {
      final offline = await _audioService.existingSurahFile(surah.number);
      if (offline == null) {
        _showMessage('لا تتوفر تلاوة محلية محفوظة لهذه السورة على هذا الجهاز.');
        return;
      }
      await _player.setLoopMode(LoopMode.off);
      await _player.setAudioSource(AudioSource.file(offline.path));
      if (!mounted) return;
      setState(() {
        _audioLabel = '${surah.nameAr} · السورة كاملة';
        _playingAyah = false;
      });
      await _player.play();
    } catch (_) {
      _showMessage('تعذر تشغيل ملف التلاوة المحلي.');
    }
  }

  Future<void> _playAyah(QuranTextLibrary library, QuranTextAyah ayah) async {
    try {
      final globalNumber = _audioService.globalAyahNumber(
          library, ayah.surahNumber, ayah.ayahNumber);
      final offline = await _audioService.existingAyahFile(globalNumber);
      if (offline == null) {
        _showMessage('لا يتوفر ملف صوتي محلي لهذه الآية على هذا الجهاز.');
        return;
      }
      await _player.setLoopMode(_repeatAyah ? LoopMode.one : LoopMode.off);
      await _player.setAudioSource(AudioSource.file(offline.path));
      if (!mounted) return;
      final surah = library.surahByNumber(ayah.surahNumber);
      setState(() {
        _audioLabel = '${surah?.nameAr ?? ''} · الآية ${ayah.ayahNumber}';
        _playingAyah = true;
      });
      await _player.play();
    } catch (_) {
      _showMessage('تعذر تشغيل ملف الآية المحلي.');
    }
  }

  Future<void> _toggleRepeat() async {
    final next = !_repeatAyah;
    setState(() => _repeatAyah = next);
    if (_playingAyah) {
      await _player.setLoopMode(next ? LoopMode.one : LoopMode.off);
    }
  }

  Future<void> _toggleBookmark(QuranTextAyah ayah, bool isBookmarked) async {
    final added = await ref.read(quranLocalStoreProvider).toggleBookmark(
          QuranPosition(
              surahNumber: ayah.surahNumber, ayahNumber: ayah.ayahNumber),
        );
    ref.invalidate(quranBookmarksProvider);
    if (mounted)
      _showMessage(added
          ? 'أُضيفت الآية إلى العلامات المحفوظة.'
          : 'أُزيلت الآية من العلامات المحفوظة.');
  }

  String _exportText(QuranTextSurah surah, QuranTextAyah ayah) =>
      '${ayah.text}\n\nالمرجع: سورة ${surah.nameAr}، الآية ${ayah.ayahNumber}\n'
      'المصدر: Tanzil Project';

  Future<void> _copyAyah(QuranTextSurah surah, QuranTextAyah ayah) async {
    await Clipboard.setData(ClipboardData(text: _exportText(surah, ayah)));
    if (mounted) _showMessage('نُسخت الآية مع مرجعها ومصدرها.');
  }

  Future<void> _shareAyah(QuranTextSurah surah, QuranTextAyah ayah) async {
    await Share.share(
      _exportText(surah, ayah),
      subject: 'القرآن الكريم · ${surah.nameAr} ${ayah.ayahNumber}',
    );
  }

  void _showAyahActions({
    required QuranTextLibrary library,
    required QuranTextSurah surah,
    required QuranTextAyah ayah,
    required bool isBookmarked,
  }) {
    showModalBottomSheet<void>(
      context: context,
      useSafeArea: true,
      showDragHandle: true,
      builder: (sheetContext) => Padding(
        padding: const EdgeInsets.fromLTRB(20, 4, 20, 24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              'سورة ${surah.nameAr} · الآية ${_arabicIndic(ayah.ayahNumber)}',
              textAlign: TextAlign.center,
              style: Theme.of(context)
                  .textTheme
                  .titleMedium
                  ?.copyWith(fontWeight: FontWeight.w900),
            ),
            const SizedBox(height: 10),
            SelectableText(
              ayah.text,
              textAlign: TextAlign.center,
              textDirection: TextDirection.rtl,
              style: TextStyle(
                fontFamily: 'AmiriQuran',
                fontSize: _fontSize,
                height: 1.9,
              ),
            ),
            const SizedBox(height: 16),
            Wrap(
              alignment: WrapAlignment.center,
              spacing: 8,
              runSpacing: 8,
              children: [
                FilledButton.tonalIcon(
                  onPressed: () {
                    Navigator.pop(sheetContext);
                    unawaited(_playAyah(library, ayah));
                  },
                  icon: const Icon(Icons.play_arrow_rounded),
                  label: const Text('استماع'),
                ),
                FilledButton.tonalIcon(
                  onPressed: () {
                    Navigator.pop(sheetContext);
                    unawaited(_toggleRepeat());
                  },
                  icon: const Icon(Icons.repeat_one_rounded),
                  label: const Text('تكرار'),
                ),
                FilledButton.tonalIcon(
                  onPressed: () {
                    Navigator.pop(sheetContext);
                    unawaited(_toggleBookmark(ayah, isBookmarked));
                  },
                  icon: Icon(isBookmarked
                      ? Icons.bookmark_remove_outlined
                      : Icons.bookmark_add_outlined),
                  label: Text(isBookmarked ? 'إزالة العلامة' : 'حفظ الموضع'),
                ),
                OutlinedButton.icon(
                  onPressed: () {
                    Navigator.pop(sheetContext);
                    unawaited(_copyAyah(surah, ayah));
                  },
                  icon: const Icon(Icons.copy_rounded),
                  label: const Text('نسخ مع المصدر'),
                ),
                OutlinedButton.icon(
                  onPressed: () {
                    Navigator.pop(sheetContext);
                    unawaited(_shareAyah(surah, ayah));
                  },
                  icon: const Icon(Icons.share_outlined),
                  label: const Text('مشاركة'),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  void _showMessage(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
  }

  void _showSourceDetails() {
    showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('المصدر والحقوق'),
        content: const SingleChildScrollView(
          child: Text(
            'Tanzil Quran Text · Copyright (C) 2007–2021 Tanzil Project · CC BY 3.0. النص محفوظ كما ورد دون تغيير.\n\n'
            'بيانات المصدر لا تعني إجازة قراءة أو اعتماداً من مرجعية أو حوزة؛ يلزم تدقيق قارئ مختص قبل اعتماد النشر النهائي.\n\n'
            'الرخصة: CC BY 3.0. أُرفق إشعار الإسناد الكامل مع بيانات المصحف المحلية.\n'
            'يجب إبقاء النص كما ورد دون تغيير وإرفاق إشعار الحقوق عند إعادة نشره.\n'
            'الخط المحلي: Amiri Quran · SIL Open Font License.\n\n'
            'الصوت: لا تُضمّن ملفات صوتية في التطبيق ولا يُبث أو يُنزّل صوت من مصدر خارجي. يعمل التشغيل فقط مع ملف محلي محفوظ سابقاً على هذا الجهاز.',
            style: TextStyle(height: 1.55),
            textDirection: TextDirection.rtl,
          ),
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('إغلاق'))
        ],
      ),
    );
  }

  void _goBack() {
    if (GoRouter.of(context).canPop()) {
      context.pop();
    } else {
      context.go('/quran');
    }
  }

  @override
  Widget build(BuildContext context) {
    final libraryState = ref.watch(quranTextLibraryProvider);
    final bookmarksState = ref.watch(quranBookmarksProvider);
    final theme = Theme.of(context);
    return Scaffold(
      backgroundColor: theme.brightness == Brightness.light
          ? const Color(0xFFFCFBF8)
          : theme.colorScheme.surface,
      appBar: AppBar(
        title: const Text('القرآن الكريم'),
        leading: IconButton(
          tooltip: 'العودة إلى فهرس السور',
          onPressed: _goBack,
          icon: const Icon(Icons.arrow_back_rounded),
        ),
        actions: [
          PopupMenuButton<double>(
            tooltip: 'حجم خط الآيات',
            icon: const Icon(Icons.text_fields_rounded),
            initialValue: _fontSize,
            onSelected: _saveFontSize,
            itemBuilder: (context) => [
              for (final size in <double>[24, 28, 32, 36, 40])
                PopupMenuItem<double>(
                    value: size, child: Text('حجم الخط ${size.round()}')),
            ],
          ),
          IconButton(
            tooltip: theme.brightness == Brightness.dark
                ? 'الوضع النهاري'
                : 'الوضع الليلي',
            onPressed: _toggleTheme,
            icon: Icon(theme.brightness == Brightness.dark
                ? Icons.light_mode_rounded
                : Icons.dark_mode_rounded),
          ),
          IconButton(
            tooltip: 'المصدر والحقوق',
            onPressed: _showSourceDetails,
            icon: const Icon(Icons.info_outline_rounded),
          ),
        ],
      ),
      body: SafeArea(
        child: libraryState.when(
          loading: () => const Center(child: CircularProgressIndicator()),
          error: (_, __) => const _ReaderMessage(
              message: 'تعذر التحقق من ملفات المصحف المحلية؛ أُوقف عرض النص لحماية دقته.'),
          data: (library) {
            final surah = library.surahByNumber(widget.surahNumber);
            if (surah == null)
              return const _ReaderMessage(message: 'رقم السورة غير صالح.');
            final ayahs = library.ayahsFor(surah.number);
            if (ayahs.length != surah.ayahCount || ayahs.isEmpty) {
              return const _ReaderMessage(
                  message: 'بيانات السورة غير مكتملة؛ أُوقف عرض النص.');
            }
            final bookmarkIds = bookmarksState.maybeWhen(
              data: (items) => items.map((item) => item.key).toSet(),
              orElse: () => <String>{},
            );
            final nextSurah = library.surahByNumber(
                surah.number == 114 ? 1 : surah.number + 1);
            return Column(
              children: [
                Padding(
                  padding: const EdgeInsets.fromLTRB(18, 8, 18, 4),
                  child: _SurahBanner(surah: surah),
                ),
                Padding(
                  padding: const EdgeInsets.fromLTRB(18, 4, 18, 8),
                  child: OutlinedButton.icon(
                    onPressed: () => _playSurah(surah),
                    icon: const Icon(Icons.play_arrow_rounded),
                    label: const Text('استماع للملف المحلي'),
                  ),
                ),
                Expanded(
                  child: ScrollablePositionedList.builder(
                    itemScrollController: _itemScrollController,
                    itemPositionsListener: _itemPositionsListener,
                    initialScrollIndex: ((widget.initialAyahNumber ?? 1) - 1)
                        .clamp(0, ayahs.length - 1)
                        .toInt(),
                    initialAlignment: .08,
                    padding: const EdgeInsets.fromLTRB(22, 8, 22, 18),
                    itemCount: ayahs.length,
                    itemBuilder: (context, index) {
                      final ayah = ayahs[index];
                      final isBookmarked = bookmarkIds
                          .contains('${ayah.surahNumber}:${ayah.ayahNumber}');
                      final isInitial =
                          widget.initialAyahNumber == ayah.ayahNumber;
                      return _AyahLine(
                        ayah: ayah,
                        fontSize: _fontSize,
                        isBookmarked: isBookmarked,
                        isHighlighted: isInitial,
                        onOpenActions: () => _showAyahActions(
                          library: library,
                          surah: surah,
                          ayah: ayah,
                          isBookmarked: isBookmarked,
                        ),
                      );
                    },
                  ),
                ),
                if (_audioLabel != null)
                  _AudioDock(
                    label: _audioLabel!,
                    playing: _player.playing,
                    repeating: _repeatAyah,
                    showRepeat: _playingAyah,
                    onPlayPause: () =>
                        _player.playing ? _player.pause() : _player.play(),
                    onRepeat: _toggleRepeat,
                    onStop: () async {
                      await _player.stop();
                      if (mounted) setState(() => _audioLabel = null);
                    },
                  ),
                if (nextSurah != null)
                  _NextSurahFooter(
                    surah: nextSurah,
                    onTap: () => context.push('/quran/${nextSurah.number}'),
                  ),
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 3, 16, 7),
                  child: Text(
                    'تشغيل محلي فقط · إذا لم يتوفر ملف محفوظ فالتلاوة غير متاحة حالياً.',
                    textAlign: TextAlign.center,
                    style: theme.textTheme.labelSmall,
                  ),
                ),
              ],
            );
          },
        ),
      ),
    );
  }
}

class _SurahBanner extends StatelessWidget {
  const _SurahBanner({required this.surah});

  final QuranTextSurah surah;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final accent = theme.colorScheme.primary;
    return Container(
      constraints: const BoxConstraints(minHeight: 56),
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 7),
      decoration: BoxDecoration(
        color: theme.colorScheme.surface,
        borderRadius: BorderRadius.circular(15),
        border: Border.all(color: accent.withOpacity(.58), width: 1.2),
        boxShadow: [
          BoxShadow(
            color: accent.withOpacity(.06),
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Row(
        children: [
          Icon(Icons.auto_awesome_outlined, color: accent, size: 20),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  'سورة ${surah.nameAr}',
                  textAlign: TextAlign.center,
                  style: theme.textTheme.titleMedium?.copyWith(
                    fontFamily: 'AmiriQuran',
                    fontWeight: FontWeight.w700,
                    color: accent,
                  ),
                ),
                Text(
                  '${_arabicIndic(surah.ayahCount)} آية',
                  style: theme.textTheme.labelSmall,
                ),
              ],
            ),
          ),
          const SizedBox(width: 10),
          Icon(Icons.menu_book_rounded, color: accent, size: 21),
        ],
      ),
    );
  }
}

class _NextSurahFooter extends StatelessWidget {
  const _NextSurahFooter({required this.surah, required this.onTap});

  final QuranTextSurah surah;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final accent = theme.colorScheme.primary;
    return Padding(
      padding: const EdgeInsets.fromLTRB(18, 4, 18, 3),
      child: Material(
        color: accent.withOpacity(.08),
        borderRadius: BorderRadius.circular(15),
        child: InkWell(
          borderRadius: BorderRadius.circular(15),
          onTap: onTap,
          child: Container(
            constraints: const BoxConstraints(minHeight: 56),
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 9),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(15),
              border: Border.all(color: accent.withOpacity(.45)),
            ),
            child: Row(
              children: [
                Icon(Icons.chevron_left_rounded, color: accent, size: 23),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    'التالي: سورة ${surah.nameAr}',
                    textAlign: TextAlign.end,
                    style: theme.textTheme.titleSmall?.copyWith(
                      color: accent,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                Icon(Icons.menu_book_outlined, color: accent, size: 20),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _AyahLine extends StatelessWidget {
  const _AyahLine({
    required this.ayah,
    required this.fontSize,
    required this.isBookmarked,
    required this.isHighlighted,
    required this.onOpenActions,
  });

  final QuranTextAyah ayah;
  final double fontSize;
  final bool isBookmarked;
  final bool isHighlighted;
  final VoidCallback onOpenActions;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      margin: const EdgeInsets.symmetric(vertical: 5),
      padding: EdgeInsets.symmetric(
        horizontal: 5,
        vertical: isHighlighted ? 7 : 3,
      ),
      decoration: isHighlighted
          ? BoxDecoration(
              color: theme.colorScheme.primaryContainer.withOpacity(.32),
              borderRadius: BorderRadius.circular(13),
            )
          : null,
      child: Row(
        textDirection: TextDirection.rtl,
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Expanded(
            child: SelectableText(
              ayah.text,
              textAlign: TextAlign.right,
              textDirection: TextDirection.rtl,
              style: TextStyle(
                fontFamily: 'AmiriQuran',
                fontSize: fontSize,
                height: 2.05,
                color: theme.colorScheme.onSurface,
                fontWeight: FontWeight.normal,
              ),
            ),
          ),
          const SizedBox(width: 8),
          Tooltip(
            message: 'إجراءات الآية ${_arabicIndic(ayah.ayahNumber)}',
            child: InkWell(
              borderRadius: BorderRadius.circular(20),
              onTap: onOpenActions,
              child: _VerseMedallion(
                number: ayah.ayahNumber,
                isBookmarked: isBookmarked,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _VerseMedallion extends StatelessWidget {
  const _VerseMedallion({required this.number, required this.isBookmarked});

  final int number;
  final bool isBookmarked;

  @override
  Widget build(BuildContext context) {
    final accent = Theme.of(context).colorScheme.primary;
    return Container(
      width: 32,
      height: 32,
      padding: const EdgeInsets.all(3),
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: isBookmarked ? accent.withOpacity(.12) : Colors.transparent,
        border: Border.all(color: accent.withOpacity(.78), width: 1.1),
      ),
      child: Container(
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          border: Border.all(color: accent.withOpacity(.42), width: .8),
        ),
        child: Center(
          child: Text(
            _arabicIndic(number),
            style: TextStyle(
              fontFamily: 'AmiriQuran',
              fontSize: 12,
              fontWeight: FontWeight.w700,
              color: accent,
            ),
          ),
        ),
      ),
    );
  }
}

String _arabicIndic(int value) => value.toString().replaceAllMapped(
      RegExp(r'\d'),
      (match) => String.fromCharCode(0x0660 + int.parse(match[0]!)),
    );

class _AudioDock extends StatelessWidget {
  const _AudioDock({
    required this.label,
    required this.playing,
    required this.repeating,
    required this.showRepeat,
    required this.onPlayPause,
    required this.onRepeat,
    required this.onStop,
  });

  final String label;
  final bool playing;
  final bool repeating;
  final bool showRepeat;
  final VoidCallback onPlayPause;
  final VoidCallback onRepeat;
  final VoidCallback onStop;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final accent = theme.colorScheme.primary;
    return Card(
      margin: const EdgeInsets.fromLTRB(14, 3, 14, 5),
      color: theme.colorScheme.primaryContainer.withOpacity(.62),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
      child: Padding(
        padding: const EdgeInsetsDirectional.only(start: 6, end: 4),
        child: Row(
          children: [
            IconButton.filledTonal(
              tooltip: playing ? 'إيقاف مؤقت' : 'متابعة التلاوة',
              onPressed: onPlayPause,
              icon: Icon(playing ? Icons.pause_rounded : Icons.play_arrow_rounded),
            ),
            const SizedBox(width: 5),
            Expanded(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(label, maxLines: 1, overflow: TextOverflow.ellipsis,
                      style: const TextStyle(fontWeight: FontWeight.w800)),
                  Text('التلاوة الآن', style: theme.textTheme.labelSmall),
                ],
              ),
            ),
            if (showRepeat)
              IconButton(
                tooltip: repeating ? 'إيقاف التكرار' : 'تكرار الآية',
                onPressed: onRepeat,
                color: repeating ? accent : null,
                icon: const Icon(Icons.repeat_one_rounded),
              ),
            IconButton(
              tooltip: 'إيقاف التلاوة',
              onPressed: onStop,
              icon: const Icon(Icons.stop_rounded),
            ),
          ],
        ),
      ),
    );
  }
}

class _ReaderMessage extends StatelessWidget {
  const _ReaderMessage({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) => Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.warning_amber_outlined, size: 42),
              const SizedBox(height: 14),
              Text(message, textAlign: TextAlign.center),
            ],
          ),
        ),
      );
}
