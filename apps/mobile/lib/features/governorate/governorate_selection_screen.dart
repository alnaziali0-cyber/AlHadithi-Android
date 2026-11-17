import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:geolocator/geolocator.dart';
import 'package:go_router/go_router.dart';

import '../../core/models.dart';
import '../../core/providers.dart';
import '../../core/services/location_detection_service.dart';
import 'governorate_wheel_picker.dart';

class GovernorateSelectionScreen extends ConsumerStatefulWidget {
  const GovernorateSelectionScreen({super.key, this.isChangeMode = false, this.isEmbedded = false, this.requiredFlow = false});

  final bool isChangeMode;
  final bool isEmbedded;
  final bool requiredFlow;

  @override
  ConsumerState<GovernorateSelectionScreen> createState() =>
      _GovernorateSelectionScreenState();
}

class _GovernorateSelectionScreenState
    extends ConsumerState<GovernorateSelectionScreen> {
  final _wheelKey = GlobalKey<GovernorateWheelPickerState>();
  final _locationService = const LocationDetectionService();

  String? _selectedGovernorateId;
  String? _pendingGpsGovernorateId;
  double? _gpsLatitude;
  double? _gpsLongitude;
  bool _isGpsSuggestion = false;
  bool _isDetecting = false;
  bool _isWheelScrolling = false;
  String? _statusMessage;
  bool _statusIsError = false;
  bool _openAppSettingsAction = false;
  bool _openLocationSettingsAction = false;
  bool _allowRequiredFlowExit = false;

  @override
  void initState() {
    super.initState();
    final preferences = ref.read(sharedPreferencesProvider);
    _selectedGovernorateId = preferences.getString('selected_governorate');
    if (preferences.getString('selected_location_source') == 'gps') {
      unawaited(_restoreSavedGpsCoordinates());
    }
  }

  Future<void> _restoreSavedGpsCoordinates() async {
    final expectedGovernorateId = _selectedGovernorateId;
    try {
      final stored = await ref.read(locationCoordinatesStoreProvider).read();
      if (!mounted || stored == null || stored.source != 'gps' ||
          expectedGovernorateId == null || _selectedGovernorateId != expectedGovernorateId) return;
      setState(() {
        _gpsLatitude = stored.latitude;
        _gpsLongitude = stored.longitude;
        _isGpsSuggestion = true;
        _pendingGpsGovernorateId = _selectedGovernorateId;
      });
    } catch (_) {
      // If secure storage is unavailable, the saved governorate remains usable manually.
    }
  }

  void _onGovernorateSelected(Governorate governorate) {
    if (!mounted) return;
    setState(() {
      _selectedGovernorateId = governorate.id;
      if (_pendingGpsGovernorateId != governorate.id) {
        _isGpsSuggestion = false;
        _gpsLatitude = null;
        _gpsLongitude = null;
        _pendingGpsGovernorateId = null;
      }
      _statusMessage = null;
      _statusIsError = false;
      _openAppSettingsAction = false;
      _openLocationSettingsAction = false;
    });
  }

  Future<void> _detectLocation(List<Governorate> governorates) async {
    if (_isDetecting) return;
    setState(() {
      _isDetecting = true;
      _statusMessage = 'جارٍ التحقق من إذن الموقع وتحديد المحافظة…';
      _statusIsError = false;
    });

    try {
      final result = await _locationService.detect(governorates);
      if (!mounted) return;

      if (result.isSuccess) {
        final governorate = result.governorate!;
        final index = governorates.indexWhere((item) => item.id == governorate.id);
        if (index >= 0) {
          _pendingGpsGovernorateId = governorate.id;
          final wheel = _wheelKey.currentState;
          final settledGovernorate = wheel == null
              ? null
              : await wheel.selectGovernorateIndex(index);
          if (!mounted) return;
          if (settledGovernorate?.id != governorate.id) {
            setState(() {
              _isGpsSuggestion = false;
              _gpsLatitude = null;
              _gpsLongitude = null;
              _pendingGpsGovernorateId = null;
              _statusMessage = 'تغير اختيار العداد أثناء تحديد الموقع. راجع المحافظة الموجودة في المنتصف ثم أكّدها.';
              _statusIsError = true;
            });
          } else {
            setState(() {
              _selectedGovernorateId = governorate.id;
              _gpsLatitude = result.latitude;
              _gpsLongitude = result.longitude;
              _isGpsSuggestion = true;
              _statusMessage = 'المحافظة المقترحة حسب موقعك: ${governorate.nameAr}. يمكنك تغييرها قبل التأكيد.';
              _statusIsError = false;
              _openAppSettingsAction = false;
              _openLocationSettingsAction = false;
            });
          }
        }
      } else {
        setState(() {
          _statusMessage = _messageForLocationStatus(result.status);
          _statusIsError = true;
          _openAppSettingsAction = result.status == LocationDetectionStatus.permissionDeniedForever;
          _openLocationSettingsAction = result.status == LocationDetectionStatus.serviceDisabled;
        });
      }
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _statusMessage = 'تعذر إكمال تحديد الموقع. يمكنك اختيار المحافظة يدوياً.';
        _statusIsError = true;
        _openAppSettingsAction = false;
        _openLocationSettingsAction = false;
      });
    } finally {
      if (mounted) setState(() => _isDetecting = false);
    }
  }

  void _onWheelScrollingChanged(bool scrolling) {
    if (!mounted || _isWheelScrolling == scrolling) return;
    setState(() => _isWheelScrolling = scrolling);
  }

  String _messageForLocationStatus(LocationDetectionStatus status) {
    return switch (status) {
      LocationDetectionStatus.serviceDisabled =>
        'خدمات الموقع متوقفة. فعّل GPS أو اختر المحافظة يدوياً.',
      LocationDetectionStatus.permissionDenied =>
        'لم يتم منح إذن الموقع. يمكنك المحاولة مجدداً أو اختيار المحافظة يدوياً.',
      LocationDetectionStatus.permissionDeniedForever =>
        'تم رفض إذن الموقع نهائياً. يمكنك تفعيله من إعدادات الجهاز أو اختيار المحافظة يدوياً.',
      LocationDetectionStatus.outsideIraq =>
        'الموقع لا يقع داخل العراق وفق نتيجة التحقق. اختر المحافظة يدوياً.',
      LocationDetectionStatus.lowAccuracy =>
        'دقة الموقع الحالية منخفضة ولا تكفي لتحديد المحافظة بثقة. حسّن إشارة GPS أو اختر يدوياً.',
      LocationDetectionStatus.governorateNotFound =>
        'تعذر مطابقة الموقع مع محافظة عراقية. اختر المحافظة يدوياً.',
      LocationDetectionStatus.timedOut =>
        'انتهت مهلة تحديد الموقع. تحقق من GPS وحاول مجدداً أو اختر يدوياً.',
      LocationDetectionStatus.unavailable =>
        'تعذر تحديد الموقع حالياً. تحقق من خدمات الموقع أو اختر المحافظة يدوياً.',
      LocationDetectionStatus.success => '',
    };
  }

  Future<void> _confirm(List<Governorate> governorates) async {
    final id = _selectedGovernorateId;
    if (id == null) return;
    final matches = governorates.where((item) => item.id == id);
    if (matches.isEmpty) return;
    final governorate = matches.first;
    final latitude = _isGpsSuggestion && _gpsLatitude != null
        ? _gpsLatitude!
        : governorate.latitude;
    final longitude = _isGpsSuggestion && _gpsLongitude != null
        ? _gpsLongitude!
        : governorate.longitude;
    final source = _isGpsSuggestion ? 'gps' : 'manual';

    try {
      await ref.read(initialSetupProvider.notifier).complete(
            governorate: governorate,
            latitude: latitude,
            longitude: longitude,
            source: source,
          );
      await ref.read(selectedGovernorateProvider.notifier).select(governorate.id);
      await ref.read(selectedPrayerCityProvider.notifier).select(null);
      ref.invalidate(prayerTimesProvider);
      if (!mounted) return;
      if (widget.isEmbedded) {
        if (widget.requiredFlow) {
          setState(() => _allowRequiredFlowExit = true);
          WidgetsBinding.instance.addPostFrameCallback((_) {
            if (mounted) Navigator.of(context).pop(true);
          });
        } else {
          Navigator.of(context).pop(true);
        }
      } else if (widget.isChangeMode) {
        context.pop();
      } else {
        context.go('/');
      }
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _statusMessage = 'تعذر حفظ المحافظة على الجهاز. تحقق من مساحة التخزين ثم أعد المحاولة.';
        _statusIsError = true;
      });
    }
  }

  int _initialIndex(List<Governorate> governorates) {
    final selected = _selectedGovernorateId;
    if (selected == null) return -1;
    return governorates.indexWhere((item) => item.id == selected);
  }

  @override
  Widget build(BuildContext context) {
    final governorates = ref.watch(governoratesProvider);
    final reduceMotion = MediaQuery.of(context).disableAnimations;
    final theme = Theme.of(context);

    return PopScope(
      canPop: !widget.requiredFlow || _allowRequiredFlowExit,
      child: Scaffold(
      backgroundColor: Colors.transparent,
      body: SafeArea(
        child: governorates.when(
          loading: () => const Center(child: CircularProgressIndicator()),
          error: (error, stack) => _CatalogueError(
            onRetry: () => ref.invalidate(governoratesProvider),
          ),
          data: (items) => LayoutBuilder(
            builder: (context, constraints) {
              final hasValidSelection = items.any((item) => item.id == _selectedGovernorateId);
              return SingleChildScrollView(
                physics: const ClampingScrollPhysics(),
                child: ConstrainedBox(
                  constraints: BoxConstraints(minHeight: constraints.maxHeight),
                  child: Center(
                    child: ConstrainedBox(
                      constraints: const BoxConstraints(maxWidth: 520),
                      child: Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            if (widget.isChangeMode)
                              Align(
                                alignment: AlignmentDirectional.centerStart,
                                child: TextButton.icon(
                                  onPressed: () => widget.isEmbedded ? Navigator.of(context).pop(false) : context.pop(),
                                  icon: const Icon(Icons.arrow_forward),
                                  label: const Text('رجوع'),
                                ),
                              ),
                            Text(
                              widget.isChangeMode ? 'تغيير المحافظة' : 'اختر محافظتك',
                              textAlign: TextAlign.center,
                              style: theme.textTheme.headlineSmall?.copyWith(
                                fontWeight: FontWeight.w900,
                              ),
                            ),
                            const SizedBox(height: 8),
                            Text(
                              'اسحب العداد للأعلى أو للأسفل، ثم أكّد المحافظة الظاهرة في المنتصف.',
                              textAlign: TextAlign.center,
                              style: theme.textTheme.bodyMedium?.copyWith(height: 1.6),
                            ),
                            const SizedBox(height: 8),
                            GovernorateWheelPicker(
                              key: _wheelKey,
                              governorates: items,
                              initialSelectedIndex: _initialIndex(items),
                              reduceMotion: reduceMotion,
                              onSelected: _onGovernorateSelected,
                              onScrollingChanged: _onWheelScrollingChanged,
                              compact: widget.isEmbedded,
                            ),
                            const SizedBox(height: 5),
                            Text(
                              'يمكنك استخدام الموقع لاقتراح المحافظة. تُحفظ الإحداثيات في التخزين الآمن على جهازك، وتُرسل عند طلب مواقيت الصلاة، ولا تُحفظ في قاعدة كاش المواقيت بالخادم. لا يُطلب إذن الموقع في الخلفية.',
                              textAlign: TextAlign.center,
                              style: theme.textTheme.bodySmall?.copyWith(height: 1.35, fontSize: 11),
                            ),
                            const SizedBox(height: 5),
                            TextButton.icon(
                              onPressed: _isDetecting ? null : () => _detectLocation(items),
                              icon: _isDetecting
                                  ? const SizedBox(
                                      width: 18,
                                      height: 18,
                                      child: CircularProgressIndicator(strokeWidth: 2),
                                    )
                                  : const Icon(Icons.my_location_outlined),
                              label: Text(_isDetecting ? 'جارٍ تحديد الموقع' : 'تحديد موقعي تلقائياً'),
                            ),
                            if (_isGpsSuggestion)
                              TextButton(
                                onPressed: _isDetecting
                                    ? null
                                    : () {
                                        setState(() {
                                          _isGpsSuggestion = false;
                                          _gpsLatitude = null;
                                          _gpsLongitude = null;
                                          _pendingGpsGovernorateId = null;
                                          _statusMessage = 'سيتم استخدام إحداثيات مركز المحافظة التقريبية بدلاً من موقع GPS.';
                                          _statusIsError = false;
                                        });
                                      },
                                child: const Text('استخدام مركز المحافظة يدوياً'),
                              ),
                            if (_statusMessage != null) ...[
                              const SizedBox(height: 4),
                              Text(
                                _statusMessage!,
                                textAlign: TextAlign.center,
                                style: theme.textTheme.bodySmall?.copyWith(
                                  color: _statusIsError
                                      ? theme.colorScheme.error
                                      : theme.colorScheme.primary,
                                  height: 1.5,
                                ),
                              ),
                              if (_openAppSettingsAction || _openLocationSettingsAction)
                                TextButton.icon(
                                  onPressed: () async {
                                    final opened = _openAppSettingsAction
                                        ? await Geolocator.openAppSettings()
                                        : await Geolocator.openLocationSettings();
                                    if (!opened && mounted) {
                                      ScaffoldMessenger.of(context).showSnackBar(
                                        const SnackBar(content: Text('تعذر فتح إعدادات الموقع على هذا الجهاز.')),
                                      );
                                    }
                                  },
                                  icon: const Icon(Icons.settings_outlined),
                                  label: Text(_openAppSettingsAction ? 'فتح إعدادات أذونات التطبيق' : 'فتح إعدادات GPS'),
                                ),
                            ],
                            const SizedBox(height: 14),
                            SizedBox(
                              height: 46,
                              child: FilledButton(
                                onPressed: !hasValidSelection || _isDetecting || _isWheelScrolling
                                    ? null
                                    : () => _confirm(items),
                                child: const Text('تأكيد المحافظة والمتابعة'),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ),
              );
            },
          ),
        ),
      ),
    ),
    );
  }
}

class _CatalogueError extends StatelessWidget {
  const _CatalogueError({required this.onRetry});
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) => Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text('تعذر تحميل قائمة المحافظات.'),
              const SizedBox(height: 12),
              TextButton(onPressed: onRetry, child: const Text('إعادة المحاولة')),
            ],
          ),
        ),
      );
}
