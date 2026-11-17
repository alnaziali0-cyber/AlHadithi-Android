import 'dart:ui';

import 'package:flutter/material.dart';

import 'theme.dart';

@immutable
class GlassNavItem {
  const GlassNavItem({
    required this.label,
    required this.icon,
    required this.location,
  });

  final String label;
  final IconData icon;
  final String location;
}

/// Existing app destinations, ordered from right to left in Arabic layouts.
/// A charity route is intentionally not listed because the app has no charity screen.
const glassNavItems = <GlassNavItem>[
  GlassNavItem(label: 'المواقيت', icon: Icons.schedule_rounded, location: '/'),
  GlassNavItem(
      label: 'المصحف', icon: Icons.menu_book_rounded, location: '/quran'),
  GlassNavItem(
      label: 'القبلة', icon: Icons.explore_outlined, location: '/qibla'),
  GlassNavItem(
      label: 'المسبحة', icon: Icons.fingerprint_rounded, location: '/tasbih'),
  GlassNavItem(
      label: 'الإعدادات', icon: Icons.settings_outlined, location: '/settings'),
];

class GlassBottomBar extends StatelessWidget {
  const GlassBottomBar({
    super.key,
    required this.items,
    required this.selectedIndex,
    required this.onSelect,
  });

  final List<GlassNavItem> items;
  final int selectedIndex;
  final ValueChanged<int> onSelect;

  static const _barRadius = BorderRadius.all(Radius.circular(40));
  static const _bubbleRadius = BorderRadius.all(Radius.circular(32));

  @override
  Widget build(BuildContext context) {
    if (items.isEmpty) return const SizedBox.shrink();

    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;
    final activeColor =
        isDark ? const Color(0xFF3DDC97) : theme.colorScheme.primary;
    final safeIndex = selectedIndex.clamp(0, items.length - 1).toInt();
    final textDirection = Directionality.of(context);
    final progress = items.length == 1 ? 0.0 : safeIndex / (items.length - 1);
    final alignmentX = textDirection == TextDirection.rtl
        ? 1 - (2 * progress)
        : (2 * progress) - 1;

    return SafeArea(
      top: false,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 0, 16, 10),
        child: SizedBox(
          height: 74,
          child: DecoratedBox(
            decoration: BoxDecoration(
              borderRadius: _barRadius,
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(isDark ? .34 : .16),
                  blurRadius: 18,
                  offset: const Offset(0, 8),
                ),
              ],
            ),
            child: ClipRRect(
              borderRadius: _barRadius,
              child: BackdropFilter(
                filter: ImageFilter.blur(sigmaX: 16, sigmaY: 16),
                child: Container(
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      begin: Alignment.topCenter,
                      end: Alignment.bottomCenter,
                      colors: isDark
                          ? [
                              Colors.white.withOpacity(.20),
                              const Color(0xB20D1218)
                            ]
                          : [
                              Colors.white.withOpacity(.76),
                              const Color(0xDDE8EFEA)
                            ],
                    ),
                    borderRadius: _barRadius,
                    border: Border.all(
                      color: Colors.white.withOpacity(isDark ? .34 : .62),
                    ),
                  ),
                  padding:
                      const EdgeInsets.symmetric(horizontal: 6, vertical: 6),
                  child: Stack(
                    fit: StackFit.expand,
                    children: [
                      IgnorePointer(
                        child: AnimatedAlign(
                          alignment: Alignment(alignmentX, 0),
                          duration: const Duration(milliseconds: 520),
                          curve: Curves.easeOutBack,
                          child: FractionallySizedBox(
                            widthFactor: 1 / items.length,
                            heightFactor: 1,
                            child: Padding(
                              padding:
                                  const EdgeInsets.symmetric(horizontal: 2),
                              child: DecoratedBox(
                                decoration: BoxDecoration(
                                  gradient: LinearGradient(
                                    begin: Alignment.topCenter,
                                    end: Alignment.bottomCenter,
                                    colors: [
                                      Colors.white
                                          .withOpacity(isDark ? .28 : .54),
                                      activeColor.withOpacity(.12),
                                    ],
                                  ),
                                  borderRadius: _bubbleRadius,
                                  border: Border.all(
                                    color: Colors.white.withOpacity(.36),
                                  ),
                                ),
                                child: const SizedBox.expand(),
                              ),
                            ),
                          ),
                        ),
                      ),
                      Row(
                        children: [
                          for (var index = 0; index < items.length; index++)
                            Expanded(
                              child: _GlassTab(
                                item: items[index],
                                selected: index == safeIndex,
                                activeColor: activeColor,
                                inactiveColor: isDark
                                    ? Colors.white.withOpacity(.74)
                                    : theme.colorScheme.onSurface
                                        .withOpacity(.72),
                                onTap: () => onSelect(index),
                              ),
                            ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _GlassTab extends StatelessWidget {
  static const _bubbleRadius = BorderRadius.all(Radius.circular(32));

  const _GlassTab({
    super.key,
    required this.item,
    required this.selected,
    required this.activeColor,
    required this.inactiveColor,
    required this.onTap,
  });

  final GlassNavItem item;
  final bool selected;
  final Color activeColor;
  final Color inactiveColor;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final color = selected ? activeColor : inactiveColor;
    final textStyle = TextStyle(
      color: color,
      fontSize: 11,
      fontWeight: selected ? FontWeight.w800 : FontWeight.w600,
    );

    return Semantics(
      key: ValueKey<String>('glass-nav-tab-${item.location}'),
      label: item.label,
      button: true,
      selected: selected,
      onTap: onTap,
      child: ExcludeSemantics(
        child: Material(
          type: MaterialType.transparency,
          child: InkWell(
            onTap: onTap,
            borderRadius: _bubbleRadius,
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 2),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  AnimatedTheme(
                    data: Theme.of(context).copyWith(
                      iconTheme: IconThemeData(color: color),
                    ),
                    duration: const Duration(milliseconds: 220),
                    child: Icon(item.icon, size: 23),
                  ),
                  const SizedBox(height: 2),
                  Flexible(
                    child: AnimatedDefaultTextStyle(
                      style: textStyle,
                      duration: const Duration(milliseconds: 220),
                      child: FittedBox(
                        fit: BoxFit.scaleDown,
                        child: Text(
                          item.label,
                          maxLines: 1,
                          softWrap: false,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
