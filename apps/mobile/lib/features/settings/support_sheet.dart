import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

class SupportSheet {
  SupportSheet._();

  static const whatsappNumber = '07866580209';
  static const whatsappUri = 'https://wa.me/9647866580209';

  static Future<void> show(BuildContext context) async {
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      backgroundColor: Theme.of(context).colorScheme.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(26)),
      ),
      builder: (sheetContext) => Padding(
        padding: const EdgeInsets.fromLTRB(18, 12, 18, 28),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Center(
              child: Container(
                width: 42,
                height: 4,
                decoration: BoxDecoration(
                  color: Theme.of(sheetContext).colorScheme.onSurface.withOpacity(.28),
                  borderRadius: BorderRadius.circular(10),
                ),
              ),
            ),
            const SizedBox(height: 20),
            Text(
              'الدعم والتواصل',
              textAlign: TextAlign.center,
              style: Theme.of(sheetContext).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900),
            ),
            const SizedBox(height: 6),
            Text(
              'اختر وسيلة التواصل المناسبة.',
              textAlign: TextAlign.center,
              style: Theme.of(sheetContext).textTheme.bodySmall,
            ),
            const SizedBox(height: 16),
            _SupportAction(
              icon: Icons.chat_rounded,
              title: 'واتساب الدعم',
              subtitle: whatsappNumber,
              onTap: () => _open(sheetContext, Uri.parse('$whatsappUri?text=${Uri.encodeComponent('السلام عليكم، أحتاج مساعدة في تطبيق خزائن الهدى.')}')),
            ),
            _SupportAction(
              icon: Icons.camera_alt_rounded,
              title: 'إنستغرام',
              subtitle: '@khazain_al_huda',
              onTap: () => _open(sheetContext, Uri.parse('https://www.instagram.com/khazain_al_huda/')),
            ),
            _SupportAction(
              icon: Icons.music_note_rounded,
              title: 'تيك توك',
              subtitle: '@khazain_al_huda',
              onTap: () => _open(sheetContext, Uri.parse('https://www.tiktok.com/@khazain_al_huda')),
            ),
          ],
        ),
      ),
    );
  }

  static Future<void> _open(BuildContext context, Uri uri) async {
    try {
      final opened = await launchUrl(uri, mode: LaunchMode.externalApplication);
      if (!opened && context.mounted) _showError(context);
    } catch (_) {
      if (context.mounted) _showError(context);
    }
  }

  static void _showError(BuildContext context) {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('تعذر فتح وسيلة التواصل على هذا الجهاز.')),
    );
  }
}

class _SupportAction extends StatelessWidget {
  const _SupportAction({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.onTap,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return ListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 4),
      leading: CircleAvatar(
        backgroundColor: theme.colorScheme.primary.withOpacity(.12),
        child: Icon(icon, color: theme.colorScheme.primary),
      ),
      title: Text(title, style: const TextStyle(fontWeight: FontWeight.w800)),
      subtitle: Text(subtitle),
      trailing: const Icon(Icons.open_in_new_rounded, size: 18),
      onTap: onTap,
    );
  }
}
