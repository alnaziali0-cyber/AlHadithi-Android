import 'package:flutter/material.dart';

/// Shared building blocks for the settings hub and its sub-pages.
///
/// Every row is a tappable button, a toggle, or an explicitly read-only status
/// row. Nothing here is decorative: callers pass real actions or leave `onTap`
/// null so the row is shown as informational.

class SettingsGroup extends StatelessWidget {
  const SettingsGroup({super.key, required this.title, required this.children, this.footer});

  final String title;
  final List<Widget> children;
  final String? footer;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final rows = <Widget>[];
    for (var index = 0; index < children.length; index++) {
      if (index > 0) {
        rows.add(Divider(
          height: 1,
          thickness: 1,
          indent: 66,
          endIndent: 14,
          color: theme.dividerColor.withOpacity(.6),
        ));
      }
      rows.add(children[index]);
    }
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(8, 0, 8, 8),
          child: Text(
            title,
            style: theme.textTheme.titleSmall?.copyWith(
              fontWeight: FontWeight.w800,
              color: theme.colorScheme.primary,
            ),
          ),
        ),
        Card(
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 6, horizontal: 8),
            child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: rows),
          ),
        ),
        if (footer != null)
          Padding(
            padding: const EdgeInsets.fromLTRB(10, 8, 10, 0),
            child: Text(footer!, style: theme.textTheme.bodySmall?.copyWith(height: 1.6)),
          ),
      ],
    );
  }
}

class SettingsTile extends StatelessWidget {
  const SettingsTile({
    super.key,
    required this.icon,
    required this.title,
    this.subtitle,
    this.value,
    this.onTap,
    this.danger = false,
    this.trailingIcon,
  });

  final IconData icon;
  final String title;
  final String? subtitle;
  final String? value;
  final VoidCallback? onTap;
  final bool danger;
  final IconData? trailingIcon;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final enabled = onTap != null;
    final titleColor = danger
        ? scheme.error
        : (enabled ? scheme.onSurface : scheme.onSurface.withOpacity(.55));
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(18),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 10),
        child: Row(
          children: [
            SettingsIconBadge(icon: icon, danger: danger, muted: !enabled),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    title,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(fontWeight: FontWeight.w800, fontSize: 15, color: titleColor),
                  ),
                  if (subtitle != null) ...[
                    const SizedBox(height: 3),
                    Text(
                      subtitle!,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.bodySmall?.copyWith(height: 1.45),
                    ),
                  ],
                ],
              ),
            ),
            if (value != null) ...[
              const SizedBox(width: 8),
              SettingsValuePill(text: value!),
            ],
            if (enabled)
              Padding(
                padding: const EdgeInsetsDirectional.only(start: 4),
                child: Icon(
                  trailingIcon ?? Icons.chevron_left_rounded,
                  size: trailingIcon == null ? 24 : 18,
                  color: scheme.onSurface.withOpacity(.45),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class SettingsSwitchTile extends StatelessWidget {
  const SettingsSwitchTile({
    super.key,
    required this.icon,
    required this.title,
    required this.value,
    required this.onChanged,
    this.subtitle,
  });

  final IconData icon;
  final String title;
  final String? subtitle;
  final bool value;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return InkWell(
      onTap: () => onChanged(!value),
      borderRadius: BorderRadius.circular(18),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 10),
        child: Row(
          children: [
            SettingsIconBadge(icon: icon),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(title, style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 15)),
                  if (subtitle != null) ...[
                    const SizedBox(height: 3),
                    Text(subtitle!, style: theme.textTheme.bodySmall?.copyWith(height: 1.45)),
                  ],
                ],
              ),
            ),
            const SizedBox(width: 8),
            Switch.adaptive(value: value, onChanged: onChanged),
          ],
        ),
      ),
    );
  }
}

/// Read-only status row: no chevron, no ripple, never looks tappable.
class SettingsStatusRow extends StatelessWidget {
  const SettingsStatusRow({super.key, required this.icon, required this.title, required this.status, this.subtitle});

  final IconData icon;
  final String title;
  final String status;
  final String? subtitle;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 10),
      child: Row(
        children: [
          SettingsIconBadge(icon: icon, muted: true),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(title, style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 15)),
                if (subtitle != null) ...[
                  const SizedBox(height: 3),
                  Text(subtitle!, style: theme.textTheme.bodySmall?.copyWith(height: 1.45)),
                ],
              ],
            ),
          ),
          const SizedBox(width: 8),
          SettingsValuePill(text: status, muted: true),
        ],
      ),
    );
  }
}

class SettingsNoticeCard extends StatelessWidget {
  const SettingsNoticeCard({super.key, required this.icon, required this.text});

  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: theme.colorScheme.surfaceContainerHighest.withOpacity(.35),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: theme.colorScheme.primary, size: 22),
          const SizedBox(width: 12),
          Expanded(child: Text(text, style: theme.textTheme.bodyMedium?.copyWith(height: 1.7))),
        ],
      ),
    );
  }
}

class SettingsIconBadge extends StatelessWidget {
  const SettingsIconBadge({super.key, required this.icon, this.danger = false, this.muted = false});

  final IconData icon;
  final bool danger;
  final bool muted;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final Color tint = danger ? scheme.error : scheme.secondary;
    final Color iconColor = danger ? scheme.error : (muted ? scheme.onSurface.withOpacity(.5) : scheme.primary);
    return Container(
      width: 40,
      height: 40,
      decoration: BoxDecoration(
        color: tint.withOpacity(muted ? .08 : .16),
        borderRadius: BorderRadius.circular(14),
      ),
      child: Icon(icon, color: iconColor, size: 21),
    );
  }
}

class SettingsValuePill extends StatelessWidget {
  const SettingsValuePill({super.key, required this.text, this.muted = false});

  final String text;
  final bool muted;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return ConstrainedBox(
      constraints: const BoxConstraints(maxWidth: 150),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
        decoration: BoxDecoration(
          color: scheme.surfaceContainerHighest.withOpacity(.6),
          borderRadius: BorderRadius.circular(999),
        ),
        child: Text(
          text,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: TextStyle(
            fontSize: 12,
            fontWeight: FontWeight.w700,
            color: muted ? scheme.onSurface.withOpacity(.6) : scheme.primary,
          ),
        ),
      ),
    );
  }
}

class SettingsOption<T> {
  const SettingsOption({required this.value, required this.label, this.description});

  final T value;
  final String label;
  final String? description;
}

/// Opens an in-app bottom sheet with one option per row. Resolves to the chosen
/// value, or null when the sheet is dismissed without a choice.
Future<T?> showSettingsOptionSheet<T>({
  required BuildContext context,
  required String title,
  required T current,
  required List<SettingsOption<T>> options,
  String? description,
}) {
  return showModalBottomSheet<T>(
    context: context,
    showDragHandle: true,
    isScrollControlled: true,
    builder: (sheetContext) {
      final theme = Theme.of(sheetContext);
      final scheme = theme.colorScheme;
      return SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 4, 16, 18),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                title,
                style: theme.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900, color: scheme.primary),
              ),
              if (description != null) ...[
                const SizedBox(height: 6),
                Text(description, style: theme.textTheme.bodySmall?.copyWith(height: 1.6)),
              ],
              const SizedBox(height: 12),
              for (final option in options)
                _OptionRow<T>(
                  option: option,
                  selected: option.value == current,
                  onTap: () => Navigator.of(sheetContext).pop(option.value),
                ),
            ],
          ),
        ),
      );
    },
  );
}

class _OptionRow<T> extends StatelessWidget {
  const _OptionRow({required this.option, required this.selected, required this.onTap});

  final SettingsOption<T> option;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Material(
        color: selected ? scheme.secondary.withOpacity(.12) : scheme.surfaceContainerHighest.withOpacity(.35),
        borderRadius: BorderRadius.circular(18),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(18),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 13),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        option.label,
                        style: TextStyle(
                          fontWeight: selected ? FontWeight.w900 : FontWeight.w700,
                          fontSize: 15,
                          color: selected ? scheme.primary : scheme.onSurface,
                        ),
                      ),
                      if (option.description != null) ...[
                        const SizedBox(height: 3),
                        Text(option.description!, style: theme.textTheme.bodySmall?.copyWith(height: 1.5)),
                      ],
                    ],
                  ),
                ),
                const SizedBox(width: 10),
                Icon(
                  selected ? Icons.check_circle_rounded : Icons.radio_button_unchecked_rounded,
                  color: selected ? scheme.secondary : scheme.onSurface.withOpacity(.35),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// Asks before any destructive or data-removing action. Resolves to true only
/// when the user explicitly confirms.
Future<bool> confirmSettingsAction(
  BuildContext context, {
  required String title,
  required String body,
  required String confirmLabel,
  bool destructive = false,
}) async {
  final scheme = Theme.of(context).colorScheme;
  final confirmed = await showDialog<bool>(
    context: context,
    builder: (dialogContext) => AlertDialog(
      title: Text(title, style: const TextStyle(fontWeight: FontWeight.w900)),
      content: Text(body, style: const TextStyle(height: 1.6)),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(dialogContext).pop(false),
          child: const Text('إلغاء'),
        ),
        FilledButton(
          style: destructive ? FilledButton.styleFrom(backgroundColor: scheme.error) : null,
          onPressed: () => Navigator.of(dialogContext).pop(true),
          child: Text(confirmLabel),
        ),
      ],
    ),
  );
  return confirmed == true;
}

/// Shows a short confirmation message after a local data action finishes.
void showSettingsMessage(BuildContext context, String message) {
  ScaffoldMessenger.maybeOf(context)
    ?..hideCurrentSnackBar()
    ..showSnackBar(SnackBar(content: Text(message)));
}
