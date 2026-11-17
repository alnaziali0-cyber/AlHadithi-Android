import 'package:flutter/material.dart';

/// Describes the reviewed library reference without opening an external site.
class LibraryReferencePanel extends StatelessWidget {
  const LibraryReferencePanel({super.key});

  static const List<String> _topics = ['الحديث', 'موسوعات إسلامية'];

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          'مكتبة أهل البيت',
          style: theme.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900),
        ),
        const SizedBox(height: 8),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Text(
                  'سُجّل داخلياً أن شبكة الفكر تصف أرشيفاً يضم 4,095 كتاباً بحجم يقارب 2,000 ميغابايت. هذه معلومة توثيقية فقط؛ الأرشيف والكتب غير موجودة في التطبيق.',
                  style: TextStyle(height: 1.65),
                ),
                const SizedBox(height: 10),
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: theme.colorScheme.tertiaryContainer.withOpacity(.55),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: const Text(
                    'لم يظهر إذن صريح لإعادة توزيع الأرشيف أو تضمين كتبه. لذلك لم يُنزّل ولم يُضمّن، ولا يتوفر حالياً كتاب محلي مرخّص من هذا المصدر. يلزم توثيق حقوق كل ملف وطبعة على حدة.',
                    style: TextStyle(height: 1.65),
                  ),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 8),
        Text(
          'أقسام مرجعية غير متوفرة محلياً',
          style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800),
        ),
        const SizedBox(height: 6),
        for (final topic in _topics)
          Card(
            margin: const EdgeInsets.only(bottom: 8),
            child: ListTile(
              leading: const Icon(Icons.menu_book_outlined),
              title: Text(topic, style: const TextStyle(fontWeight: FontWeight.w800)),
              subtitle: const Text('لا يوجد حالياً ملف كتاب محلي موثّق لهذا القسم.'),
            ),
          ),
      ],
    );
  }
}
