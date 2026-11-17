class Governorate {
  const Governorate(
      {required this.id,
      required this.nameAr,
      required this.nameEn,
      required this.latitude,
      required this.longitude,
      required this.timezone,
      required this.coordinatesQuality,
      this.aliases = const <String>[]});
  final String id;
  final String nameAr;
  final String nameEn;
  final double latitude;
  final double longitude;
  final String timezone;
  final String coordinatesQuality;
  final List<String> aliases;

  factory Governorate.fromJson(Map<String, dynamic> json) => Governorate(
        id: json['id'] as String,
        nameAr: json['name_ar'] as String,
        nameEn: json['name_en'] as String,
        latitude: (json['latitude'] as num).toDouble(),
        longitude: (json['longitude'] as num).toDouble(),
        timezone: json['timezone'] as String,
        coordinatesQuality: json['coordinates_quality'] as String? ?? 'unknown',
        aliases: (json['aliases'] as List<dynamic>? ?? const <dynamic>[])
            .whereType<String>()
            .toList(growable: false),
      );
}

class PrayerCityEntry {
  const PrayerCityEntry({
    required this.cityId,
    required this.governorateId,
    required this.governorateNameAr,
    required this.cityNameAr,
    required this.cityNameEn,
    required this.latitude,
    required this.longitude,
    required this.timezone,
    required this.coordinateSource,
    required this.coordinateAccuracy,
    required this.verified,
  });

  final String cityId;
  final String governorateId;
  final String governorateNameAr;
  final String cityNameAr;
  final String cityNameEn;
  final double latitude;
  final double longitude;
  final String timezone;
  final String coordinateSource;
  final String coordinateAccuracy;
  final bool verified;

  factory PrayerCityEntry.fromJson(Map<String, dynamic> json) =>
      PrayerCityEntry(
        cityId: json['city_id'] as String,
        governorateId: json['governorate_id'] as String,
        governorateNameAr: json['governorate_name_ar'] as String,
        cityNameAr: json['city_name_ar'] as String,
        cityNameEn: json['city_name_en'] as String,
        latitude: (json['latitude'] as num).toDouble(),
        longitude: (json['longitude'] as num).toDouble(),
        timezone: json['timezone'] as String,
        coordinateSource: json['coordinate_source'] as String? ?? '',
        coordinateAccuracy: json['coordinate_accuracy'] as String? ?? 'unknown',
        verified: json['verified'] == true,
      );
}

enum PrayerTimesSource { localCalculation, misrQuranApi }

class PrayerTimesResult {
  const PrayerTimesResult({
    required this.displayTimes,
    required this.date,
    required this.methodLabel,
    required this.definitions,
    required this.isStale,
    this.source = PrayerTimesSource.localCalculation,
    this.hijriDate,
    this.locationName,
    this.coordinateAccuracy,
    this.coordinateSource,
    this.locationVerified,
  });
  final Map<String, String> displayTimes;
  final String date;
  final String methodLabel;
  final Map<String, dynamic> definitions;
  final bool isStale;
  final PrayerTimesSource source;
  final String? hijriDate;
  final String? locationName;
  final String? coordinateAccuracy;
  final String? coordinateSource;
  final bool? locationVerified;

  String get sourceLabel => switch (source) {
        PrayerTimesSource.misrQuranApi => 'خدمة MisrQuran (غير معتمدة فقهياً)',
        PrayerTimesSource.localCalculation =>
          'حساب التطبيق المحلي (غير معتمد فقهياً)',
      };

  PrayerTimesResult copyWith({
    bool? isStale,
    PrayerTimesSource? source,
    String? hijriDate,
    String? locationName,
    String? coordinateAccuracy,
    String? coordinateSource,
    bool? locationVerified,
  }) =>
      PrayerTimesResult(
        displayTimes: displayTimes,
        date: date,
        methodLabel: methodLabel,
        definitions: definitions,
        isStale: isStale ?? this.isStale,
        source: source ?? this.source,
        hijriDate: hijriDate ?? this.hijriDate,
        locationName: locationName ?? this.locationName,
        coordinateAccuracy: coordinateAccuracy ?? this.coordinateAccuracy,
        coordinateSource: coordinateSource ?? this.coordinateSource,
        locationVerified: locationVerified ?? this.locationVerified,
      );

  factory PrayerTimesResult.fromJson(Map<String, dynamic> json,
      {bool isStale = false}) {
    final display = Map<String, dynamic>.from(json['display_times'] as Map);
    final location = json['location'] is Map
        ? Map<String, dynamic>.from(json['location'] as Map)
        : const <String, dynamic>{};
    return PrayerTimesResult(
      displayTimes:
          display.map((key, value) => MapEntry(key, value.toString())),
      date: json['date'] as String,
      methodLabel:
          (json['method'] as Map)['label_ar'] as String? ?? 'غير معروف',
      definitions:
          Map<String, dynamic>.from(json['definitions'] as Map? ?? const {}),
      isStale: isStale,
      source: json['source'] == 'misr_quran_api'
          ? PrayerTimesSource.misrQuranApi
          : PrayerTimesSource.localCalculation,
      hijriDate: json['hijri_date']?.toString(),
      locationName: location['name_ar']?.toString(),
      coordinateAccuracy: location['coordinates_quality']?.toString(),
      coordinateSource: location['coordinate_source']?.toString(),
      locationVerified:
          location['verified'] is bool ? location['verified'] as bool : null,
    );
  }
  Map<String, dynamic> toJson() => {
        'date': date,
        'display_times': displayTimes,
        'method': {'label_ar': methodLabel},
        'definitions': definitions,
        'source': source == PrayerTimesSource.misrQuranApi
            ? 'misr_quran_api'
            : 'local_calculation',
        if (hijriDate != null) 'hijri_date': hijriDate,
        'location': {
          'name_ar': locationName,
          'coordinates_quality': coordinateAccuracy,
          'coordinate_source': coordinateSource,
          'verified': locationVerified,
        },
      };
}

class CatalogResult {
  const CatalogResult(
      {required this.items, required this.total, required this.isStale});
  final List<Map<String, dynamic>> items;
  final int total;
  final bool isStale;

  factory CatalogResult.fromJson(Map<String, dynamic> json,
      {bool isStale = false}) {
    final rawItems = json['items'];
    final items = rawItems is List
        ? rawItems
            .whereType<Map>()
            .map((item) => Map<String, dynamic>.from(item))
            .toList(growable: false)
        : const <Map<String, dynamic>>[];
    final rawTotal = json['total'];
    return CatalogResult(
      items: items,
      total: rawTotal is num ? rawTotal.toInt() : items.length,
      isStale: isStale,
    );
  }

  Map<String, dynamic> toJson() => {'items': items, 'total': total};
}

class FavoriteEntry {
  const FavoriteEntry({
    required this.entityType,
    required this.entityId,
    required this.title,
    required this.subtitle,
    required this.available,
    this.isStale = false,
  });

  final String entityType;
  final String entityId;
  final String title;
  final String subtitle;
  final bool available;
  final bool isStale;

  factory FavoriteEntry.fromJson(Map<String, dynamic> json,
          {bool isStale = false}) =>
      FavoriteEntry(
        entityType: json['entity_type']?.toString() ?? '',
        entityId: json['entity_id']?.toString() ?? '',
        title: json['title']?.toString() ?? 'مادة محفوظة',
        subtitle: json['subtitle']?.toString() ?? '',
        available: json['available'] == true,
        isStale: isStale || json['_stale'] == true,
      );

  Map<String, dynamic> toJson() => {
        'entity_type': entityType,
        'entity_id': entityId,
        'title': title,
        'subtitle': subtitle,
        'available': available,
        '_stale': isStale,
      };
}

class QuranSurahEntry {
  const QuranSurahEntry({
    required this.number,
    required this.nameAr,
    required this.nameEn,
    required this.ayahCount,
  });

  final int number;
  final String nameAr;
  final String nameEn;
  final int ayahCount;

  factory QuranSurahEntry.fromJson(Map<String, dynamic> json) =>
      QuranSurahEntry(
        number: (json['number'] as num).toInt(),
        nameAr: json['name_ar']?.toString() ?? '',
        nameEn: json['name_en']?.toString() ?? '',
        ayahCount: (json['ayah_count'] as num).toInt(),
      );
}

class QuranAyahEntry {
  const QuranAyahEntry({
    required this.surahNumber,
    required this.ayahNumber,
    required this.textUthmani,
    required this.juzNumber,
    required this.hizbNumber,
    required this.pageNumber,
    required this.sajdah,
  });

  final int surahNumber;
  final int ayahNumber;
  final String textUthmani;
  final int? juzNumber;
  final int? hizbNumber;
  final int? pageNumber;
  final bool sajdah;

  factory QuranAyahEntry.fromJson(Map<String, dynamic> json) => QuranAyahEntry(
        surahNumber: (json['surah_number'] as num).toInt(),
        ayahNumber: (json['ayah_number'] as num).toInt(),
        textUthmani: json['text_uthmani']?.toString() ?? '',
        juzNumber: (json['juz_number'] as num?)?.toInt(),
        hizbNumber: (json['hizb_number'] as num?)?.toInt(),
        pageNumber: (json['page_number'] as num?)?.toInt(),
        sajdah: json['sajdah'] == true,
      );
}

class QuranAyahPage {
  const QuranAyahPage({
    required this.surah,
    required this.items,
    required this.total,
    required this.source,
    required this.sourceVersion,
    required this.textSha256,
  });

  final QuranSurahEntry surah;
  final List<QuranAyahEntry> items;
  final int total;
  final String source;
  final String sourceVersion;
  final String textSha256;

  factory QuranAyahPage.fromJson(Map<String, dynamic> json) {
    final surahData = Map<String, dynamic>.from(json['surah'] as Map);
    final rawItems = json['items'];
    final items = rawItems is List
        ? rawItems
            .whereType<Map>()
            .map((item) =>
                QuranAyahEntry.fromJson(Map<String, dynamic>.from(item)))
            .toList(growable: false)
        : const <QuranAyahEntry>[];
    return QuranAyahPage(
      surah: QuranSurahEntry.fromJson(surahData),
      items: items,
      total: (json['total'] as num?)?.toInt() ?? items.length,
      source: json['source']?.toString() ?? '',
      sourceVersion: json['source_version']?.toString() ?? '',
      textSha256: json['text_sha256']?.toString() ?? '',
    );
  }
}

class SourceCatalogEntry {
  const SourceCatalogEntry({
    required this.sourceId,
    required this.name,
    required this.category,
    required this.officialUrl,
    required this.observed,
    required this.useFor,
    required this.licenseStatus,
    required this.notes,
  });

  final String sourceId;
  final String name;
  final String category;
  final String officialUrl;
  final String observed;
  final String useFor;
  final String licenseStatus;
  final String notes;

  factory SourceCatalogEntry.fromJson(Map<String, dynamic> json) =>
      SourceCatalogEntry(
        sourceId: json['source_id']?.toString() ?? '',
        name: json['name']?.toString() ?? '',
        category: json['category']?.toString() ?? '',
        officialUrl: json['official_url']?.toString() ?? '',
        observed: json['observed']?.toString() ?? '',
        useFor: json['use_for']?.toString() ?? '',
        licenseStatus: json['license_status']?.toString() ?? 'unknown',
        notes: json['notes']?.toString() ?? '',
      );
}
