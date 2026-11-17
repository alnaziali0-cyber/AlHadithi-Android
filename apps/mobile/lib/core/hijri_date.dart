/// Tabular Islamic (civil) date conversion.
///
/// This is an estimate only. It must not be presented as an official Iraqi or
/// Ja'fari moon-sighting date because month starts depend on local authority
/// and sighting decisions.
class ApproximateHijriDate {
  const ApproximateHijriDate({required this.day, required this.month, required this.year});

  final int day;
  final int month;
  final int year;

  static const monthNames = <String>[
    'محرم',
    'صفر',
    'ربيع الأول',
    'ربيع الآخر',
    'جمادى الأولى',
    'جمادى الآخرة',
    'رجب',
    'شعبان',
    'رمضان',
    'شوال',
    'ذو القعدة',
    'ذو الحجة',
  ];

  String get monthYearLabel => '${monthNames[month - 1]} $year هـ';
  String get label => '$day ${monthNames[month - 1]} $year هـ';
  String get labelWithNotice => '$label · تقريبي';
}

ApproximateHijriDate toApproximateHijri(DateTime date) {
  final day = DateTime(date.year, date.month, date.day);
  final a = (14 - day.month) ~/ 12;
  final y = day.year + 4800 - a;
  final m = day.month + 12 * a - 3;
  final julianDay = day.day + (153 * m + 2) ~/ 5 + 365 * y + y ~/ 4 - y ~/ 100 + y ~/ 400 - 32045;

  final year = (30 * (julianDay - 1948439) + 10646) ~/ 10631;
  int islamicToJulianDay(int hijriYear, int hijriMonth, int hijriDay) {
    return hijriDay + ((59 * (hijriMonth - 1) + 1) ~/ 2) +
        (hijriYear - 1) * 354 + (3 + 11 * hijriYear) ~/ 30 + 1948439;
  }

  final month = ((julianDay - (29 + islamicToJulianDay(year, 1, 1))) / 29.5).ceil() + 1;
  final boundedMonth = month.clamp(1, 12).toInt();
  final hijriDay = julianDay - islamicToJulianDay(year, boundedMonth, 1) + 1;
  return ApproximateHijriDate(day: hijriDay, month: boundedMonth, year: year);
}
