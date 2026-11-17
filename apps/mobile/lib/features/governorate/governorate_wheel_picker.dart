import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../core/models.dart';

const _governorateGold = Color(0xFFC8A14A);

class GovernorateWheelPicker extends StatefulWidget {
  const GovernorateWheelPicker({
    super.key,
    required this.governorates,
    required this.initialSelectedIndex,
    required this.onSelected,
    required this.reduceMotion,
    required this.onScrollingChanged,
    this.compact = false,
  });

  final List<Governorate> governorates;
  final int initialSelectedIndex;
  final ValueChanged<Governorate> onSelected;
  final bool reduceMotion;
  final ValueChanged<bool> onScrollingChanged;
  final bool compact;

  @override
  State<GovernorateWheelPicker> createState() => GovernorateWheelPickerState();
}

class GovernorateWheelPickerState extends State<GovernorateWheelPicker> {
  static const int _initialVirtualItem = 10000;
  late final FixedExtentScrollController _controller;
  late int _centerVirtualItem;
  int? _selectedIndex;
  bool _programmaticScroll = false;

  @override
  void initState() {
    super.initState();
    _selectedIndex = widget.initialSelectedIndex >= 0 &&
            widget.initialSelectedIndex < widget.governorates.length
        ? widget.initialSelectedIndex
        : null;
    _centerVirtualItem = _selectedIndex == null
        ? _initialVirtualItem
        : _initialVirtualItem + _selectedIndex! + 1;
    _controller = FixedExtentScrollController(initialItem: _centerVirtualItem);
  }

  @override
  void didUpdateWidget(covariant GovernorateWheelPicker oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.governorates.length != widget.governorates.length &&
        widget.governorates.isNotEmpty) {
      final retained = _selectedIndex;
      if (retained != null && retained >= widget.governorates.length) {
        _selectedIndex = null;
        _centerVirtualItem = _initialVirtualItem;
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (mounted && _controller.hasClients) {
            _controller.jumpToItem(_centerVirtualItem);
          }
        });
      }
    }
  }

  Future<Governorate?> selectGovernorateIndex(int index, {int? tappedVirtualItem}) async {
    if (index < 0 || index >= widget.governorates.length) return null;
    final count = widget.governorates.length;
    var target = tappedVirtualItem ?? (_initialVirtualItem + index + 1);
    if (_selectedIndex != null && _controller.hasClients) {
      final currentVirtual = _controller.selectedItem;
      final currentIndex = _indexForVirtualItem(currentVirtual);
      var delta = (index - currentIndex + count) % count;
      if (delta > count / 2) delta -= count;
      target = currentVirtual + delta;
    }
    if (!_controller.hasClients) {
      _select(index, target);
      return widget.governorates[index];
    }
    if (widget.reduceMotion) {
      _controller.jumpToItem(target);
      _select(index, target);
      return widget.governorates[index];
    }
    _programmaticScroll = true;
    try {
      await _controller.animateToItem(
        target,
        duration: const Duration(milliseconds: 420),
        curve: Curves.easeOutCubic,
      );
    } catch (_) {
      _programmaticScroll = false;
      rethrow;
    }
    if (!mounted) return null;
    final settledVirtualItem = _controller.selectedItem;
    final settledIndex = _indexForVirtualItem(settledVirtualItem);
    _programmaticScroll = false;
    _select(settledIndex, settledVirtualItem);
    return widget.governorates[settledIndex];
  }

  int _indexForVirtualItem(int virtualItem) {
    final count = widget.governorates.length;
    if (_selectedIndex == null) {
      if (virtualItem > _initialVirtualItem) {
        return (virtualItem - _initialVirtualItem - 1) % count;
      }
      return ((virtualItem - _initialVirtualItem) % count + count) % count;
    }
    return ((virtualItem - _centerVirtualItem + _selectedIndex!) % count + count) % count;
  }

  void _select(int index, int virtualItem) {
    if (!mounted) return;
    setState(() {
      _selectedIndex = index;
      _centerVirtualItem = virtualItem;
    });
    widget.onSelected(widget.governorates[index]);
  }

  void _onSelectedItemChanged(int virtualItem) {
    if (widget.governorates.isEmpty) return;
    if (_selectedIndex == null && virtualItem == _initialVirtualItem) return;
    if (_programmaticScroll) return;
    final index = _indexForVirtualItem(virtualItem);
    _select(index, virtualItem);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (widget.governorates.isEmpty) {
      return const SizedBox(
        height: 300,
        child: Center(child: Text('لا توجد محافظات متاحة')),
      );
    }
    final color = Theme.of(context).colorScheme.onSurface;
    return SizedBox(
      height: widget.compact ? 188 : 252,
      child: Directionality(
        textDirection: TextDirection.rtl,
        child: NotificationListener<ScrollNotification>(
          onNotification: (notification) {
            if (notification is ScrollStartNotification) {
              widget.onScrollingChanged(true);
            } else if (notification is ScrollEndNotification) {
              widget.onScrollingChanged(false);
            }
            return false;
          },
          child: ListWheelScrollView.useDelegate(
          controller: _controller,
          itemExtent: widget.compact ? 42 : 54,
          diameterRatio: widget.reduceMotion ? 1000 : 1.75,
          perspective: widget.reduceMotion ? 0.0001 : 0.0028,
          squeeze: 1.0,
          useMagnifier: !widget.reduceMotion,
          magnification: widget.reduceMotion ? 1.0 : 1.12,
          overAndUnderCenterOpacity: widget.reduceMotion ? 0.82 : 0.48,
          physics: const FixedExtentScrollPhysics(),
          clipBehavior: Clip.none,
          onSelectedItemChanged: _onSelectedItemChanged,
          childDelegate: ListWheelChildBuilderDelegate(
            childCount: null,
            builder: (context, virtualItem) {
              final isInitialPrompt =
                  _selectedIndex == null && virtualItem == _initialVirtualItem;
              if (isInitialPrompt) {
                return const Center(
                  child: Text(
                    'اختر محافظتك',
                    textAlign: TextAlign.center,
                    style: TextStyle(fontSize: 20, fontWeight: FontWeight.w700),
                  ),
                );
              }
              final index = _indexForVirtualItem(virtualItem);
              final governorate = widget.governorates[index];
              final distance = (virtualItem - _centerVirtualItem).abs();
              final opacity = math.max(0.22, 1.0 - (distance * 0.22)).toDouble();
              final fontSize = switch (distance) {
                0 => widget.compact ? 19.0 : 22.0,
                1 => widget.compact ? 15.5 : 18.0,
                2 => widget.compact ? 13.0 : 15.0,
                _ => widget.compact ? 11.5 : 13.0,
              };
              return Semantics(
                button: true,
                selected: distance == 0,
                label: governorate.nameAr,
                child: GestureDetector(
                  behavior: HitTestBehavior.opaque,
                  onTap: () => selectGovernorateIndex(index, tappedVirtualItem: virtualItem),
                  child: Center(
                    child: Opacity(
                      opacity: opacity,
                      child: Transform.scale(
                        scale: distance == 0 ? 1.0 : math.max(0.84, 1.0 - distance * 0.055).toDouble(),
                        child: Container(
                          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 2),
                          decoration: distance == 0
                              ? BoxDecoration(
                                  color: _governorateGold.withOpacity(.12),
                                  borderRadius: BorderRadius.circular(18),
                                  border: Border.all(color: _governorateGold.withOpacity(.72)),
                                )
                              : null,
                          child: Text(
                            governorate.nameAr,
                            maxLines: 1,
                            textAlign: TextAlign.center,
                            style: TextStyle(
                              color: distance == 0 ? _governorateGold : color,
                              fontSize: fontSize,
                              fontWeight: distance == 0 ? FontWeight.w900 : FontWeight.w600,
                              letterSpacing: 0.1,
                            ),
                          ),
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
