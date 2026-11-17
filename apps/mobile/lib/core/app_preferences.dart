import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'providers.dart';

/// Prayers whose displayed time the user may shift manually (minutes).
/// Asr is intentionally absent: the backend does not calculate it yet.
const adjustablePrayerKeys = ['fajr', 'dhuhr', 'maghrib', 'isha'];

const prayerOffsetMinLimit = -30;
const prayerOffsetMaxLimit = 30;

/// Keys that describe reference times rather than obligatory prayers.
const referenceTimeKeys = ['sunrise', 'sunset'];

enum AppTextSize {
  small('small', 'صغير', 0.9),
  standard('standard', 'افتراضي', 1.0),
  large('large', 'كبير', 1.15),
  extraLarge('extra_large', 'كبير جداً', 1.3);

  const AppTextSize(this.storageKey, this.labelAr, this.factor);
  final String storageKey;
  final String labelAr;
  final double factor;

  static AppTextSize fromStorage(String? value) {
    for (final size in AppTextSize.values) {
      if (size.storageKey == value) return size;
    }
    return AppTextSize.standard;
  }
}

enum AppFontFamily {
  notoNaskh('noto_naskh', 'نسخ عربي (افتراضي)', 'NotoNaskhArabicUI'),
  alanSans('alan_sans', 'Alan Sans', 'AlanSansLight'),
  geeza('geeza', 'Geeza', 'GeezaArabic'),
  beinArabic('bein_arabic', 'beIN Arabic', 'BeINArabic');

  const AppFontFamily(this.storageKey, this.labelAr, this.themeFamily);
  final String storageKey;
  final String labelAr;
  final String themeFamily;

  static AppFontFamily fromStorage(String? value) {
    for (final font in AppFontFamily.values) {
      if (font.storageKey == value) return font;
    }
    return AppFontFamily.notoNaskh;
  }
}

/// Parses "HH:mm" and returns the minute of day, or null when malformed.
int? minuteOfDayFromHhmm(String value) {
  final match = RegExp(r'^(\d{1,2}):(\d{2})$').firstMatch(value.trim());
  if (match == null) return null;
  final hours = int.parse(match.group(1)!);
  final minutes = int.parse(match.group(2)!);
  if (hours > 23 || minutes > 59) return null;
  return hours * 60 + minutes;
}

String hhmmFromMinuteOfDay(int minuteOfDay) {
  final hours = minuteOfDay ~/ 60;
  final minutes = minuteOfDay % 60;
  return '${hours.toString().padLeft(2, '0')}:${minutes.toString().padLeft(2, '0')}';
}

/// Shifts a "HH:mm" value by [minutes], clamped inside the same calendar day
/// so an adjustment never wraps a Fajr value into the previous night.
String shiftHhmm(String value, int minutes) {
  final base = minuteOfDayFromHhmm(value);
  if (base == null || minutes == 0) return value;
  final shifted = (base + minutes).clamp(0, 23 * 60 + 59).toInt();
  return hhmmFromMinuteOfDay(shifted);
}

/// Returns a copy of [times] with manual offsets applied to adjustable prayers.
/// Keys without an offset, and values that are not "HH:mm", are left untouched.
Map<String, String> applyPrayerOffsets(
    Map<String, String> times, Map<String, int> offsets) {
  final adjusted = Map<String, String>.from(times);
  for (final key in adjustablePrayerKeys) {
    final value = times[key];
    final offset = offsets[key] ?? 0;
    if (value != null && offset != 0) {
      adjusted[key] = shiftHhmm(value, offset);
    }
  }
  return adjusted;
}

class AppPreferences {
  const AppPreferences({
    this.textSize = AppTextSize.standard,
    this.appFontFamily = AppFontFamily.notoNaskh,
    this.hapticsEnabled = true,
    this.showReferenceTimes = true,
    this.prayerNotificationsEnabled = false,
    this.prayerOffsets = const {},
  });

  final AppTextSize textSize;
  final AppFontFamily appFontFamily;
  final bool hapticsEnabled;
  final bool showReferenceTimes;
  final bool prayerNotificationsEnabled;
  final Map<String, int> prayerOffsets;

  int offsetFor(String prayerKey) => prayerOffsets[prayerKey] ?? 0;

  AppPreferences copyWith({
    AppTextSize? textSize,
    AppFontFamily? appFontFamily,
    bool? hapticsEnabled,
    bool? showReferenceTimes,
    bool? prayerNotificationsEnabled,
    Map<String, int>? prayerOffsets,
  }) {
    return AppPreferences(
      textSize: textSize ?? this.textSize,
      appFontFamily: appFontFamily ?? this.appFontFamily,
      hapticsEnabled: hapticsEnabled ?? this.hapticsEnabled,
      showReferenceTimes: showReferenceTimes ?? this.showReferenceTimes,
      prayerNotificationsEnabled:
          prayerNotificationsEnabled ?? this.prayerNotificationsEnabled,
      prayerOffsets: prayerOffsets ?? this.prayerOffsets,
    );
  }
}

class AppPreferencesController extends StateNotifier<AppPreferences> {
  AppPreferencesController(this._prefs) : super(_read(_prefs));

  final SharedPreferences _prefs;

  static const _textSizeKey = 'display_text_size';
  static const _appFontFamilyKey = 'app_font_family';
  static const _hapticsKey = 'haptics_enabled';
  static const _referenceTimesKey = 'show_reference_times';
  static const _prayerNotificationsKey = 'prayer_notifications_enabled';
  static String _offsetKey(String prayerKey) => 'prayer_offset_$prayerKey';

  static AppPreferences _read(SharedPreferences prefs) {
    final offsets = <String, int>{};
    for (final key in adjustablePrayerKeys) {
      final stored = prefs.getInt(_offsetKey(key));
      if (stored != null && stored != 0) {
        offsets[key] =
            stored.clamp(prayerOffsetMinLimit, prayerOffsetMaxLimit).toInt();
      }
    }
    return AppPreferences(
      textSize: AppTextSize.fromStorage(prefs.getString(_textSizeKey)),
      appFontFamily:
          AppFontFamily.fromStorage(prefs.getString(_appFontFamilyKey)),
      hapticsEnabled: prefs.getBool(_hapticsKey) ?? true,
      showReferenceTimes: prefs.getBool(_referenceTimesKey) ?? true,
      prayerNotificationsEnabled:
          prefs.getBool(_prayerNotificationsKey) ?? false,
      prayerOffsets: offsets,
    );
  }

  Future<void> setTextSize(AppTextSize size) async {
    state = state.copyWith(textSize: size);
    await _prefs.setString(_textSizeKey, size.storageKey);
  }

  Future<void> setAppFontFamily(AppFontFamily font) async {
    state = state.copyWith(appFontFamily: font);
    await _prefs.setString(_appFontFamilyKey, font.storageKey);
  }

  Future<void> setHapticsEnabled(bool enabled) async {
    state = state.copyWith(hapticsEnabled: enabled);
    await _prefs.setBool(_hapticsKey, enabled);
  }

  Future<void> setShowReferenceTimes(bool show) async {
    state = state.copyWith(showReferenceTimes: show);
    await _prefs.setBool(_referenceTimesKey, show);
  }

  Future<void> setPrayerNotificationsEnabled(bool enabled) async {
    state = state.copyWith(prayerNotificationsEnabled: enabled);
    await _prefs.setBool(_prayerNotificationsKey, enabled);
  }

  Future<void> setPrayerOffset(String prayerKey, int minutes) async {
    if (!adjustablePrayerKeys.contains(prayerKey)) {
      throw ArgumentError('Prayer offset is not adjustable: $prayerKey');
    }
    final clamped =
        minutes.clamp(prayerOffsetMinLimit, prayerOffsetMaxLimit).toInt();
    final next = Map<String, int>.from(state.prayerOffsets);
    if (clamped == 0) {
      next.remove(prayerKey);
    } else {
      next[prayerKey] = clamped;
    }
    state = state.copyWith(prayerOffsets: next);
    await _prefs.setInt(_offsetKey(prayerKey), clamped);
  }

  /// Restores display and prayer-adjustment preferences to their defaults.
  /// Theme, governorate, calculation method and stored data are not touched.
  Future<void> resetDisplayAndPrayerSettings() async {
    state = const AppPreferences();
    await _prefs.remove(_textSizeKey);
    await _prefs.remove(_appFontFamilyKey);
    await _prefs.remove(_hapticsKey);
    await _prefs.remove(_referenceTimesKey);
    await _prefs.remove(_prayerNotificationsKey);
    for (final key in adjustablePrayerKeys) {
      await _prefs.remove(_offsetKey(key));
    }
  }
}

final appPreferencesProvider =
    StateNotifierProvider<AppPreferencesController, AppPreferences>(
  (ref) => AppPreferencesController(ref.watch(sharedPreferencesProvider)),
);
