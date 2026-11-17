import 'dart:async';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:just_audio/just_audio.dart';

import '../../core/quran_audio_service.dart';

typedef LocalAudioLoader = Future<List<LocalQuranAudio>> Function();

class AudioLibraryScreen extends StatefulWidget {
  const AudioLibraryScreen({super.key, this.loadLocalAudio});

  /// Injection seam for tests; production reads only files already on device.
  final LocalAudioLoader? loadLocalAudio;

  @override
  State<AudioLibraryScreen> createState() => _AudioLibraryScreenState();
}

class _AudioLibraryScreenState extends State<AudioLibraryScreen> {
  late Future<List<LocalQuranAudio>> _files;
  AudioPlayer? _player;
  StreamSubscription<PlayerState>? _playerStateSubscription;
  int? _playingSurah;

  @override
  void initState() {
    super.initState();
    _files = _loadLocalAudio();
  }

  Future<List<LocalQuranAudio>> _loadLocalAudio() =>
      widget.loadLocalAudio?.call() ?? QuranAudioService().listExistingSurahFiles();

  @override
  void dispose() {
    _playerStateSubscription?.cancel();
    _player?.dispose();
    super.dispose();
  }

  Future<void> _play(LocalQuranAudio audio) async {
    final player = _player ??= AudioPlayer();
    if (_playingSurah == audio.surahNumber && player.playing) {
      await player.pause();
      if (mounted) setState(() => _playingSurah = null);
      return;
    }
    try {
      await player.setAudioSource(AudioSource.file(audio.file.path));
      if (!mounted) return;
      setState(() => _playingSurah = audio.surahNumber);
      _playerStateSubscription ??= player.playerStateStream.listen((state) {
        if (state.processingState == ProcessingState.completed && mounted) {
          setState(() => _playingSurah = null);
        }
      });
      unawaited(player.play().catchError((Object _) {
        if (mounted) {
          setState(() => _playingSurah = null);
          _showMessage('تعذر تشغيل الملف الصوتي المحلي.');
        }
      }));
    } catch (_) {
      if (mounted) _showMessage('تعذر تشغيل الملف الصوتي المحلي.');
    }
  }

  void _showMessage(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final accent = theme.colorScheme.primary;
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(18, 16, 18, 8),
          child: Row(
            children: [
              Container(
                width: 48,
                height: 48,
                decoration: BoxDecoration(
                  color: accent.withOpacity(.12),
                  borderRadius: BorderRadius.circular(15),
                ),
                child: Icon(Icons.headphones_rounded, color: accent, size: 27),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('مكتبة الصوتيات',
                        style: theme.textTheme.headlineSmall
                            ?.copyWith(fontWeight: FontWeight.w900)),
                    Text('ملفات محفوظة على هذا الجهاز فقط',
                        style: theme.textTheme.bodySmall),
                  ],
                ),
              ),
            ],
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
          child: Card(
            color: theme.colorScheme.primaryContainer.withOpacity(.45),
            child: const Padding(
              padding: EdgeInsets.all(13),
              child: Text(
                'تعرض هذه المكتبة الملفات الصوتية المحلية الموجودة مسبقاً فقط. لا تفتح مواقع خارجية ولا تبث أو تنزّل صوتاً من الإنترنت.',
                style: TextStyle(height: 1.55),
              ),
            ),
          ),
        ),
        Expanded(
          child: FutureBuilder<List<LocalQuranAudio>>(
            future: _files,
            builder: (context, snapshot) {
              if (snapshot.connectionState == ConnectionState.waiting) {
                return const Center(child: CircularProgressIndicator());
              }
              if (snapshot.hasError) {
                return _LocalAudioMessage(
                  message: 'تعذر فحص الملفات الصوتية المحفوظة على هذا الجهاز.',
                  onRetry: () => setState(() => _files = _loadLocalAudio()),
                );
              }
              final files = snapshot.data ?? const <LocalQuranAudio>[];
              if (files.isEmpty) {
                return _LocalAudioMessage(
                  message: 'لا تتوفر حالياً ملفات تلاوة محلية محفوظة. لم نضف روابط أو ملفات غير مثبتة الحقوق.',
                  action: FilledButton.tonalIcon(
                    onPressed: () => context.go('/quran'),
                    icon: const Icon(Icons.menu_book_rounded),
                    label: const Text('فتح المصحف'),
                  ),
                );
              }
              return ListView.separated(
                padding: const EdgeInsets.fromLTRB(14, 4, 14, 18),
                itemCount: files.length,
                separatorBuilder: (_, __) => const SizedBox(height: 6),
                itemBuilder: (context, index) {
                  final audio = files[index];
                  final isPlaying = _playingSurah == audio.surahNumber;
                  return Card(
                    child: ListTile(
                      leading: const Icon(Icons.music_note_rounded),
                      title: Text('تلاوة محفوظة · السورة ${audio.surahNumber}',
                          style: const TextStyle(fontWeight: FontWeight.w800)),
                      subtitle: const Text('ملف محلي على هذا الجهاز'),
                      trailing: IconButton(
                        tooltip: isPlaying ? 'إيقاف مؤقت' : 'تشغيل الملف المحلي',
                        onPressed: () => _play(audio),
                        icon: Icon(isPlaying
                            ? Icons.pause_rounded
                            : Icons.play_arrow_rounded),
                      ),
                    ),
                  );
                },
              );
            },
          ),
        ),
      ],
    );
  }
}

class _LocalAudioMessage extends StatelessWidget {
  const _LocalAudioMessage({required this.message, this.action, this.onRetry});

  final String message;
  final Widget? action;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) => Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.library_music_outlined, size: 42),
              const SizedBox(height: 12),
              Text(message, textAlign: TextAlign.center),
              if (action != null) ...[
                const SizedBox(height: 14),
                action!,
              ],
              if (onRetry != null) ...[
                const SizedBox(height: 10),
                TextButton.icon(
                  onPressed: onRetry,
                  icon: const Icon(Icons.refresh_rounded),
                  label: const Text('إعادة الفحص'),
                ),
              ],
            ],
          ),
        ),
      );
}
