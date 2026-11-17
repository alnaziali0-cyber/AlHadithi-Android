import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'router.dart';
import 'theme.dart';
import '../core/app_preferences.dart';
import '../core/prayer_notification_service.dart';
import '../core/prayer_notification_sync.dart';
import '../core/providers.dart';

class KhazainApp extends ConsumerStatefulWidget {
  const KhazainApp({super.key});

  @override
  ConsumerState<KhazainApp> createState() => _KhazainAppState();
}

class _KhazainAppState extends ConsumerState<KhazainApp> with WidgetsBindingObserver {
  bool _sameOffsets(Map<String, int> a, Map<String, int> b) {
    if (a.length != b.length) return false;
    for (final entry in a.entries) {
      if (b[entry.key] != entry.value) return false;
    }
    return true;
  }
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      // Refresh the rolling notification window whenever the user returns.
      ref.invalidate(prayerTimesProvider);
    }
  }

  Future<void> _syncNotifications() async {
    try {
      await syncPrayerNotifications(ref);
    } catch (error) {
      debugPrint('Prayer notification schedule refresh failed: $error');
    }
  }

  @override
  Widget build(BuildContext context) {
    ref.listen(prayerTimesProvider, (previous, next) {
      if (!ref.read(appPreferencesProvider).prayerNotificationsEnabled) return;
      next.when(
        data: (result) {
          if (result != null) {
            unawaited(_syncNotifications());
          } else {
            unawaited(PrayerNotificationService.instance.cancelPrayerAlerts());
          }
        },
        loading: () {},
        error: (error, stack) {
          debugPrint('Prayer times unavailable; clearing potentially outdated alerts: $error');
          unawaited(PrayerNotificationService.instance.cancelPrayerAlerts());
        },
      );
    });

    ref.listen<AppPreferences>(appPreferencesProvider, (previous, next) {
      final wasEnabled = previous?.prayerNotificationsEnabled ?? false;
      if (wasEnabled != next.prayerNotificationsEnabled) {
        if (!next.prayerNotificationsEnabled) {
          unawaited(PrayerNotificationService.instance.cancelPrayerAlerts());
        }
        return;
      }
      if (next.prayerNotificationsEnabled && previous != null && !_sameOffsets(previous.prayerOffsets, next.prayerOffsets)) {
        unawaited(_syncNotifications());
      }
    });

    final themeMode = ref.watch(themeModeProvider);
    final textSize = ref.watch(appPreferencesProvider.select((prefs) => prefs.textSize));
    final appFontFamily = ref.watch(appPreferencesProvider.select((prefs) => prefs.appFontFamily.themeFamily));
    final router = ref.watch(routerProvider);
    return MaterialApp.router(
      title: 'خَزَائِنُ الهُدَى',
      debugShowCheckedModeBanner: false,
      theme: lightTheme(fontFamily: appFontFamily),
      darkTheme: darkTheme(fontFamily: appFontFamily),
      themeMode: themeMode,
      builder: (context, child) {
        // Preserve OS accessibility scaling while applying the user's text-size choice.
        final mediaQuery = MediaQuery.of(context);
        final systemScale = mediaQuery.textScaler.scale(1.0);
        final scale = (systemScale * textSize.factor).clamp(0.8, 2.4).toDouble();
        final isDark = themeMode == ThemeMode.dark ||
            (themeMode == ThemeMode.system && mediaQuery.platformBrightness == Brightness.dark);
        return MediaQuery(
          data: mediaQuery.copyWith(textScaler: TextScaler.linear(scale)),
          child: Stack(children: [
            Positioned.fill(child: IgnorePointer(child: Image.asset(
              'assets/branding/mosque_background.jpg',
              fit: BoxFit.cover,
              alignment: Alignment.topCenter,
              errorBuilder: (_, __, ___) => const SizedBox.shrink(),
            ))),
            Positioned.fill(child: IgnorePointer(child: ColoredBox(
              color: isDark ? Colors.black.withOpacity(.40) : Colors.white.withOpacity(.20),
            ))),
            Positioned.fill(child: child ?? const SizedBox.shrink()),
          ]),
        );
      },
      routerConfig: router,
      locale: const Locale('ar'),
      supportedLocales: const [Locale('ar'), Locale('en')],
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
    );
  }
}
