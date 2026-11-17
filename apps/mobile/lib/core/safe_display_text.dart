/// Removes external URLs from user-visible text while preserving link labels.
String safeDisplayText(Object? value) {
  final input = value?.toString() ?? '';
  final withoutMarkdownTargets = input.replaceAllMapped(
    RegExp(r'\[([^\]]+)\]\(https?://[^)\s]+\)', caseSensitive: false),
    (match) => match.group(1) ?? '',
  );
  return withoutMarkdownTargets
      .replaceAll(RegExp(r'https?://[^\s<>()]+|www\.[^\s<>()]+', caseSensitive: false), '')
      .replaceAll(RegExp(r'[ \t]{2,}'), ' ')
      .trim();
}
