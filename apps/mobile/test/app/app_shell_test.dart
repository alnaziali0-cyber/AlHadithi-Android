import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:khazain_al_huda/app/app_shell.dart';

void main() {
  testWidgets('glass tabs follow RTL and navigate only to implemented screens',
      (tester) async {
    final router = GoRouter(
      initialLocation: '/quran/3',
      routes: [
        ShellRoute(
          builder: (context, state, child) =>
              AppShell(location: state.uri.path, child: child),
          routes: [
            GoRoute(
              path: '/',
              builder: (context, state) => const Text('route:home'),
            ),
            GoRoute(
              path: '/quran',
              builder: (context, state) => const Text('route:quran'),
              routes: [
                GoRoute(
                  path: ':surahNumber',
                  builder: (context, state) => Text(
                      'route:quran:${state.pathParameters['surahNumber']}'),
                ),
              ],
            ),
            GoRoute(
              path: '/qibla',
              builder: (context, state) => const Text('route:qibla'),
            ),
            GoRoute(
              path: '/tasbih',
              builder: (context, state) => const Text('route:tasbih'),
            ),
            GoRoute(
              path: '/settings',
              builder: (context, state) => const Text('route:settings'),
            ),
          ],
        ),
      ],
    );
    addTearDown(router.dispose);

    await tester.pumpWidget(
      MaterialApp.router(
        routerConfig: router,
        locale: const Locale('ar'),
        supportedLocales: const [Locale('ar')],
        localizationsDelegates: GlobalMaterialLocalizations.delegates,
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('route:quran:3'), findsOneWidget);
    expect(find.text('المواقيت'), findsNothing);
    await tester.binding.handlePopRoute();
    await tester.pumpAndSettle();

    expect(find.text('route:quran'), findsOneWidget);
    expect(find.text('المواقيت'), findsOneWidget);
    expect(find.text('المصحف'), findsOneWidget);
    expect(find.text('القبلة'), findsOneWidget);
    expect(find.text('المسبحة'), findsOneWidget);
    expect(find.text('الإعدادات'), findsOneWidget);
    expect(find.text('صدقة'), findsNothing);
    expect(
      tester.getSemantics(find.byKey(const ValueKey('glass-nav-tab-/quran'))),
      matchesSemantics(
        label: 'المصحف',
        isButton: true,
        hasSelectedState: true,
        isSelected: true,
        hasTapAction: true,
      ),
    );

    await tester.tap(find.byKey(const ValueKey('glass-nav-tab-/qibla')));
    await tester.pumpAndSettle();

    expect(find.text('route:qibla'), findsOneWidget);
    expect(
      tester.getSemantics(find.byKey(const ValueKey('glass-nav-tab-/qibla'))),
      matchesSemantics(
        label: 'القبلة',
        isButton: true,
        hasSelectedState: true,
        isSelected: true,
        hasTapAction: true,
      ),
    );
  });
}
