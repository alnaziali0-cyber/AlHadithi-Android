import 'package:flutter_test/flutter_test.dart';
import 'package:khazain_al_huda/core/safe_display_text.dart';

void main() {
  test('removes raw external URLs from display text', () {
    expect(
      safeDisplayText('راجع المصدر https://example.org/path?x=1'),
      'راجع المصدر',
    );
  });

  test('preserves a Markdown link label while removing its external target',
      () {
    expect(
      safeDisplayText('راجع [المصدر](https://example.org/book) الآن'),
      'راجع المصدر الآن',
    );
  });
}
