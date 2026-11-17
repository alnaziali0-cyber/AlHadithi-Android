import 'package:flutter_test/flutter_test.dart';
import 'package:khazain_al_huda/core/qibla_math.dart';

void main() {
  test('calculates a south-west Qibla bearing from Baghdad', () {
    final bearing = qiblaBearingDegrees(latitude: 33.3152, longitude: 44.3661);
    expect(bearing, closeTo(200, 1.5));
  });

  test('normalizes headings crossing north', () {
    expect(relativeQiblaDegrees(qiblaBearing: 5, compassHeading: 355), closeTo(10, 0.001));
    expect(relativeQiblaDegrees(qiblaBearing: 355, compassHeading: 5), closeTo(-10, 0.001));
  });

  test('rejects invalid coordinates', () {
    expect(() => qiblaBearingDegrees(latitude: 91, longitude: 0), throwsArgumentError);
  });
}
