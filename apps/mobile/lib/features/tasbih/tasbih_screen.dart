import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app/theme.dart';
import '../../core/app_preferences.dart';
import '../../core/providers.dart';

class TasbihScreen extends ConsumerStatefulWidget {
  const TasbihScreen({super.key});

  @override
  ConsumerState<TasbihScreen> createState() => _TasbihScreenState();
}

class _TasbihScreenState extends ConsumerState<TasbihScreen> {
  static const _fixedPhrases = <String>[
    'اللَّهُ أَكْبَرُ',
    'الْحَمْدُ لِلَّهِ',
    'سُبْحَانَ اللَّهِ',
  ];
  static const _fixedTargets = <int>[34, 33, 33];
  static const _totalFixedTarget = 100;
  static const _sequenceVersion = 2;
  static const _sequenceVersionKey = 'tasbih_fatimi_sequence_version';
  static const _stepKey = 'tasbih_fatimi_v2_step';
  static const _countKey = 'tasbih_fatimi_v2_count';

  static const _infinitePhrases = <String>[
    'اللَّهُ أَكْبَرُ',
    'سُبْحَانَ اللَّهِ',
    'الْحَمْدُ لِلَّهِ',
    'لَا إِلَهَ إِلَّا اللَّهُ',
    'أَسْتَغْفِرُ اللَّهَ',
    'اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ وَآلِ مُحَمَّدٍ',
  ];

  bool _loaded = false;
  bool _infiniteMode = false;
  bool _showLegacyNotice = false;
  int _normalStep = 0;
  int _normalCount = 0;
  int _infiniteCount = 0;
  String _infinitePhrase = _infinitePhrases.first;
  late final TextEditingController _customPhraseController;
  Future<void> _pendingPersistence = Future<void>.value();

  int get _displayCount => _infiniteMode ? _infiniteCount : _normalCount;
  String get _displayPhrase =>
      _infiniteMode ? _infinitePhrase : _fixedPhrases[_normalStep];
  int get _currentTarget => _fixedTargets[_normalStep];

  int get _cycleCount {
    var completedBefore = 0;
    for (var index = 0; index < _normalStep; index++) {
      completedBefore += _fixedTargets[index];
    }
    return completedBefore + _normalCount;
  }

  @override
  void initState() {
    super.initState();
    _customPhraseController = TextEditingController();
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_loaded) return;
    _loaded = true;
    final prefs = ref.read(sharedPreferencesProvider);
    final sequenceVersion = prefs.getInt(_sequenceVersionKey);
    if (sequenceVersion == _sequenceVersion) {
      _normalStep = (prefs.getInt(_stepKey) ?? 0)
          .clamp(0, _fixedPhrases.length - 1)
          .toInt();
      _normalCount = (prefs.getInt(_countKey) ?? 0)
          .clamp(0, _fixedTargets[_normalStep] - 1)
          .toInt();
    } else {
      // Preserve legacy tasbih_normal_* keys. The first old step is the same
      // phrase, so its partial count can be carried forward; later old steps
      // used a different order and must not be relabelled as the new order.
      final oldStep =
          (prefs.getInt('tasbih_normal_step') ?? 0).clamp(0, 2).toInt();
      final oldCount =
          (prefs.getInt('tasbih_normal_count') ?? 0).clamp(0, 33).toInt();
      if (oldStep == 0) {
        _normalStep = 0;
        _normalCount = oldCount.clamp(0, _fixedTargets.first - 1).toInt();
      } else {
        _normalStep = 0;
        _normalCount = 0;
        _showLegacyNotice = oldCount > 0 || oldStep > 0;
      }
    }
    _infiniteCount =
        (prefs.getInt('tasbih_infinite_count') ?? 0).clamp(0, 1 << 30).toInt();
    _infinitePhrase =
        prefs.getString('tasbih_infinite_phrase') ?? _infinitePhrases.first;
    if (!_infinitePhrases.contains(_infinitePhrase))
      _infinitePhrase = _infinitePhrases.first;
    _infiniteMode = prefs.getBool('tasbih_infinite_mode') ?? false;
    _customPhraseController.text = _infinitePhrase;
  }

  @override
  void dispose() {
    _customPhraseController.dispose();
    super.dispose();
  }

  Future<void> _persist() {
    final prefs = ref.read(sharedPreferencesProvider);
    final step = _normalStep;
    final count = _normalCount;
    final infiniteCount = _infiniteCount;
    final infinitePhrase = _infinitePhrase;
    final infiniteMode = _infiniteMode;
    final write = _pendingPersistence.then((_) async {
      await prefs.setInt(_stepKey, step);
      await prefs.setInt(_countKey, count);
      await prefs.setInt(_sequenceVersionKey, _sequenceVersion);
      await prefs.setInt('tasbih_infinite_count', infiniteCount);
      await prefs.setString('tasbih_infinite_phrase', infinitePhrase);
      await prefs.setBool('tasbih_infinite_mode', infiniteMode);
    });
    _pendingPersistence = write;
    return write;
  }

  Future<void> _increment() async {
    final haptics = ref.read(appPreferencesProvider).hapticsEnabled;
    String? notice;
    setState(() {
      if (_infiniteMode) {
        _infiniteCount++;
      } else if (_normalCount + 1 < _currentTarget) {
        _normalCount++;
      } else if (_normalStep < _fixedPhrases.length - 1) {
        final completedPhrase = _fixedPhrases[_normalStep];
        final completedTarget = _currentTarget;
        _normalStep++;
        _normalCount = 0;
        notice =
            'اكتمل $completedPhrase ($completedTarget)، انتقل إلى ${_fixedPhrases[_normalStep]}.';
      } else {
        _normalStep = 0;
        _normalCount = 0;
        notice =
            'اكتملت دورة التسبيح الفاطمي (100 ذكر)؛ عاد العداد إلى الصفر تلقائياً.';
      }
    });
    await _persist();
    if (haptics) await HapticFeedback.selectionClick();
    if (notice != null && mounted) {
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(SnackBar(
            content: Text(notice!), duration: const Duration(seconds: 2)));
    }
  }

  Future<void> _resetCurrent() async {
    setState(() {
      if (_infiniteMode) {
        _infiniteCount = 0;
      } else {
        _normalStep = 0;
        _normalCount = 0;
      }
    });
    await _persist();
  }

  Future<void> _selectInfinitePhrase(String? phrase) async {
    if (phrase == null) return;
    setState(() {
      _infinitePhrase = phrase;
      _customPhraseController.text = phrase;
    });
    await _persist();
  }

  Future<void> _useCustomPhrase(String value) async {
    final phrase = value.trim();
    if (phrase.isEmpty) return;
    setState(() => _infinitePhrase = phrase);
    await _persist();
  }

  Future<void> _setMode(bool infinite) async {
    setState(() => _infiniteMode = infinite);
    await _persist();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final haptics = ref.watch(appPreferencesProvider).hapticsEnabled;
    final progress = _infiniteMode
        ? 0.0
        : (_cycleCount / _totalFixedTarget).clamp(0.0, 1.0).toDouble();

    return ListView(
      padding: const EdgeInsets.fromLTRB(18, 18, 18, 30),
      children: [
        Text('السبحة الإلكترونية',
            style: theme.textTheme.headlineSmall
                ?.copyWith(fontWeight: FontWeight.w900)),
        const SizedBox(height: 5),
        Text('اختر التسبيح الفاطمي المرتب أو التسبيح المفتوح بلا حد.',
            style: theme.textTheme.bodySmall),
        const SizedBox(height: 16),
        SegmentedButton<bool>(
          segments: const [
            ButtonSegment(
                value: false,
                icon: Icon(Icons.format_list_numbered_rounded),
                label: Text('التسبيح الفاطمي')),
            ButtonSegment(
                value: true,
                icon: Icon(Icons.all_inclusive_rounded),
                label: Text('تسبيح لا نهائي')),
          ],
          selected: {_infiniteMode},
          onSelectionChanged: (value) => _setMode(value.first),
          showSelectedIcon: false,
        ),
        if (_showLegacyNotice && !_infiniteMode) ...[
          const SizedBox(height: 10),
          Card(
            child: ListTile(
              leading: const Icon(Icons.info_outline_rounded),
              title: const Text('بدأ عدّ جديد للترتيب الفاطمي 34/33/33.'),
              subtitle: const Text(
                  'لم تُحذف بيانات العداد السابق؛ حُفظت منفصلة كي لا تختلط الأذكار أو أعدادها.'),
              trailing: IconButton(
                tooltip: 'إخفاء التنبيه',
                onPressed: () => setState(() => _showLegacyNotice = false),
                icon: const Icon(Icons.close_rounded),
              ),
            ),
          ),
        ],
        const SizedBox(height: 14),
        Card(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 18, 16, 18),
            child: Column(
              children: [
                if (_infiniteMode) ...[
                  Align(
                      alignment: AlignmentDirectional.centerStart,
                      child: Text('اختر الذكر',
                          style: theme.textTheme.titleSmall
                              ?.copyWith(fontWeight: FontWeight.w900))),
                  const SizedBox(height: 8),
                  DropdownButtonFormField<String>(
                    value: _infinitePhrases.contains(_infinitePhrase)
                        ? _infinitePhrase
                        : null,
                    isExpanded: true,
                    decoration: const InputDecoration(
                        prefixIcon: Icon(Icons.auto_awesome_rounded)),
                    items: _infinitePhrases
                        .map((phrase) => DropdownMenuItem(
                            value: phrase,
                            child:
                                Text(phrase, overflow: TextOverflow.ellipsis)))
                        .toList(),
                    onChanged: _selectInfinitePhrase,
                  ),
                  const SizedBox(height: 10),
                  TextField(
                    controller: _customPhraseController,
                    textAlign: TextAlign.center,
                    decoration: const InputDecoration(
                        labelText: 'أو اكتب ذكراً مخصصاً'),
                    onSubmitted: _useCustomPhrase,
                    onEditingComplete: () =>
                        _useCustomPhrase(_customPhraseController.text),
                  ),
                  const SizedBox(height: 12),
                ] else ...[
                  Row(
                    children: [
                      Text('الذكر ${_normalStep + 1} من 3',
                          style: theme.textTheme.titleSmall
                              ?.copyWith(fontWeight: FontWeight.w900)),
                      const Spacer(),
                      Text('$_cycleCount / $_totalFixedTarget',
                          style: theme.textTheme.bodySmall
                              ?.copyWith(fontWeight: FontWeight.w800)),
                    ],
                  ),
                  const SizedBox(height: 15),
                ],
                SizedBox(
                  width: 226,
                  height: 226,
                  child: Stack(
                    alignment: Alignment.center,
                    children: [
                      SizedBox.expand(
                          child: CircularProgressIndicator(
                        value: _infiniteMode ? null : progress,
                        strokeWidth: 8,
                        strokeCap: StrokeCap.round,
                        backgroundColor:
                            theme.colorScheme.surfaceContainerHighest,
                        color: KhazainPalette.green,
                      )),
                      Material(
                        color: Colors.transparent,
                        shape: const CircleBorder(),
                        child: InkWell(
                          onTap: _increment,
                          customBorder: const CircleBorder(),
                          child: Ink(
                            width: 195,
                            height: 195,
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              gradient: LinearGradient(
                                begin: Alignment.topLeft,
                                end: Alignment.bottomRight,
                                colors: [
                                  KhazainPalette.green,
                                  const Color(0xFF256D35)
                                ],
                              ),
                              boxShadow: [
                                BoxShadow(
                                    color:
                                        KhazainPalette.green.withOpacity(.22),
                                    blurRadius: 20,
                                    offset: const Offset(0, 8))
                              ],
                            ),
                            child: Column(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                Text('$_displayCount',
                                    style: const TextStyle(
                                        fontSize: 55,
                                        height: 1.05,
                                        fontWeight: FontWeight.w900,
                                        color: Colors.white)),
                                const SizedBox(height: 5),
                                const Text('اضغط للتسبيح',
                                    style: TextStyle(
                                        color: Colors.white,
                                        fontWeight: FontWeight.w800)),
                                const SizedBox(height: 4),
                                Text(
                                    _infiniteMode
                                        ? 'بلا حد'
                                        : 'من $_currentTarget',
                                    style: const TextStyle(
                                        color: Color(0xFFE4F5E5),
                                        fontSize: 12)),
                              ],
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 12),
                Text(_displayPhrase,
                    textAlign: TextAlign.center,
                    style: theme.textTheme.titleMedium
                        ?.copyWith(fontWeight: FontWeight.w900, height: 1.8)),
                const SizedBox(height: 7),
                if (!_infiniteMode)
                  Text(
                    'المتبقي في هذا الذكر: ${_currentTarget - _normalCount}',
                    style: theme.textTheme.bodySmall
                        ?.copyWith(fontWeight: FontWeight.w800),
                    textAlign: TextAlign.center,
                  )
                else
                  Text('يستمر العد بلا توقف حتى تصفّره بنفسك.',
                      style: theme.textTheme.bodySmall,
                      textAlign: TextAlign.center),
                const SizedBox(height: 16),
                OutlinedButton.icon(
                  onPressed: _resetCurrent,
                  icon: const Icon(Icons.restart_alt_rounded),
                  label: Text(_infiniteMode
                      ? 'تصفير العداد'
                      : 'إعادة التسبيح من البداية'),
                ),
              ],
            ),
          ),
        ),
        if (!_infiniteMode) ...[
          const SizedBox(height: 13),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(13),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Text('ترتيب التسبيح الفاطمي',
                      style: TextStyle(fontWeight: FontWeight.w900)),
                  const SizedBox(height: 8),
                  for (var i = 0; i < _fixedPhrases.length; i++)
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 5),
                      child: Row(
                        children: [
                          Icon(
                              i < _normalStep
                                  ? Icons.check_circle_rounded
                                  : i == _normalStep
                                      ? Icons.radio_button_checked_rounded
                                      : Icons.circle_outlined,
                              size: 19,
                              color: i <= _normalStep
                                  ? KhazainPalette.green
                                  : theme.disabledColor),
                          const SizedBox(width: 9),
                          Expanded(
                              child: Text(_fixedPhrases[i],
                                  style: TextStyle(
                                      fontWeight: i == _normalStep
                                          ? FontWeight.w900
                                          : FontWeight.w500))),
                          Text(
                            '${i < _normalStep ? _fixedTargets[i] : i == _normalStep ? _normalCount : 0}/${_fixedTargets[i]}',
                            style: theme.textTheme.bodySmall,
                          ),
                        ],
                      ),
                    ),
                ],
              ),
            ),
          ),
        ],
        const SizedBox(height: 12),
        Card(
          child: SwitchListTile.adaptive(
            value: haptics,
            onChanged: (value) => ref
                .read(appPreferencesProvider.notifier)
                .setHapticsEnabled(value),
            secondary:
                Icon(Icons.vibration_rounded, color: theme.colorScheme.primary),
            title: const Text('الاهتزاز عند التسبيح',
                style: TextStyle(fontWeight: FontWeight.w800)),
            subtitle: const Text('اهتزاز خفيف مع كل ضغطة'),
          ),
        ),
        const SizedBox(height: 8),
        Text('تُحفظ أعداد التسبيح والذكر المختار محلياً على جهازك.',
            style: theme.textTheme.bodySmall?.copyWith(height: 1.5)),
      ],
    );
  }
}
