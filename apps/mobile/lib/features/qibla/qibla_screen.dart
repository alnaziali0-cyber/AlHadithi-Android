import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_compass/flutter_compass.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app/theme.dart';
import '../../core/location_coordinates_store.dart';
import '../../core/governorate_selection_sheet.dart';
import '../../core/models.dart';
import '../../core/providers.dart';
import '../../core/qibla_math.dart';

class QiblaLocation {
  const QiblaLocation({
    required this.latitude,
    required this.longitude,
    required this.description,
    required this.isApproximate,
  });

  final double latitude;
  final double longitude;
  final String description;
  final bool isApproximate;
}

final qiblaLocationProvider = FutureProvider<QiblaLocation?>((ref) async {
  final prefs = ref.watch(sharedPreferencesProvider);
  final selectedGovernorateId = ref.watch(selectedGovernorateProvider);
  final selectedCityId = ref.watch(selectedPrayerCityProvider);
  final coordinatesStore = ref.watch(locationCoordinatesStoreProvider);
  final citiesFuture =
      selectedCityId == null ? null : ref.watch(prayerCitiesProvider.future);
  final governoratesFuture = selectedGovernorateId == null
      ? null
      : ref.watch(governoratesProvider.future);

  if (prefs.getString('selected_location_source') == 'gps') {
    try {
      final StoredLocationCoordinates? location = await coordinatesStore.read();
      if (location != null && location.source == 'gps') {
        return QiblaLocation(
          latitude: location.latitude,
          longitude: location.longitude,
          description:
              prefs.getString('selected_governorate_name') ?? 'الموقع المحفوظ',
          isApproximate: false,
        );
      }
    } catch (_) {
      // Fall back to the selected governorate centre; never invent GPS coordinates.
    }
  }

  if (selectedCityId != null) {
    try {
      final cities = await citiesFuture!;
      for (final city in cities) {
        if (city.cityId == selectedCityId) {
          return QiblaLocation(
            latitude: city.latitude,
            longitude: city.longitude,
            description: '${city.cityNameAr} — ${city.governorateNameAr}',
            isApproximate: true,
          );
        }
      }
    } catch (_) {
      // Use governorate fallback below if the city list is unavailable.
    }
  }

  if (selectedGovernorateId == null) return null;
  try {
    final governorates = await governoratesFuture!;
    for (final governorate in governorates) {
      if (governorate.id == selectedGovernorateId) {
        return QiblaLocation(
          latitude: governorate.latitude,
          longitude: governorate.longitude,
          description: governorate.nameAr,
          isApproximate: true,
        );
      }
    }
  } catch (_) {
    return null;
  }
  return null;
});

class QiblaScreen extends ConsumerWidget {
  const QiblaScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final location = ref.watch(qiblaLocationProvider);

    return CustomScrollView(
      slivers: [
        SliverPadding(
          padding: const EdgeInsets.fromLTRB(18, 18, 18, 30),
          sliver: SliverList(
            delegate: SliverChildListDelegate([
              Text('اتجاه القبلة',
                  style: theme.textTheme.headlineSmall
                      ?.copyWith(fontWeight: FontWeight.w900)),
              const SizedBox(height: 6),
              Text('بوصلة مباشرة نحو الكعبة المشرفة',
                  style: theme.textTheme.bodyMedium),
              const SizedBox(height: 18),
              location.when(
                loading: () => const Center(
                    child: Padding(
                        padding: EdgeInsets.all(35),
                        child: CircularProgressIndicator())),
                error: (_, __) => _LocationMessage(
                    onRetry: () => ref.invalidate(qiblaLocationProvider)),
                data: (value) {
                  if (value == null) {
                    return _NoLocationCard(onChooseLocation: () async {
                      await showGovernorateSelectionSheet(context);
                      ref.invalidate(qiblaLocationProvider);
                    });
                  }
                  final bearing = qiblaBearingDegrees(
                      latitude: value.latitude, longitude: value.longitude);
                  return Column(
                    children: [
                      _CompassPanel(qiblaBearing: bearing),
                      const SizedBox(height: 16),
                      Card(
                        child: Padding(
                          padding: const EdgeInsets.all(16),
                          child: Column(
                              crossAxisAlignment: CrossAxisAlignment.stretch,
                              children: [
                                Row(children: [
                                  Icon(Icons.location_on_outlined,
                                      color: theme.colorScheme.secondary),
                                  const SizedBox(width: 8),
                                  Expanded(
                                      child: Text(value.description,
                                          style: const TextStyle(
                                              fontWeight: FontWeight.w800))),
                                  IconButton(
                                      onPressed: () =>
                                          ref.invalidate(qiblaLocationProvider),
                                      tooltip: 'تحديث',
                                      icon: const Icon(Icons.refresh_rounded)),
                                ]),
                                Text(
                                    value.isApproximate
                                        ? 'الاتجاه محسوب من إحداثيات تقريبية لمركز المدينة أو المحافظة.'
                                        : 'الاتجاه محسوب من إحداثيات GPS المحفوظة بموافقتك.',
                                    style: theme.textTheme.bodySmall
                                        ?.copyWith(height: 1.5)),
                                const SizedBox(height: 8),
                                Text(
                                    'زاوية القبلة من الشمال الحقيقي: ${bearing.toStringAsFixed(1)}°',
                                    style: theme.textTheme.titleSmall?.copyWith(
                                        fontWeight: FontWeight.w800,
                                        color: theme.colorScheme.primary)),
                              ]),
                        ),
                      ),
                      const SizedBox(height: 12),
                      const _CalibrationNotice(),
                    ],
                  );
                },
              ),
            ]),
          ),
        ),
      ],
    );
  }
}

class _CompassPanel extends StatelessWidget {
  const _CompassPanel({required this.qiblaBearing});
  final double qiblaBearing;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final gold = theme.colorScheme.secondary;
    return Card(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(18, 18, 18, 22),
        child: Column(children: [
          Container(
            width: 76,
            height: 76,
            decoration: BoxDecoration(
                color: gold.withOpacity(.12), shape: BoxShape.circle),
            child: Icon(Icons.explore_outlined, color: gold, size: 38),
          ),
          const SizedBox(height: 12),
          const Text('حرّك الهاتف ببطء حتى تستقر البوصلة',
              textAlign: TextAlign.center),
          const SizedBox(height: 18),
          StreamBuilder<CompassEvent>(
            stream: FlutterCompass.events,
            builder: (context, snapshot) {
              final heading = snapshot.data?.heading;
              if (snapshot.hasError || heading == null || !heading.isFinite) {
                return _CompassUnavailable(bearing: qiblaBearing);
              }
              final relative = relativeQiblaDegrees(
                  qiblaBearing: qiblaBearing, compassHeading: heading);
              final aligned = relative.abs() <= 5;
              return Column(children: [
                SizedBox(
                  width: 282,
                  height: 282,
                  child: Stack(alignment: Alignment.center, children: [
                    Container(
                      width: 272,
                      height: 272,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        gradient: RadialGradient(colors: [
                          theme.colorScheme.surface,
                          theme.colorScheme.surfaceContainerHighest
                              .withOpacity(.5)
                        ]),
                        border:
                            Border.all(color: gold.withOpacity(.7), width: 2),
                        boxShadow: [
                          BoxShadow(
                              color: theme.colorScheme.primary.withOpacity(.06),
                              blurRadius: 20,
                              spreadRadius: 3)
                        ],
                      ),
                    ),
                    const Positioned(
                        top: 16,
                        child: Text('شمال',
                            style: TextStyle(fontWeight: FontWeight.w900))),
                    const Positioned(
                        bottom: 16,
                        child: Text('جنوب',
                            style: TextStyle(fontWeight: FontWeight.w700))),
                    const Positioned(
                        left: 22,
                        child: Text('غرب',
                            style: TextStyle(fontWeight: FontWeight.w700))),
                    const Positioned(
                        right: 22,
                        child: Text('شرق',
                            style: TextStyle(fontWeight: FontWeight.w700))),
                    for (var i = 0; i < 36; i++)
                      Transform.rotate(
                        angle: i * math.pi / 18,
                        child: Align(
                          alignment: Alignment.topCenter,
                          child: Container(
                              margin: const EdgeInsets.only(top: 5),
                              width: i % 3 == 0 ? 2.2 : 1,
                              height: i % 3 == 0 ? 13 : 7,
                              color:
                                  theme.colorScheme.primary.withOpacity(.38)),
                        ),
                      ),
                    Transform.rotate(
                      angle: relative * math.pi / 180,
                      child: Column(mainAxisSize: MainAxisSize.min, children: [
                        Icon(Icons.navigation_rounded,
                            size: 108,
                            color: aligned ? KhazainPalette.green : gold),
                        const SizedBox(height: 25),
                      ]),
                    ),
                    Container(
                        width: 16,
                        height: 16,
                        decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            color: theme.colorScheme.primary,
                            border: Border.all(
                                color: theme.colorScheme.surface, width: 3))),
                  ]),
                ),
                const SizedBox(height: 18),
                Text(
                    aligned
                        ? 'أنت باتجاه القبلة تقريباً'
                        : '${relative.abs().toStringAsFixed(0)}° ${relative >= 0 ? 'إلى اليمين' : 'إلى اليسار'}',
                    style: theme.textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w900,
                        color: aligned
                            ? KhazainPalette.green
                            : theme.colorScheme.primary)),
                const SizedBox(height: 6),
                Text('اتجاه الهاتف: ${heading.toStringAsFixed(0)}°',
                    style: theme.textTheme.bodySmall),
              ]);
            },
          ),
        ]),
      ),
    );
  }
}

class _CompassUnavailable extends StatelessWidget {
  const _CompassUnavailable({required this.bearing});
  final double bearing;

  @override
  Widget build(BuildContext context) => Column(children: [
        Container(
            width: 184,
            height: 184,
            decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(
                    color: Theme.of(context).colorScheme.secondary, width: 2)),
            child: const Icon(Icons.explore_off_outlined, size: 74)),
        const SizedBox(height: 12),
        const Text('مستشعر البوصلة غير متاح أو لم يرسل قراءة بعد.',
            textAlign: TextAlign.center),
        const SizedBox(height: 8),
        Text('زاوية القبلة الحسابية: ${bearing.toStringAsFixed(1)}°',
            style: const TextStyle(fontWeight: FontWeight.w800)),
      ]);
}

class _NoLocationCard extends StatelessWidget {
  const _NoLocationCard({required this.onChooseLocation});
  final VoidCallback onChooseLocation;
  @override
  Widget build(BuildContext context) => Card(
      child: Padding(
          padding: const EdgeInsets.all(22),
          child: Column(children: [
            const Icon(Icons.location_off_outlined, size: 42),
            const SizedBox(height: 12),
            const Text('حدد المحافظة أو المدينة أولاً لاحتساب اتجاه القبلة.',
                textAlign: TextAlign.center),
            const SizedBox(height: 12),
            FilledButton.icon(
                onPressed: onChooseLocation,
                icon: const Icon(Icons.location_on_outlined),
                label: const Text('اختيار المحافظة أو المدينة')),
          ])));
}

class _LocationMessage extends StatelessWidget {
  const _LocationMessage({required this.onRetry});
  final VoidCallback onRetry;
  @override
  Widget build(BuildContext context) => Card(
      child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(children: [
            const Text('تعذر تحميل موقعك لحساب القبلة.'),
            TextButton(onPressed: onRetry, child: const Text('إعادة المحاولة'))
          ])));
}

class _CalibrationNotice extends StatelessWidget {
  const _CalibrationNotice();
  @override
  Widget build(BuildContext context) => Text(
      'لتحسين القراءة، أبعد الهاتف عن المعادن والمغناطيسات وحرّكه على شكل رقم 8 عند اضطرابها. الحساب الجغرافي محسوب من الشمال الحقيقي، بينما مستشعر الهاتف قد يعرض الشمال المغناطيسي؛ لم يُطبّق تصحيح الانحراف المغناطيسي في هذا الإصدار، لذلك يبقى السهم إرشادياً ويجب التحقق ميدانياً.',
      style: Theme.of(context).textTheme.bodySmall?.copyWith(height: 1.6));
}
