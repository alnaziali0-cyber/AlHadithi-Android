import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:khazain_al_huda/core/models.dart';
import 'package:khazain_al_huda/features/governorate/governorate_wheel_picker.dart';

const _governorates = <Governorate>[
  Governorate(id: 'baghdad', nameAr: 'بغداد', nameEn: 'Baghdad', latitude: 33.3152, longitude: 44.3661, timezone: 'Asia/Baghdad', coordinatesQuality: 'approximate'),
  Governorate(id: 'basra', nameAr: 'البصرة', nameEn: 'Basra', latitude: 30.5085, longitude: 47.7804, timezone: 'Asia/Baghdad', coordinatesQuality: 'approximate'),
  Governorate(id: 'babil', nameAr: 'بابل', nameEn: 'Babil', latitude: 32.48, longitude: 44.43, timezone: 'Asia/Baghdad', coordinatesQuality: 'approximate'),
];

void main() {
  testWidgets('new wheel has no implicitly selected governorate', (tester) async {
    Governorate? selected;
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: GovernorateWheelPicker(
          governorates: _governorates,
          initialSelectedIndex: -1,
          reduceMotion: true,
          onScrollingChanged: (_) {},
          onSelected: (value) => selected = value,
        ),
      ),
    ));

    expect(find.text('اختر محافظتك'), findsOneWidget);
    expect(selected, isNull);
  });

  testWidgets('tapping a neighbouring governorate selects it', (tester) async {
    Governorate? selected;
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: GovernorateWheelPicker(
          governorates: _governorates,
          initialSelectedIndex: -1,
          reduceMotion: true,
          onScrollingChanged: (_) {},
          onSelected: (value) => selected = value,
        ),
      ),
    ));

    await tester.tap(find.text('بغداد').first);
    await tester.pumpAndSettle();

    expect(selected?.id, 'baghdad');
  });

  testWidgets('previous neighbour wraps to the final catalogue entry', (tester) async {
    Governorate? selected;
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: GovernorateWheelPicker(
          governorates: _governorates,
          initialSelectedIndex: -1,
          reduceMotion: true,
          onScrollingChanged: (_) {},
          onSelected: (value) => selected = value,
        ),
      ),
    ));

    await tester.tap(find.text('بابل').first);
    await tester.pumpAndSettle();

    expect(selected?.id, 'babil');
  });

  testWidgets('selected item can wrap through the circular wheel', (tester) async {
    final key = GlobalKey<GovernorateWheelPickerState>();
    Governorate? selected;
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: GovernorateWheelPicker(
          key: key,
          governorates: _governorates,
          initialSelectedIndex: 0,
          reduceMotion: true,
          onScrollingChanged: (_) {},
          onSelected: (value) => selected = value,
        ),
      ),
    ));

    await key.currentState!.selectGovernorateIndex(2);
    await tester.pumpAndSettle();
    expect(selected?.id, 'babil');
    await key.currentState!.selectGovernorateIndex(0);
    await tester.pumpAndSettle();
    expect(selected?.id, 'baghdad');
  });

  testWidgets('wheel is shorter and highlights the selected governorate in gold', (tester) async {
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: GovernorateWheelPicker(
          governorates: _governorates,
          initialSelectedIndex: 0,
          reduceMotion: true,
          onScrollingChanged: (_) {},
          onSelected: (_) {},
        ),
      ),
    ));

    expect(tester.getSize(find.byType(GovernorateWheelPicker)).height, 252);
    final selectedText = tester
        .widgetList<Text>(find.text('بغداد'))
        .firstWhere((text) => text.style?.color == const Color(0xFFC8A14A));
    expect(selectedText.style?.color, const Color(0xFFC8A14A));
  });
}
