String? initialSetupRedirect({
  required String? selectedGovernorateId,
  required String matchedLocation,
  required bool isChangeMode,
}) {
  final hasSelection = selectedGovernorateId != null && selectedGovernorateId.trim().isNotEmpty;
  final isSetupRoute = matchedLocation == '/setup';
  if (!hasSelection && !isSetupRoute) return '/setup';
  if (hasSelection && isSetupRoute && !isChangeMode) return '/';
  return null;
}
