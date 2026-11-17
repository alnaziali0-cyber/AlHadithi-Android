import 'package:flutter_test/flutter_test.dart';
import 'package:khazain_al_huda/core/hijri_date.dart';

void main() {
  test('converts a known civil Hijri date', () {
    final date = toApproximateHijri(DateTime(2024, 3, 11));
    expect(date.year, 1445);
    expect(date.month, 9);
    expect(date.day, 1);
    expect(date.label, '1 رمضان 1445 هـ');
  });

  test('labels the result as approximate for local moon-sighting differences', () {
    expect(toApproximateHijri(DateTime(2026, 11, 14)).labelWithNotice, contains('تقريبي'));
  });
}
