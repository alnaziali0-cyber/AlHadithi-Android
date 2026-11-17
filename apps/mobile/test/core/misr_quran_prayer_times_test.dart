import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:khazain_al_huda/core/misr_quran_prayer_times.dart';
import 'package:timezone/data/latest_all.dart' as tz_data;
import 'package:timezone/timezone.dart' as tz;

const _baghdadLatitude = 33.3152;
const _baghdadLongitude = 44.3661;
const _hijriDate = 'الاثنين06 جمادى الآخرة 1448 هـ';

Map<String, dynamic> _response(
        {String currentServerUtc = '2026-11-16T15:25:01.2247241Z'}) =>
    {
      'date': '2026-11-16T15:25:01.224716+00:00',
      'fajr': '02:01:00',
      'sunrise': '03:34:00',
      'dhuhr': '08:47:00',
      'asr': '11:40:00',
      'sunset': '14:00:00',
      'maghrib': '14:00:00',
      'isha': '15:23:00',
      'hijriDate': _hijriDate,
      'currentServerUTCTime': currentServerUtc,
    };

Dio _dio(_PrayerTimesAdapter adapter) {
  final dio = Dio();
  dio.httpClientAdapter = adapter;
  return dio;
}

MisrQuranPrayerTimesRepository _repository(_PrayerTimesAdapter adapter) =>
    MisrQuranPrayerTimesRepository(
      dio: _dio(adapter),
      location: tz.getLocation('Asia/Baghdad'),
    );

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  tz_data.initializeTimeZones();

  test(
      'parses seven UTC clock fields into Baghdad time and preserves Arabic Hijri date',
      () async {
    final adapter = _PrayerTimesAdapter(jsonEncode(_response()));
    final result = await _repository(adapter).fetch(
      latitude: _baghdadLatitude,
      longitude: _baghdadLongitude,
    );

    expect(result.date, '2026-11-16');
    expect(result.displayTimes, {
      'fajr': '05:01',
      'sunrise': '06:34',
      'dhuhr': '11:47',
      'asr': '14:40',
      'sunset': '17:00',
      'maghrib': '17:00',
      'isha': '18:23',
    });
    expect(result.hijriDate, _hijriDate);
    expect(result.source.name, 'misrQuranApi');
    expect(result.sourceLabel, 'خدمة MisrQuran (غير معتمدة فقهياً)');
  });

  test(
      'uses the Baghdad service date when current UTC time crosses local midnight',
      () async {
    final adapter = _PrayerTimesAdapter(
      jsonEncode(_response(currentServerUtc: '2026-11-16T21:30:00Z')),
    );
    final result = await _repository(adapter).fetch(
      latitude: _baghdadLatitude,
      longitude: _baghdadLongitude,
    );

    expect(result.date, '2026-11-17');
    expect(result.displayTimes['fajr'], '05:01');
  });

  test(
      'places a late UTC clock on the matching Baghdad service day after midnight rollover',
      () async {
    final response = _response()..['isha'] = '21:30:00';
    final result =
        await _repository(_PrayerTimesAdapter(jsonEncode(response))).fetch(
      latitude: _baghdadLatitude,
      longitude: _baghdadLongitude,
    );

    expect(result.displayTimes['isha'], '00:30');
    expect(result.date, '2026-11-16');
  });

  test(
      'rejects missing, null, and malformed prayer clocks instead of returning partial data',
      () async {
    for (final mutate in <void Function(Map<String, dynamic>)>[
      (json) => json.remove('asr'),
      (json) => json['fajr'] = null,
      (json) => json['isha'] = '25:00:00',
    ]) {
      final json = _response();
      mutate(json);
      await expectLater(
        _repository(_PrayerTimesAdapter(jsonEncode(json))).fetch(
          latitude: _baghdadLatitude,
          longitude: _baghdadLongitude,
        ),
        throwsA(isA<FormatException>()),
      );
    }
  });

  test('rejects an HTTP error response', () async {
    final repository = _repository(
      _PrayerTimesAdapter('{"message":"unavailable"}', statusCode: 503),
    );

    await expectLater(
      repository.fetch(
          latitude: _baghdadLatitude, longitude: _baghdadLongitude),
      throwsA(isA<DioException>()),
    );
  });

  test('rejects malformed JSON', () async {
    final repository = _repository(_PrayerTimesAdapter('not-json'));

    await expectLater(
      repository.fetch(
          latitude: _baghdadLatitude, longitude: _baghdadLongitude),
      throwsA(anyOf(isA<DioException>(), isA<FormatException>())),
    );
  });

  test('sends only the approved query fields and never adds the app device key',
      () async {
    final adapter = _PrayerTimesAdapter(jsonEncode(_response()));
    final result = await _repository(adapter).fetch(
      latitude: _baghdadLatitude,
      longitude: _baghdadLongitude,
    );

    expect(adapter.request!.method, 'GET');
    expect(adapter.request!.path, '/api/PrayerTimes/GetTodaysTimes');
    expect(adapter.request!.queryParameters, {
      'lat': _baghdadLatitude,
      'lng': _baghdadLongitude,
      'dayLightSaving': false,
    });
    expect(adapter.request!.headers.keys.map((key) => key.toLowerCase()),
        isNot(contains('x-device-key')));
    expect(result.toJson().keys, isNot(contains('latitude')));
    expect(result.toJson().keys, isNot(contains('longitude')));
  });

  test('rejects non-finite or out-of-range coordinates before making a request',
      () async {
    final adapter = _PrayerTimesAdapter(jsonEncode(_response()));
    final repository = _repository(adapter);

    await expectLater(
      repository.fetch(latitude: double.nan, longitude: _baghdadLongitude),
      throwsArgumentError,
    );
    expect(adapter.request, isNull);
  });
}

class _PrayerTimesAdapter implements HttpClientAdapter {
  _PrayerTimesAdapter(this.body, {this.statusCode = 200});

  final String body;
  final int statusCode;
  RequestOptions? request;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    request = options;
    return ResponseBody.fromString(
      body,
      statusCode,
      headers: {
        Headers.contentTypeHeader: ['application/json; charset=utf-8']
      },
    );
  }

  @override
  void close({bool force = false}) {}
}
