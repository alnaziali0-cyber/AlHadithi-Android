import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../core/providers.dart';

class BookReaderScreen extends ConsumerStatefulWidget {
  const BookReaderScreen({
    super.key,
    required this.bookId,
    required this.bookTitle,
    required this.volumeCount,
  });

  final int bookId;
  final String bookTitle;
  final int volumeCount;

  @override
  ConsumerState<BookReaderScreen> createState() => _BookReaderScreenState();
}

class _BookReaderScreenState extends ConsumerState<BookReaderScreen> {
  static const int _pageBatchSize = 40;
  final List<Map<String, dynamic>> _pages = [];
  int _volume = 1;
  int? _startPageNumber;
  int _offset = 0;
  int _total = 0;
  bool _loading = false;
  bool _loadingMore = false;
  bool _restoringProgress = true;
  bool _isStale = false;
  String? _error;
  String? _savedProgressLabel;
  bool _progressIsLocalOnly = false;
  int? _savingPageNumber;

  @override
  void initState() {
    super.initState();
    _initializeReader();
  }

  Future<void> _initializeReader() async {
    await _purgeLegacyBookPageCache();
    final progress = await _loadSavedProgress();
    int? startPage;
    if (progress != null) {
      final savedVolume = progress['volume_number'];
      final savedPage = progress['page_number'];
      if (savedVolume is num && savedPage is num) {
        final volume = savedVolume.toInt();
        final page = savedPage.toInt();
        if (volume >= 1 && volume <= widget.volumeCount && page >= 1) {
          _volume = volume;
          startPage = page;
        }
      }
    }
    if (!mounted) return;
    setState(() => _restoringProgress = false);
    await _loadPages(reset: true, startPageNumber: startPage);
  }

  String get _progressCacheKey => 'book_progress_local_${widget.bookId}';

  String _formatProgressLabel(Map<String, dynamic> progress) {
    final volume = progress['volume_number']?.toString() ?? '1';
    final page = progress['page_number']?.toString() ?? '—';
    return 'آخر موضع قراءة محفوظ: المجلد $volume · الصفحة $page';
  }

  Future<Map<String, dynamic>?> _loadSavedProgress() async {
    final prefs = ref.read(sharedPreferencesProvider);
    Map<String, dynamic>? local;
    final encoded = prefs.getString(_progressCacheKey);
    if (encoded != null) {
      try {
        local = Map<String, dynamic>.from(jsonDecode(encoded) as Map);
        if (mounted) {
          setState(() {
            _savedProgressLabel = _formatProgressLabel(local!);
            _progressIsLocalOnly = local!['synced'] != true;
          });
        }
      } catch (_) {
        await prefs.remove(_progressCacheKey);
        local = null;
      }
    }

    try {
      final remote = await ref.read(deviceLibraryApiProvider).getBookProgress(widget.bookId);
      final pendingLocal = local;
      if (pendingLocal != null && pendingLocal['synced'] != true) {
        final synchronized = await ref.read(deviceLibraryApiProvider).saveBookProgress(
          bookId: widget.bookId,
          volumeNumber: (pendingLocal['volume_number'] as num).toInt(),
          pageNumber: (pendingLocal['page_number'] as num).toInt(),
        );
        final syncedLocal = <String, dynamic>{
          ...pendingLocal,
          'synced': true,
          'updated_at': synchronized['updated_at'] ?? DateTime.now().toUtc().toIso8601String(),
        };
        await prefs.setString(_progressCacheKey, jsonEncode(syncedLocal));
        if (mounted) {
          setState(() {
            _savedProgressLabel = _formatProgressLabel(syncedLocal);
            _progressIsLocalOnly = false;
          });
        }
        return syncedLocal;
      }
      if (remote != null) {
        final cachedRemote = <String, dynamic>{...remote, 'synced': true};
        await prefs.setString(_progressCacheKey, jsonEncode(cachedRemote));
        if (mounted) {
          setState(() {
            _savedProgressLabel = _formatProgressLabel(cachedRemote);
            _progressIsLocalOnly = false;
          });
        }
        return cachedRemote;
      }
      return local;
    } catch (_) {
      // A locally saved position remains available and will be retried next time.
      if (mounted && local != null) setState(() => _progressIsLocalOnly = true);
      return local;
    }
  }

  Future<void> _saveReadingPosition(int pageNumber) async {
    if (pageNumber < 1 || _savingPageNumber != null) return;
    setState(() => _savingPageNumber = pageNumber);
    final prefs = ref.read(sharedPreferencesProvider);
    final local = <String, dynamic>{
      'volume_number': _volume,
      'page_number': pageNumber,
      'updated_at': DateTime.now().toUtc().toIso8601String(),
      'synced': false,
    };
    try {
      await prefs.setString(_progressCacheKey, jsonEncode(local));
      if (mounted) {
        setState(() {
          _savedProgressLabel = _formatProgressLabel(local);
          _progressIsLocalOnly = true;
        });
      }
      try {
        final saved = await ref.read(deviceLibraryApiProvider).saveBookProgress(
          bookId: widget.bookId,
          volumeNumber: _volume,
          pageNumber: pageNumber,
        );
        local['synced'] = true;
        local['updated_at'] = saved['updated_at'] ?? local['updated_at'];
        await prefs.setString(_progressCacheKey, jsonEncode(local));
        if (mounted) {
          setState(() {
            _savedProgressLabel = _formatProgressLabel(local);
            _progressIsLocalOnly = false;
          });
        }
      } catch (_) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('حُفظ موضع القراءة على الجهاز، وستتم محاولة مزامنته عند عودة الاتصال.')),
          );
        }
      }
    } finally {
      if (mounted) setState(() => _savingPageNumber = null);
    }
  }

  Future<void> _loadPages({required bool reset, int? startPageNumber}) async {
    if (_loading || _loadingMore) return;
    final requestedOffset = reset ? 0 : _offset;
    if (reset) {
      setState(() {
        _loading = true;
        _error = null;
        _pages.clear();
        _offset = 0;
        _startPageNumber = startPageNumber;
        _isStale = false;
      });
    } else {
      setState(() {
        _loadingMore = true;
        _error = null;
      });
    }

    final cacheKey = 'book_pages_v2_${widget.bookId}_${_volume}_${_startPageNumber ?? 0}_$requestedOffset';
    try {
      final response = await ref.read(apiProvider).get<Map<String, dynamic>>(
        '/api/v1/public/books/${widget.bookId}/pages',
        queryParameters: {
          'volume_number': _volume,
          'limit': _pageBatchSize,
          'offset': requestedOffset,
          if (_startPageNumber != null) 'start_page_number': _startPageNumber,
        },
      );
      final payload = Map<String, dynamic>.from(response.data ?? const {});
      final prefs = ref.read(sharedPreferencesProvider);
      final rawBook = payload['book'];
      final offlineAllowed = rawBook is Map && rawBook['offline_access_allowed'] == true;
      if (offlineAllowed) {
        await prefs.setString(cacheKey, jsonEncode(payload));
        final rawItems = payload['items'];
        if (rawItems is List) {
          for (final rawPage in rawItems.whereType<Map>()) {
            final page = Map<String, dynamic>.from(rawPage);
            final pageNumber = page['page_number'];
            if (pageNumber is num && pageNumber.toInt() > 0) {
              final pageKey = 'book_page_v2_${widget.bookId}_${_volume}_${pageNumber.toInt()}';
              await prefs.setString(pageKey, jsonEncode({
                'book': rawBook,
                'items': [page],
                'total': 1,
                'limit': 1,
                'offset': 0,
                'volume_number': _volume,
              }));
            }
          }
        }
      } else {
        await _clearOfflineBookCache(prefs);
      }
      if (!mounted) return;
      _appendPagePayload(payload, stale: false, requestedOffset: requestedOffset);
    } on DioException catch (error) {
      if (!mounted) return;
      final prefs = ref.read(sharedPreferencesProvider);
      final cached = prefs.getString(cacheKey);
      if (cached != null) {
        try {
          final payload = Map<String, dynamic>.from(jsonDecode(cached) as Map);
          if (!mounted) return;
          _appendPagePayload(payload, stale: true, requestedOffset: requestedOffset);
        } catch (_) {
          if (mounted) setState(() => _error = 'تعذر قراءة النسخة المحفوظة من الصفحات.');
        }
      } else {
        final cachedPages = <Map<String, dynamic>>[];
        final prefix = 'book_page_v2_${widget.bookId}_${_volume}_';
        for (final key in prefs.getKeys().where((entry) => entry.startsWith(prefix))) {
          final encodedPage = prefs.getString(key);
          if (encodedPage == null) continue;
          try {
            final pagePayload = Map<String, dynamic>.from(jsonDecode(encodedPage) as Map);
            final items = pagePayload['items'];
            if (items is List && items.isNotEmpty && items.first is Map) {
              final page = Map<String, dynamic>.from(items.first as Map);
              final pageNumber = page['page_number'];
              if (pageNumber is num &&
                  (_startPageNumber == null || pageNumber.toInt() >= _startPageNumber!)) {
                cachedPages.add(page);
              }
            }
          } catch (_) {
            await prefs.remove(key);
          }
        }
        cachedPages.sort((left, right) =>
            (left['page_number'] as num).compareTo(right['page_number'] as num));
        final remaining = cachedPages.skip(requestedOffset).take(_pageBatchSize).toList();
        if (remaining.isNotEmpty) {
          if (!mounted) return;
          _appendPagePayload({
            'book': {'id': widget.bookId, 'offline_access_allowed': true},
            'items': remaining,
            'total': cachedPages.length,
            'limit': _pageBatchSize,
            'offset': requestedOffset,
            'volume_number': _volume,
          }, stale: true, requestedOffset: requestedOffset);
        } else if (mounted) {
          setState(() => _error = error.response?.statusCode == 404
              ? 'هذا الكتاب غير متاح أو لم يجتز شروط النشر وحقوق الاستخدام.'
              : 'تعذر تحميل صفحات الكتاب. تحقق من اتصال الإنترنت ثم أعد المحاولة.');
        }
      }
      // Legacy cache keys are deliberately ignored because they predate rights-based offline gating.
    } catch (_) {
      if (mounted) setState(() => _error = 'تعذر معالجة بيانات صفحات الكتاب.');
    } finally {
      if (mounted) {
        setState(() {
          _loading = false;
          _loadingMore = false;
        });
      }
    }
  }

  Future<void> _purgeLegacyBookPageCache() async {
    final prefs = ref.read(sharedPreferencesProvider);
    final legacyPrefix = 'book_pages_${widget.bookId}_';
    final legacyKeys = prefs.getKeys()
        .where((key) => key.startsWith(legacyPrefix))
        .toList(growable: false);
    for (final key in legacyKeys) {
      await prefs.remove(key);
    }
  }

  Future<void> _clearOfflineBookCache(SharedPreferences prefs) async {
    final pagePrefix = 'book_page_v2_${widget.bookId}_';
    final batchPrefix = 'book_pages_v2_${widget.bookId}_';
    final keys = prefs.getKeys().where(
      (key) => key.startsWith(pagePrefix) || key.startsWith(batchPrefix),
    ).toList(growable: false);
    for (final key in keys) {
      await prefs.remove(key);
    }
  }

  void _appendPagePayload(
    Map<String, dynamic> payload, {
    required bool stale,
    required int requestedOffset,
  }) {
    final rawItems = payload['items'];
    final newPages = rawItems is List
        ? rawItems.whereType<Map>().map((item) => Map<String, dynamic>.from(item)).toList()
        : <Map<String, dynamic>>[];
    final rawTotal = payload['total'];
    setState(() {
      if (requestedOffset == 0) _pages.clear();
      _pages.addAll(newPages);
      _total = rawTotal is num ? rawTotal.toInt() : _pages.length;
      _offset = requestedOffset + newPages.length;
      _isStale = _isStale || stale;
      _error = null;
    });
  }

  Future<void> _changeVolume(int? volume) async {
    if (volume == null || volume == _volume || _loading || _loadingMore) return;
    setState(() => _volume = volume);
    await _loadPages(reset: true);
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.bookTitle, maxLines: 1, overflow: TextOverflow.ellipsis),
        actions: [
          IconButton(
            tooltip: 'إعادة تحميل الصفحات',
            onPressed: _loading || _loadingMore ? null : () => _loadPages(reset: true, startPageNumber: _startPageNumber),
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: SafeArea(
        child: Column(
          children: [
            if (widget.volumeCount > 1)
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
                child: DropdownButtonFormField<int>(
                  value: _volume,
                  decoration: const InputDecoration(labelText: 'المجلد'),
                  items: List.generate(
                    widget.volumeCount,
                    (index) => DropdownMenuItem(value: index + 1, child: Text('المجلد ${index + 1}')),
                  ),
                  onChanged: _changeVolume,
                ),
              ),
            if (_isStale)
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(10),
                color: theme.colorScheme.tertiaryContainer,
                child: const Text('تُعرض صفحات محفوظة دون اتصال؛ قد تكون هناك صفحات أحدث على الخادم.'),
              ),
            if (_savedProgressLabel != null)
              Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 9),
                color: theme.colorScheme.surfaceContainerHighest,
                child: Row(
                  children: [
                    const Icon(Icons.bookmark_outline, size: 18),
                    const SizedBox(width: 8),
                    Expanded(child: Text('$_savedProgressLabel${_progressIsLocalOnly ? ' · محفوظ محلياً' : ''}')),
                  ],
                ),
              ),
            if (_error != null)
              Padding(
                padding: const EdgeInsets.all(16),
                child: Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      children: [
                        Text(_error!, textAlign: TextAlign.center),
                        const SizedBox(height: 8),
                        FilledButton.icon(
                          onPressed: _loading || _loadingMore ? null : () => _loadPages(reset: _pages.isEmpty, startPageNumber: _startPageNumber),
                          icon: const Icon(Icons.refresh),
                          label: const Text('إعادة المحاولة'),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            Expanded(
              child: (_loading || _restoringProgress) && _pages.isEmpty
                  ? const Center(child: CircularProgressIndicator())
                  : _pages.isEmpty
                      ? const Center(child: Padding(
                          padding: EdgeInsets.all(24),
                          child: Text('لم تُرفع صفحات رقمية لهذا الكتاب بعد. ستظهر الصفحات بعد إدخالها ومراجعة حقوق نشر الطبعة.'),
                        ))
                      : ListView.builder(
                          padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
                          itemCount: _pages.length + 1,
                          itemBuilder: (context, index) {
                            if (index == _pages.length) {
                              final hasMore = _pages.length < _total;
                              if (!hasMore) {
                                return Padding(
                                  padding: const EdgeInsets.all(18),
                                  child: Text('وصلت إلى نهاية الصفحات المتاحة ($_total).', textAlign: TextAlign.center),
                                );
                              }
                              return Padding(
                                padding: const EdgeInsets.symmetric(vertical: 16),
                                child: Center(
                                  child: OutlinedButton.icon(
                                    onPressed: _loadingMore ? null : () => _loadPages(reset: false),
                                    icon: _loadingMore
                                        ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                                        : const Icon(Icons.expand_more),
                                    label: Text(_loadingMore ? 'جارٍ التحميل' : 'تحميل صفحات إضافية'),
                                  ),
                                ),
                              );
                            }
                            final page = _pages[index];
                            final pageNumber = page['page_number']?.toString() ?? '—';
                            final text = page['text_content']?.toString() ?? '';
                            return Card(
                              margin: const EdgeInsets.only(bottom: 12),
                              child: Padding(
                                padding: const EdgeInsets.all(18),
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.stretch,
                                  children: [
                                    Row(
                                      children: [
                                        Expanded(
                                          child: Text(
                                            'صفحة $pageNumber',
                                            style: theme.textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w800, color: theme.colorScheme.secondary),
                                          ),
                                        ),
                                        IconButton(
                                          tooltip: 'حفظ موضع القراءة في هذه الصفحة',
                                          onPressed: _savingPageNumber != null || int.tryParse(pageNumber) == null
                                              ? null
                                              : () => _saveReadingPosition(int.parse(pageNumber)),
                                          icon: _savingPageNumber == int.tryParse(pageNumber)
                                              ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
                                              : const Icon(Icons.bookmark_add_outlined),
                                        ),
                                      ],
                                    ),
                                    const Divider(height: 22),
                                    SelectableText(text, style: theme.textTheme.bodyLarge?.copyWith(height: 2.0)),
                                  ],
                                ),
                              ),
                            );
                          },
                        ),
            ),
          ],
        ),
      ),
    );
  }
}
