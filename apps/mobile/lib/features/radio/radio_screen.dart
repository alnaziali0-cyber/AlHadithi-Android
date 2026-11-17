import 'package:flutter/material.dart';

class RadioScreen extends StatelessWidget {
  const RadioScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return CustomScrollView(
      slivers: [
        SliverPadding(
          padding: const EdgeInsets.all(18),
          sliver: SliverList(
            delegate: SliverChildListDelegate([
              Text(
                'إذاعة العتبة الحسينية',
                style: theme.textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900),
              ),
              const SizedBox(height: 12),
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(20),
                  child: Column(
                    children: [
                      Icon(Icons.radio_rounded,
                          size: 42, color: theme.colorScheme.secondary),
                      const SizedBox(height: 12),
                      const Text(
                        'لا يتوفر حالياً ملف إذاعي محلي مرخّص داخل التطبيق. أوقفنا فتح مواقع البث وتشغيل الروابط الخارجية؛ لن يظهر بث حتى يتوفر ملف محلي مع إذن واضح.',
                        textAlign: TextAlign.center,
                        style: TextStyle(height: 1.7),
                      ),
                    ],
                  ),
                ),
              ),
            ]),
          ),
        ),
      ],
    );
  }
}
