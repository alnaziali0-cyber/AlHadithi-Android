import 'dart:convert';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test('bundled Iraqi governorate catalogue has 19 stable unique ids', () async {
    final raw = await rootBundle.loadString('assets/data/iraq_governorates.json');
    final decoded = jsonDecode(raw);
    expect(decoded, isA<List<dynamic>>());
    final entries = decoded as List<dynamic>;
    final ids = entries.map((entry) => (entry as Map<String, dynamic>)['id'] as String).toList();
    expect(entries.length, 19);
    expect(ids.toSet().length, 19);
    expect(ids, containsAll(<String>[
      'baghdad', 'basra', 'nineveh', 'erbil', 'sulaymaniyah', 'duhok',
      'halabja', 'kirkuk', 'diyala', 'saladin', 'anbar', 'babil',
      'karbala', 'najaf', 'qadisiyyah', 'muthanna', 'dhi_qar', 'maysan', 'wasit',
    ]));
    for (final entry in entries.cast<Map<String, dynamic>>()) {
      expect(entry['name_ar'], isA<String>());
      expect(entry['name_en'], isA<String>());
      expect(entry['aliases'], isA<List<dynamic>>());
      expect(entry['latitude'], isA<num>());
      expect(entry['longitude'], isA<num>());
    }
  });
}
