import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:khazain_al_huda/core/providers.dart';
import 'package:khazain_al_huda/features/tasbih/tasbih_screen.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late SharedPreferences preferences;

  setUp(() async {
    SharedPreferences.setMockInitialValues({'haptics_enabled': false});
    preferences = await SharedPreferences.getInstance();
  });

  Future<void> pumpTasbih(WidgetTester tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [sharedPreferencesProvider.overrideWithValue(preferences)],
        child: const MaterialApp(home: Scaffold(body: TasbihScreen())),
      ),
    );
    await tester.pump();
  }

  Future<void> tapCounter(WidgetTester tester, int times) async {
    for (var index = 0; index < times; index++) {
      await tester.tap(find.text('اضغط للتسبيح'));
      await tester.pump(const Duration(milliseconds: 1));
    }
  }

  testWidgets('Fatimi order counts 34/33/33 and automatically resets after 100',
      (tester) async {
    await pumpTasbih(tester);

    expect(find.text('التسبيح الفاطمي'), findsOneWidget);
    expect(find.text('اللَّهُ أَكْبَرُ'), findsWidgets);
    expect(find.text('0 / 100'), findsOneWidget);

    await tapCounter(tester, 34);
    expect(find.text('34 / 100'), findsOneWidget);
    expect(find.text('الذكر 2 من 3'), findsOneWidget);
    expect(find.text('المتبقي في هذا الذكر: 33'), findsOneWidget);
    expect(find.text('الْحَمْدُ لِلَّهِ'), findsWidgets);

    await tapCounter(tester, 33);
    expect(find.text('67 / 100'), findsOneWidget);
    expect(find.text('الذكر 3 من 3'), findsOneWidget);
    expect(find.text('سُبْحَانَ اللَّهِ'), findsWidgets);

    await tapCounter(tester, 33);
    await tester.pumpAndSettle(
      const Duration(milliseconds: 50),
      EnginePhase.sendSemanticsUpdate,
      const Duration(seconds: 5),
    );
    expect(find.text('0 / 100'), findsOneWidget);
    expect(find.text('الذكر 1 من 3'), findsOneWidget);
    expect(preferences.getInt('tasbih_fatimi_v2_step'), 0);
    expect(preferences.getInt('tasbih_fatimi_v2_count'), 0);
    expect(preferences.getInt('tasbih_fatimi_sequence_version'), 2);
  });

  testWidgets(
      'does not relabel old-order progress and keeps the old local values intact',
      (tester) async {
    SharedPreferences.setMockInitialValues({
      'haptics_enabled': false,
      'tasbih_normal_step': 1,
      'tasbih_normal_count': 12,
      'tasbih_infinite_count': 4,
    });
    preferences = await SharedPreferences.getInstance();

    await pumpTasbih(tester);
    expect(find.text('بدأ عدّ جديد للترتيب الفاطمي 34/33/33.'), findsOneWidget);
    expect(find.text('0 / 100'), findsOneWidget);

    await tester.tap(find.text('اضغط للتسبيح'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 1));
    expect(preferences.getInt('tasbih_fatimi_v2_step'), 0);
    expect(preferences.getInt('tasbih_fatimi_v2_count'), 1);
    expect(preferences.getInt('tasbih_normal_step'), 1);
    expect(preferences.getInt('tasbih_normal_count'), 12);
    expect(preferences.getInt('tasbih_infinite_count'), 4);
  });
}
