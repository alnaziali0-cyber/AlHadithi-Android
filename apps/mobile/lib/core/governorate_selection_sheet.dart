import 'package:flutter/material.dart';

import '../features/governorate/governorate_selection_screen.dart';

Future<bool?> showGovernorateSelectionSheet(
  BuildContext context, {
  bool requiredSelection = false,
}) {
  return showModalBottomSheet<bool>(
    context: context,
    isScrollControlled: true,
    isDismissible: !requiredSelection,
    enableDrag: !requiredSelection,
    backgroundColor: Theme.of(context).colorScheme.surface,
    useSafeArea: true,
    showDragHandle: !requiredSelection,
    shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(28))),
    builder: (context) => FractionallySizedBox(
      heightFactor: requiredSelection ? .76 : .66,
      child: ClipRRect(
        borderRadius: const BorderRadius.vertical(top: Radius.circular(28)),
        child: GovernorateSelectionScreen(isChangeMode: !requiredSelection, isEmbedded: true, requiredFlow: requiredSelection),
      ),
    ),
  );
}
