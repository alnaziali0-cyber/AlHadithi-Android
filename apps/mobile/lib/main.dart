import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:intl/date_symbol_data_local.dart';

import 'app/khazain_app.dart';
import 'core/providers.dart';
import 'core/prayer_notification_service.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initializeDateFormatting('ar');
  final preferences = await SharedPreferences.getInstance();
  try {
    await PrayerNotificationService.instance.initialize();
  } catch (error) {
    debugPrint('Prayer notification initialization failed: $error');
  }
  runApp(ProviderScope(
    overrides: [sharedPreferencesProvider.overrideWithValue(preferences)],
    child: const KhazainApp(),
  ));
}
