import 'dart:io';

import 'package:path_provider/path_provider.dart';

import 'quran_text_repository.dart';

/// Finds previously saved recitations on this device; it never fetches audio.
class QuranAudioService {
  static const reciterId = 'ar.alafasy';

  Future<List<LocalQuranAudio>> listExistingSurahFiles() async {
    final directory = await _audioDirectory();
    final files = <LocalQuranAudio>[];
    for (var number = 1; number <= 114; number++) {
      final file = File(
        '${directory.path}${Platform.pathSeparator}surah_${number.toString().padLeft(3, '0')}.mp3',
      );
      if (await file.exists() && await file.length() > 0) {
        files.add(LocalQuranAudio(surahNumber: number, file: file));
      }
    }
    return files;
  }

  Future<File?> existingSurahFile(int surahNumber) async {
    if (surahNumber < 1 || surahNumber > 114) return null;
    final directory = await _audioDirectory();
    final file = File(
      '${directory.path}${Platform.pathSeparator}surah_${surahNumber.toString().padLeft(3, '0')}.mp3',
    );
    if (!await file.exists() || await file.length() == 0) return null;
    return file;
  }

  Future<File?> existingAyahFile(int globalAyahNumber) async {
    if (globalAyahNumber < 1 || globalAyahNumber > 6236) return null;
    final directory = await _audioDirectory();
    final file = File(
      '${directory.path}${Platform.pathSeparator}ayah_${globalAyahNumber.toString().padLeft(4, '0')}.mp3',
    );
    if (!await file.exists() || await file.length() == 0) return null;
    return file;
  }

  int globalAyahNumber(
      QuranTextLibrary library, int surahNumber, int ayahNumber) {
    final surah = library.surahByNumber(surahNumber);
    if (surah == null || ayahNumber < 1 || ayahNumber > surah.ayahCount) {
      throw RangeError('Invalid Quran position: $surahNumber:$ayahNumber');
    }
    var globalNumber = ayahNumber;
    for (var number = 1; number < surahNumber; number++) {
      globalNumber += library.surahs[number - 1].ayahCount;
    }
    if (globalNumber < 1 || globalNumber > 6236) {
      throw StateError('The global Hafs ayah number is outside 1-6236.');
    }
    return globalNumber;
  }

  Future<Directory> _audioDirectory() async {
    final documents = await getApplicationDocumentsDirectory();
    final separator = Platform.pathSeparator;
    // Keep the pre-existing cache location so files already on a user's device
    // remain discoverable and are not deleted or moved.
    return Directory(
      '${documents.path}${separator}quran_audio${separator}islamic_network${separator}$reciterId',
    );
  }
}

class LocalQuranAudio {
  const LocalQuranAudio({required this.surahNumber, required this.file});

  final int surahNumber;
  final File file;
}
