import 'dart:convert';

import 'package:crypto/crypto.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'providers.dart';

const quranAssetDirectory = 'assets/data/quran';
const quranManifestAssetPath = '$quranAssetDirectory/quran.json';
const quranTextAssetPath = '$quranAssetDirectory/ayahs.json';
const quranManifestSha256 =
    'e3a8645cc83b56fab524dc47a59f33d4c370bd1e70dc4d016b3bf6e426b2ea4a';

const _requiredDataFiles = <String>[
  'surahs.json',
  'ayahs.json',
  'juz.json',
  'hizb.json',
  'sajdah.json',
  'bookmarks.json',
  'quran_sources.json',
  'quran_attribution.json',
];

final quranTextLibraryProvider = FutureProvider<QuranTextLibrary>((ref) async {
  return QuranTextLibrary.load();
});

final quranLocalStoreProvider = Provider<QuranLocalStore>(
  (ref) => QuranLocalStore(ref.watch(sharedPreferencesProvider)),
);

final quranBookmarksProvider = FutureProvider<List<QuranBookmark>>(
  (ref) => ref.watch(quranLocalStoreProvider).loadBookmarks(),
);

final quranLastReadProvider = FutureProvider<QuranPosition?>(
  (ref) => ref.watch(quranLocalStoreProvider).loadLastRead(),
);

class QuranTextLibrary {
  const QuranTextLibrary({
    required this.surahs,
    required this.ayahsBySurah,
    required this.juzs,
    required this.hizbs,
    required this.hizbQuarters,
    required this.sajdahs,
    required this.reading,
    required this.scriptName,
    required this.bundleVersion,
    required this.sourceSha256,
    required this.sourceLabel,
    required this.audio,
  });

  final List<QuranTextSurah> surahs;
  final Map<int, List<QuranTextAyah>> ayahsBySurah;
  final List<QuranDivision> juzs;
  final List<QuranDivision> hizbs;
  final List<QuranQuarter> hizbQuarters;
  final List<QuranSajdah> sajdahs;
  final String reading;
  final String scriptName;
  final String bundleVersion;
  final String sourceSha256;
  final String sourceLabel;
  final Map<String, dynamic> audio;

  int get totalAyahs =>
      ayahsBySurah.values.fold<int>(0, (sum, rows) => sum + rows.length);

  Map<int, QuranTextSurah> get surahsByNumber => {
        for (final surah in surahs) surah.number: surah,
      };

  QuranTextSurah? surahByNumber(int number) {
    if (number < 1 || number > surahs.length) return null;
    return surahs[number - 1];
  }

  List<QuranTextAyah> ayahsFor(int surahNumber) =>
      ayahsBySurah[surahNumber] ?? const <QuranTextAyah>[];

  List<QuranSearchResult> search(String query, {int limit = 40}) {
    final key = normalizeQuranSearchText(query);
    if (key.isEmpty || limit < 1) return const <QuranSearchResult>[];
    final matches = <QuranSearchResult>[];
    for (final surah in surahs) {
      for (final ayah in ayahsFor(surah.number)) {
        if (ayah.searchKey.contains(key)) {
          matches.add(QuranSearchResult(surah: surah, ayah: ayah));
          if (matches.length >= limit)
            return List<QuranSearchResult>.unmodifiable(matches);
        }
      }
    }
    return List<QuranSearchResult>.unmodifiable(matches);
  }

  static Future<QuranTextLibrary> load({
    Future<String> Function(String path)? assetLoader,
  }) async {
    final loader = assetLoader ?? rootBundle.loadString;
    final manifestText = await loader(quranManifestAssetPath);
    final files = <String, String>{};
    for (final name in _requiredDataFiles) {
      files[name] = await loader('$quranAssetDirectory/$name');
    }
    return parse(manifestText: manifestText, dataFiles: files);
  }

  static QuranTextLibrary parse({
    required String manifestText,
    required Map<String, String> dataFiles,
  }) {
    final manifestDigest = sha256.convert(utf8.encode(manifestText)).toString();
    if (manifestDigest != quranManifestSha256) {
      throw const FormatException(
          'Quran manifest SHA-256 does not match the reviewed bundle.');
    }
    final manifest = _object(jsonDecode(manifestText), 'quran.json');
    final integrity = _object(manifest['integrity'], 'quran.json.integrity');
    if (manifest['schema_version'] != 1 ||
        manifest['reading'] is! Map ||
        _object(manifest['reading'], 'quran.json.reading')['id'] != 'hafs' ||
        _object(manifest['script'], 'quran.json.script')['id'] != 'simple') {
      throw const FormatException(
          'The pinned Quran bundle is not Hafs/Imla\'ei.');
    }

    for (final name in _requiredDataFiles) {
      final text = dataFiles[name];
      final expected = integrity[name];
      if (text == null || expected is! String || expected.isEmpty) {
        throw FormatException(
            'A required Quran data file or checksum is missing: $name.');
      }
      final actual = sha256.convert(utf8.encode(text)).toString();
      if (actual != expected) {
        throw FormatException('Quran data SHA-256 mismatch for $name.');
      }
    }

    final ayahsHash =
        sha256.convert(utf8.encode(dataFiles['ayahs.json']!)).toString();
    if (ayahsHash != manifest['text_sha256'] ||
        ayahsHash != manifest['text_source_sha256']) {
      throw const FormatException(
          'The Quran text snapshot hash does not match its source.');
    }

    final counts = _object(manifest['counts'], 'quran.json.counts');
    if (counts['surahs'] != 114 ||
        counts['ayahs'] != 6236 ||
        counts['juz'] != 30 ||
        counts['hizb'] != 60 ||
        counts['hizb_quarters'] != 240) {
      throw const FormatException(
          'Quran bundle counts do not match the expected Hafs numbering.');
    }

    final surahData =
        _object(jsonDecode(dataFiles['surahs.json']!), 'surahs.json');
    final rawSurahs = _array(surahData['items'], 'surahs.json.items');
    if (rawSurahs.length != 114) {
      throw const FormatException('Surah index must contain 114 entries.');
    }
    final surahs = <QuranTextSurah>[];
    for (var index = 0; index < rawSurahs.length; index++) {
      final row = _object(rawSurahs[index], 'surah row');
      final surah = QuranTextSurah.fromJson(row);
      if (surah.number != index + 1 ||
          surah.nameAr.isEmpty ||
          surah.ayahCount < 1) {
        throw const FormatException(
            'Surah index is incomplete or out of order.');
      }
      surahs.add(surah);
    }

    final juzData = _object(jsonDecode(dataFiles['juz.json']!), 'juz.json');
    final juzs = _readDivisions(
        juzData['items'], 'juz.json.items', surahs.length,
        expectedCount: 30);
    final hizbData = _object(jsonDecode(dataFiles['hizb.json']!), 'hizb.json');
    final hizbs = _readDivisions(
        hizbData['items'], 'hizb.json.items', surahs.length,
        expectedCount: 60);
    final quarters = <QuranQuarter>[];
    final rawQuarters = _array(hizbData['quarters'], 'hizb.json.quarters');
    if (rawQuarters.length != 240 || hizbData['quarter_count'] != 240) {
      throw const FormatException(
          'Hizb quarter index must contain 240 start positions.');
    }
    for (var index = 0; index < rawQuarters.length; index++) {
      final row = _object(rawQuarters[index], 'hizb quarter row');
      final start = _readPosition(row['start'], surahs.length);
      if (row['number'] != index + 1 ||
          row['hizb_number'] is! int ||
          row['quarter_in_hizb'] is! int) {
        throw const FormatException('Hizb quarter index is invalid.');
      }
      quarters.add(QuranQuarter(
        number: index + 1,
        hizbNumber: row['hizb_number'] as int,
        quarterInHizb: row['quarter_in_hizb'] as int,
        startSurah: start.surah,
        startAyah: start.ayah,
      ));
    }

    final sajdahData =
        _object(jsonDecode(dataFiles['sajdah.json']!), 'sajdah.json');
    final rawSajdahs = _array(sajdahData['items'], 'sajdah.json.items');
    if (rawSajdahs.length != 15 || sajdahData['count'] != rawSajdahs.length) {
      throw const FormatException(
          'Sajdah index must contain the 15 source positions.');
    }
    final sajdahs = <QuranSajdah>[];
    for (var index = 0; index < rawSajdahs.length; index++) {
      final row = _object(rawSajdahs[index], 'sajdah row');
      final position = _readPosition(row['position'], surahs.length);
      if (row['number'] != index + 1 ||
          position.ayah > surahs[position.surah - 1].ayahCount) {
        throw const FormatException('Sajdah reference is outside a surah.');
      }
      sajdahs.add(QuranSajdah(
        number: index + 1,
        surahNumber: position.surah,
        ayahNumber: position.ayah,
        sourceType: row['source_type']?.toString() ?? 'unspecified',
      ));
    }

    final ayahRoot =
        _object(jsonDecode(dataFiles['ayahs.json']!), 'ayahs.json');
    if (ayahRoot.length != 114) {
      throw const FormatException(
          'Ayah text must contain exactly 114 surah keys.');
    }
    final sajdahKeys = sajdahs
        .map((entry) => '${entry.surahNumber}:${entry.ayahNumber}')
        .toSet();
    final grouped = <int, List<QuranTextAyah>>{};
    var totalAyahs = 0;
    for (var surahNumber = 1; surahNumber <= 114; surahNumber++) {
      final rawRows =
          _array(ayahRoot['$surahNumber'], 'ayahs.json.$surahNumber');
      final surah = surahs[surahNumber - 1];
      if (rawRows.length != surah.ayahCount) {
        throw FormatException('Ayah count mismatch in surah $surahNumber.');
      }
      final rows = <QuranTextAyah>[];
      for (var index = 0; index < rawRows.length; index++) {
        final row = _object(rawRows[index], 'ayah row');
        final text = row['text'];
        if (row['chapter'] != surahNumber ||
            row['verse'] != index + 1 ||
            text is! String ||
            text.trim().isEmpty) {
          throw FormatException(
              'Invalid verse sequence or text at $surahNumber:${index + 1}.');
        }
        final ayahNumber = index + 1;
        rows.add(QuranTextAyah(
          surahNumber: surahNumber,
          ayahNumber: ayahNumber,
          text: text,
          juzNumber: _divisionAt(juzs, surahNumber, ayahNumber),
          hizbNumber: _divisionAt(hizbs, surahNumber, ayahNumber),
          sajdah: sajdahKeys.contains('$surahNumber:$ayahNumber'),
        ));
        totalAyahs++;
      }
      grouped[surahNumber] = List<QuranTextAyah>.unmodifiable(rows);
    }
    if (totalAyahs != 6236 || manifest['reading']['ayah_count'] != totalAyahs) {
      throw const FormatException(
          'The bundle must contain 6,236 sequential Hafs ayahs.');
    }

    final attribution = _object(
        jsonDecode(dataFiles['quran_attribution.json']!),
        'quran_attribution.json');
    final textSource = _object(
        attribution['text_source'], 'quran_attribution.json.text_source');
    final audio = _object(manifest['audio'], 'quran.json.audio');
    final audioAttribution =
        _object(attribution['audio'], 'quran_attribution.json.audio');
    if (audio['reciter_id'] != audioAttribution['reciter_id'] ||
        audio['reading'] != 'hafs' ||
        audio['bundled_in_app'] != false) {
      throw const FormatException(
          'Audio source metadata is inconsistent or unlicensed.');
    }

    return QuranTextLibrary(
      surahs: List<QuranTextSurah>.unmodifiable(surahs),
      ayahsBySurah: Map<int, List<QuranTextAyah>>.unmodifiable(grouped),
      juzs: List<QuranDivision>.unmodifiable(juzs),
      hizbs: List<QuranDivision>.unmodifiable(hizbs),
      hizbQuarters: List<QuranQuarter>.unmodifiable(quarters),
      sajdahs: List<QuranSajdah>.unmodifiable(sajdahs),
      reading: 'حفص عن عاصم',
      scriptName: _object(manifest['script'], 'quran.json.script')['name_ar']
              ?.toString() ??
          '',
      bundleVersion: manifest['bundle_version']?.toString() ?? '',
      sourceSha256: ayahsHash,
      sourceLabel: textSource['dataset']?.toString() ?? 'Tanzil',
      audio: Map<String, dynamic>.unmodifiable(audio),
    );
  }
}

class QuranTextSurah {
  const QuranTextSurah({
    required this.number,
    required this.nameAr,
    required this.transliteration,
    required this.revelationType,
    required this.ayahCount,
  });

  final int number;
  final String nameAr;
  final String transliteration;
  final String revelationType;
  final int ayahCount;

  factory QuranTextSurah.fromJson(Map<String, dynamic> json) => QuranTextSurah(
        number: _integer(json['number'], 'surah.number'),
        nameAr: json['name_ar']?.toString() ?? '',
        transliteration: json['transliteration']?.toString() ?? '',
        revelationType: json['revelation_type']?.toString() ?? '',
        ayahCount: _integer(json['ayah_count'], 'surah.ayah_count'),
      );
}

class QuranTextAyah {
  QuranTextAyah({
    required this.surahNumber,
    required this.ayahNumber,
    required this.text,
    required this.juzNumber,
    required this.hizbNumber,
    required this.sajdah,
  }) : searchKey = normalizeQuranSearchText(text);

  final int surahNumber;
  final int ayahNumber;
  final String text;
  final String searchKey;
  final int juzNumber;
  final int hizbNumber;
  final bool sajdah;
}

class QuranDivision {
  const QuranDivision({
    required this.number,
    required this.startSurah,
    required this.startAyah,
  });

  final int number;
  final int startSurah;
  final int startAyah;
}

class QuranQuarter {
  const QuranQuarter({
    required this.number,
    required this.hizbNumber,
    required this.quarterInHizb,
    required this.startSurah,
    required this.startAyah,
  });

  final int number;
  final int hizbNumber;
  final int quarterInHizb;
  final int startSurah;
  final int startAyah;
}

class QuranSajdah {
  const QuranSajdah({
    required this.number,
    required this.surahNumber,
    required this.ayahNumber,
    required this.sourceType,
  });

  final int number;
  final int surahNumber;
  final int ayahNumber;
  final String sourceType;
}

class QuranSearchResult {
  const QuranSearchResult({required this.surah, required this.ayah});

  final QuranTextSurah surah;
  final QuranTextAyah ayah;
}

String normalizeQuranSearchText(String input) => input
    .replaceAll(RegExp(r'[\u064B-\u065F\u0670\u06D6-\u06ED]'), '')
    .replaceAll('ـ', '')
    .replaceAll(RegExp('[أإآٱ]'), 'ا')
    .replaceAll(RegExp(r'\s+'), ' ')
    .trim()
    .toLowerCase();

class QuranPosition {
  const QuranPosition({required this.surahNumber, required this.ayahNumber});

  final int surahNumber;
  final int ayahNumber;

  String get key => '$surahNumber:$ayahNumber';

  Map<String, dynamic> toJson() => {
        'surah_number': surahNumber,
        'ayah_number': ayahNumber,
      };

  factory QuranPosition.fromJson(Map<String, dynamic> json) => QuranPosition(
        surahNumber: _integer(json['surah_number'], 'position.surah_number'),
        ayahNumber: _integer(json['ayah_number'], 'position.ayah_number'),
      );
}

class QuranBookmark extends QuranPosition {
  const QuranBookmark({
    required super.surahNumber,
    required super.ayahNumber,
    required this.savedAt,
  });

  final DateTime savedAt;

  @override
  Map<String, dynamic> toJson() => {
        ...super.toJson(),
        'saved_at': savedAt.toUtc().toIso8601String(),
      };

  factory QuranBookmark.fromJson(Map<String, dynamic> json) => QuranBookmark(
        surahNumber: _integer(json['surah_number'], 'bookmark.surah_number'),
        ayahNumber: _integer(json['ayah_number'], 'bookmark.ayah_number'),
        savedAt: DateTime.tryParse(json['saved_at']?.toString() ?? '') ??
            DateTime.fromMillisecondsSinceEpoch(0, isUtc: true),
      );
}

class QuranLocalStore {
  const QuranLocalStore(this._preferences);

  static const _bookmarksKey = 'quran_local_bookmarks_v1';
  static const _lastReadKey = 'quran_local_last_read_v1';
  final SharedPreferences _preferences;

  List<QuranBookmark> loadBookmarks() {
    final raw = _preferences.getString(_bookmarksKey);
    if (raw == null) return const <QuranBookmark>[];
    try {
      final decoded = jsonDecode(raw);
      if (decoded is! List) return const <QuranBookmark>[];
      final bookmarks = decoded
          .whereType<Map>()
          .map((row) => QuranBookmark.fromJson(Map<String, dynamic>.from(row)))
          .where(_isValidPosition)
          .toList()
        ..sort((a, b) => b.savedAt.compareTo(a.savedAt));
      return List<QuranBookmark>.unmodifiable(bookmarks);
    } catch (_) {
      return const <QuranBookmark>[];
    }
  }

  Future<bool> toggleBookmark(QuranPosition position) async {
    final bookmarks = loadBookmarks().toList();
    final existingIndex =
        bookmarks.indexWhere((item) => item.key == position.key);
    final added = existingIndex < 0;
    if (added) {
      bookmarks.insert(
          0,
          QuranBookmark(
            surahNumber: position.surahNumber,
            ayahNumber: position.ayahNumber,
            savedAt: DateTime.now().toUtc(),
          ));
    } else {
      bookmarks.removeAt(existingIndex);
    }
    await _preferences.setString(
      _bookmarksKey,
      jsonEncode(
          bookmarks.map((item) => item.toJson()).toList(growable: false)),
    );
    return added;
  }

  QuranPosition? loadLastRead() {
    final raw = _preferences.getString(_lastReadKey);
    if (raw == null) return null;
    try {
      final decoded = jsonDecode(raw);
      if (decoded is! Map) return null;
      final position =
          QuranPosition.fromJson(Map<String, dynamic>.from(decoded));
      return _isValidPosition(position) ? position : null;
    } catch (_) {
      return null;
    }
  }

  Future<void> saveLastRead(QuranPosition position) async {
    if (!_isValidPosition(position)) return;
    await _preferences.setString(
      _lastReadKey,
      jsonEncode({
        ...position.toJson(),
        'saved_at': DateTime.now().toUtc().toIso8601String(),
      }),
    );
  }
}

bool _isValidPosition(QuranPosition position) =>
    position.surahNumber >= 1 &&
    position.surahNumber <= 114 &&
    position.ayahNumber >= 1;

List<QuranDivision> _readDivisions(
  dynamic value,
  String label,
  int surahCount, {
  required int expectedCount,
}) {
  final rows = _array(value, label);
  if (rows.length != expectedCount)
    throw FormatException('$label must contain $expectedCount entries.');
  final result = <QuranDivision>[];
  for (var index = 0; index < rows.length; index++) {
    final row = _object(rows[index], label);
    final start = _readPosition(row['start'], surahCount);
    if (row['number'] != index + 1)
      throw FormatException('$label numbering is not sequential.');
    result.add(QuranDivision(
        number: index + 1, startSurah: start.surah, startAyah: start.ayah));
  }
  return result;
}

({int surah, int ayah}) _readPosition(dynamic value, int surahCount) {
  final row = _object(value, 'position');
  final surah = _integer(row['surah'], 'position.surah');
  final ayah = _integer(row['ayah'], 'position.ayah');
  if (surah < 1 || surah > surahCount || ayah < 1) {
    throw const FormatException('A Quran index position is out of range.');
  }
  return (surah: surah, ayah: ayah);
}

int _divisionAt(List<QuranDivision> divisions, int surah, int ayah) {
  var selected = 1;
  for (final division in divisions) {
    if (division.startSurah < surah ||
        (division.startSurah == surah && division.startAyah <= ayah)) {
      selected = division.number;
    } else {
      break;
    }
  }
  return selected;
}

Map<String, dynamic> _object(dynamic value, String label) {
  if (value is Map) return Map<String, dynamic>.from(value);
  throw FormatException('$label must be a JSON object.');
}

List<dynamic> _array(dynamic value, String label) {
  if (value is List) return value;
  throw FormatException('$label must be a JSON array.');
}

int _integer(dynamic value, String label) {
  if (value is int) return value;
  if (value is num) return value.toInt();
  throw FormatException('$label must be an integer.');
}
