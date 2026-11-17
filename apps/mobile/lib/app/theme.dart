import 'package:flutter/material.dart';

class KhazainPalette {
  static const green = Color(0xFF4CAF50);
  static const greenSoft = Color(0xFF8BD18E);
  static const gold = Color(0xFFC9983E);
  static const dark = Color(0xFF101512);
  static const darkSurface = Color(0xE61A211D);
  static const darkElevated = Color(0xEE222B25);
  static const lightSurface = Color(0xEFFFFFFF);
  static const lightInk = Color(0xFF1D2A22);
}

ThemeData lightTheme({String fontFamily = 'NotoNaskhArabicUI'}) {
  final scheme = ColorScheme.fromSeed(
    seedColor: KhazainPalette.green,
    brightness: Brightness.light,
    primary: KhazainPalette.green,
    secondary: const Color(0xFF477C4A),
    surface: Colors.white,
  );
  return ThemeData(
    useMaterial3: true,
    fontFamily: fontFamily,
    colorScheme: scheme,
    scaffoldBackgroundColor: Colors.transparent,
    dividerColor: const Color(0xFF777F78).withOpacity(.22),
    cardTheme: CardThemeData(
      color: KhazainPalette.lightSurface,
      elevation: 0,
      margin: EdgeInsets.zero,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(22),
        side: const BorderSide(color: Color(0x55FFFFFF)),
      ),
    ),
    appBarTheme: const AppBarTheme(
      backgroundColor: Colors.transparent,
      foregroundColor: KhazainPalette.lightInk,
      elevation: 0,
      centerTitle: false,
    ),
    navigationBarTheme: NavigationBarThemeData(
      backgroundColor: const Color(0xF2FFFFFF),
      indicatorColor: KhazainPalette.green.withOpacity(.12),
      labelTextStyle: const WidgetStatePropertyAll(
        TextStyle(fontSize: 11, fontWeight: FontWeight.w700),
      ),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: Colors.white.withOpacity(.88),
      border: OutlineInputBorder(borderRadius: BorderRadius.circular(16)),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: const BorderSide(color: Color(0x55777777)),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: const BorderSide(color: KhazainPalette.green, width: 1.5),
      ),
    ),
  );
}

ThemeData darkTheme({String fontFamily = 'NotoNaskhArabicUI'}) {
  final scheme = ColorScheme.fromSeed(
    seedColor: KhazainPalette.green,
    brightness: Brightness.dark,
    primary: KhazainPalette.green,
    secondary: KhazainPalette.greenSoft,
    surface: KhazainPalette.darkSurface,
  );
  return ThemeData(
    useMaterial3: true,
    fontFamily: fontFamily,
    colorScheme: scheme,
    scaffoldBackgroundColor: Colors.transparent,
    dividerColor: const Color(0xFF6B746D).withOpacity(.18),
    cardTheme: CardThemeData(
      color: KhazainPalette.darkSurface,
      elevation: 0,
      margin: EdgeInsets.zero,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(22),
        side: const BorderSide(color: Color(0x222FFFFFFF)),
      ),
    ),
    appBarTheme: const AppBarTheme(
      backgroundColor: Colors.transparent,
      foregroundColor: Color(0xFFF0F2EF),
      elevation: 0,
      centerTitle: false,
    ),
    navigationBarTheme: NavigationBarThemeData(
      backgroundColor: const Color(0xF21A211D),
      indicatorColor: KhazainPalette.green.withOpacity(.17),
      labelTextStyle: const WidgetStatePropertyAll(
        TextStyle(fontSize: 11, fontWeight: FontWeight.w700),
      ),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: KhazainPalette.darkElevated,
      border: OutlineInputBorder(borderRadius: BorderRadius.circular(16)),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: const BorderSide(color: Color(0x335E6A61)),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: const BorderSide(color: KhazainPalette.green, width: 1.5),
      ),
    ),
  );
}
