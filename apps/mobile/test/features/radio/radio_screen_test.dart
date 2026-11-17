import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:khazain_al_huda/features/radio/radio_screen.dart';

void main() {
  testWidgets('does not expose external radio streams or source links',
      (tester) async {
    await tester.pumpWidget(
      const MaterialApp(home: Scaffold(body: RadioScreen())),
    );

    expect(find.text('إذاعة العتبة الحسينية'), findsOneWidget);
    expect(find.textContaining('لا يتوفر حالياً ملف إذاعي محلي'), findsOneWidget);
    expect(find.byType(FilledButton), findsNothing);
    expect(find.byType(OutlinedButton), findsNothing);
    expect(find.textContaining('https://'), findsNothing);
    expect(find.byIcon(Icons.open_in_new), findsNothing);
  });
}
