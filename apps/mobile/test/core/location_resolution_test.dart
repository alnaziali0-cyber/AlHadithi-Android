import 'package:flutter_test/flutter_test.dart';
import 'package:geocoding/geocoding.dart';
import 'package:khazain_al_huda/core/models.dart';
import 'package:khazain_al_huda/core/services/location_detection_service.dart';

const _governorates = <Governorate>[
  Governorate(id: 'baghdad', nameAr: 'بغداد', nameEn: 'Baghdad', latitude: 33.3152, longitude: 44.3661, timezone: 'Asia/Baghdad', coordinatesQuality: 'approximate', aliases: ['Baghdad']),
  Governorate(id: 'babil', nameAr: 'بابل', nameEn: 'Babil', latitude: 32.48, longitude: 44.43, timezone: 'Asia/Baghdad', coordinatesQuality: 'approximate', aliases: ['Babil', 'Babylon']),
];

void main() {
  const service = LocationDetectionService();

  test('accepts an Iraq placemark and matches the stable governorate id', () {
    final placemark = Placemark(
      isoCountryCode: 'IQ',
      country: 'Iraq',
      administrativeArea: 'Babil Governorate',
    );

    final result = service.resolveIraqPlacemark(placemark, _governorates);
    expect(result?.id, 'babil');
  });

  test('rejects a non-Iraq country even when its admin area looks familiar', () {
    final placemark = Placemark(
      isoCountryCode: 'US',
      country: 'United States',
      administrativeArea: 'Baghdad',
    );

    expect(service.resolveIraqPlacemark(placemark, _governorates), isNull);
  });

  test('rejects coordinates outside the Iraq sanity envelope', () {
    expect(service.isPlausibleIraqCoordinate(33.3, 44.4), isTrue);
    expect(service.isPlausibleIraqCoordinate(40.0, 44.4), isFalse);
    expect(service.isPlausibleIraqCoordinate(double.nan, 44.4), isFalse);
  });
}
