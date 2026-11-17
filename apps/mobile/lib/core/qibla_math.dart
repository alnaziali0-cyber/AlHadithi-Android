import 'dart:math' as math;

const double kaabaLatitude = 21.422487;
const double kaabaLongitude = 39.826206;

/// Initial great-circle bearing from the supplied point to the Kaaba.
/// Returns a true-north bearing in degrees in the range [0, 360).
double qiblaBearingDegrees({required double latitude, required double longitude}) {
  if (!latitude.isFinite || !longitude.isFinite ||
      latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
    throw ArgumentError('Coordinates must be finite WGS84 latitude/longitude values.');
  }
  final lat1 = _radians(latitude);
  final lat2 = _radians(kaabaLatitude);
  final deltaLongitude = _radians(kaabaLongitude - longitude);
  final y = math.sin(deltaLongitude) * math.cos(lat2);
  final x = math.cos(lat1) * math.sin(lat2) -
      math.sin(lat1) * math.cos(lat2) * math.cos(deltaLongitude);
  return normalizeDegrees(_degrees(math.atan2(y, x)));
}

double normalizeDegrees(double value) => (value % 360 + 360) % 360;

double relativeQiblaDegrees({required double qiblaBearing, required double compassHeading}) {
  final relative = normalizeDegrees(qiblaBearing - compassHeading);
  return relative > 180 ? relative - 360 : relative;
}

double _radians(double degrees) => degrees * math.pi / 180;
double _degrees(double radians) => radians * 180 / math.pi;
