import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:khazain_al_huda/core/quran_text_repository.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test(
      'loads the pinned Hafs/Imla\'ei bundle with exact source text and full indexes',
      () async {
    final library = await QuranTextLibrary.load();

    expect(library.surahs, hasLength(114));
    expect(library.totalAyahs, 6236);
    expect(library.surahs.first.nameAr, 'الفاتحة');
    expect(library.surahs.last.nameAr, 'الناس');
    expect(library.ayahsFor(1).first.text,
        'بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ');
    expect(library.ayahsFor(9).first.text, startsWith('بَرَاءَةٌ'));
    expect(library.ayahsFor(114).last.ayahNumber, 6);
    expect(library.juzs, hasLength(30));
    expect(library.hizbs, hasLength(60));
    expect(library.hizbQuarters, hasLength(240));
    expect(library.sajdahs, hasLength(15));
    expect(library.ayahsFor(1).first.juzNumber, 1);
    expect(library.ayahsFor(1).first.hizbNumber, 1);
    expect(library.reading, 'حفص عن عاصم');
    expect(library.sourceLabel, 'Tanzil Quran Text 1.1 — Simple / Imla\'ei');
    expect(
      library.sourceSha256,
      '703246a73a0a3d57f721b02e3edad9a2ee6831b79f49ffb9513ddfe99a65b801',
    );
  });

  test('search removes diacritics only from the search key, not displayed text',
      () async {
    final library = await QuranTextLibrary.load();
    final firstAyah = library.ayahsFor(1).first;

    expect(library.search('بسم الله الرحمن الرحيم'), isNotEmpty);
    expect(firstAyah.text, 'بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ');
    expect(firstAyah.searchKey, 'بسم الله الرحمن الرحيم');
  });

  test('rejects changed data even when the JSON remains parseable', () async {
    final manifestText = await rootBundle.loadString(quranManifestAssetPath);
    final names = <String>[
      'surahs.json',
      'ayahs.json',
      'juz.json',
      'hizb.json',
      'sajdah.json',
      'bookmarks.json',
      'quran_sources.json',
      'quran_attribution.json',
    ];
    final files = <String, String>{};
    for (final name in names) {
      files[name] = await rootBundle.loadString('$quranAssetDirectory/$name');
    }
    files['ayahs.json'] = files['ayahs.json']!.replaceFirst('بِسْمِ', 'بسم');

    expect(
      () =>
          QuranTextLibrary.parse(manifestText: manifestText, dataFiles: files),
      throwsFormatException,
    );
  });

  test('rejects an unpinned manifest instead of displaying unknown Quran data',
      () {
    expect(
      () => QuranTextLibrary.parse(
          manifestText: '{"schema_version":1}', dataFiles: const {}),
      throwsFormatException,
    );
  });

  test('bookmarks and last reading position are stored locally', () async {
    SharedPreferences.setMockInitialValues({});
    final preferences = await SharedPreferences.getInstance();
    final store = QuranLocalStore(preferences);
    const position = QuranPosition(surahNumber: 2, ayahNumber: 255);

    expect(await store.toggleBookmark(position), isTrue);
    expect(store.loadBookmarks().single.key, '2:255');
    expect(await store.toggleBookmark(position), isFalse);
    expect(store.loadBookmarks(), isEmpty);

    await store.saveLastRead(position);
    expect(store.loadLastRead()?.key, '2:255');
  });
}
