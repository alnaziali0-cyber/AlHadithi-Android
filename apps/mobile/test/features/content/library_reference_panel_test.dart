import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:khazain_al_huda/features/content/library_reference_panel.dart';

void main() {
  testWidgets('shows the archive as an internal rights note, not as a link',
      (tester) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: Scaffold(
          body: SingleChildScrollView(child: LibraryReferencePanel()),
        ),
      ),
    );

    expect(find.textContaining('4,095 كتاباً'), findsOneWidget);
    expect(find.text('الحديث'), findsOneWidget);
    expect(find.text('موسوعات إسلامية'), findsOneWidget);
    expect(find.textContaining('لم يُنزّل ولم يُضمّن'), findsOneWidget);
    expect(find.textContaining('لا يوجد حالياً ملف كتاب محلي'), findsNWidgets(2));
    expect(find.byType(OutlinedButton), findsNothing);
    expect(find.byIcon(Icons.open_in_new), findsNothing);
    expect(find.textContaining('https://'), findsNothing);
  });
}
