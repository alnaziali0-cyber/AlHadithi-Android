import 'dart:convert';

import 'package:crypto/crypto.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../core/data_maintenance.dart';
import '../../core/prayer_notification_service.dart';
import '../../core/prayer_notification_sync.dart';
import '../../core/providers.dart';
import 'settings_widgets.dart';

const _developerUsername = 'KHAZAINALHUDA';
const _developerSalt = 'khazain-al-huda-dev-v1::';
// Salted SHA-256 of username:password+salt. The password itself is not shipped.
const _developerPasswordDigest =
    'd486220be61d7e5fb1160a96223c07c41c283db4a95193e55ac6b0492b5350f9';

class DeveloperPanelScreen extends ConsumerStatefulWidget {
  const DeveloperPanelScreen({super.key});

  @override
  ConsumerState<DeveloperPanelScreen> createState() =>
      _DeveloperPanelScreenState();
}

class _DeveloperPanelScreenState extends ConsumerState<DeveloperPanelScreen> {
  final _username = TextEditingController();
  final _password = TextEditingController();
  bool _unlocked = false;
  bool _busy = false;
  String? _error;

  @override
  void dispose() {
    _username.dispose();
    _password.dispose();
    super.dispose();
  }

  String _digest(String username, String password) => sha256
      .convert(utf8.encode('$username:$password$_developerSalt'))
      .toString();

  bool _constantTimeEquals(String a, String b) {
    if (a.length != b.length) return false;
    var difference = 0;
    for (var i = 0; i < a.length; i++) {
      difference |= a.codeUnitAt(i) ^ b.codeUnitAt(i);
    }
    return difference == 0;
  }

  Future<void> _unlock() async {
    if (_busy) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    final username = _username.text.trim().toUpperCase();
    final digest = _digest(username, _password.text);
    final valid = username == _developerUsername &&
        _constantTimeEquals(digest, _developerPasswordDigest);
    if (!mounted) return;
    setState(() {
      _busy = false;
      _unlocked = valid;
      _error = valid ? null : 'بيانات الدخول غير صحيحة.';
    });
    if (valid) _password.clear();
  }

  Future<void> _openWallpaperManagement() async {
    final base = Uri.tryParse(ref.read(apiProvider).options.baseUrl);
    if (base == null || !{'http', 'https'}.contains(base.scheme) || base.host.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('عنوان API غير صالح لفتح لوحة الإدارة.')),
      );
      return;
    }
    final adminUri = base.replace(path: '/admin', fragment: 'wallpapers');
    final opened = await launchUrl(adminUri, mode: LaunchMode.externalApplication);
    if (!opened && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('تعذر فتح لوحة إدارة الخلفيات.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      backgroundColor: Colors.transparent,
      appBar: AppBar(title: const Text('لوحة المطوّر')),
      body: SafeArea(
        child: _unlocked
            ? _buildPanel(context)
            : Center(
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 440),
                  child: ListView(
                    shrinkWrap: true,
                    padding: const EdgeInsets.all(22),
                    children: [
                      Icon(Icons.admin_panel_settings_rounded,
                          size: 58, color: theme.colorScheme.primary),
                      const SizedBox(height: 14),
                      Text('دخول المطوّر',
                          textAlign: TextAlign.center,
                          style: theme.textTheme.headlineSmall
                              ?.copyWith(fontWeight: FontWeight.w900)),
                      const SizedBox(height: 8),
                      Text('هذه اللوحة محمية ببيانات اعتماد محلية.',
                          textAlign: TextAlign.center,
                          style: theme.textTheme.bodySmall),
                      const SizedBox(height: 22),
                      TextField(
                        controller: _username,
                        textCapitalization: TextCapitalization.characters,
                        autocorrect: false,
                        enableSuggestions: false,
                        decoration: const InputDecoration(
                            labelText: 'اسم المستخدم',
                            prefixIcon: Icon(Icons.person_outline)),
                      ),
                      const SizedBox(height: 12),
                      TextField(
                        controller: _password,
                        obscureText: true,
                        autocorrect: false,
                        enableSuggestions: false,
                        decoration: const InputDecoration(
                            labelText: 'كلمة المرور',
                            prefixIcon: Icon(Icons.lock_outline)),
                        onSubmitted: (_) => _unlock(),
                      ),
                      if (_error != null)
                        Padding(
                          padding: const EdgeInsets.only(top: 10),
                          child: Text(_error!,
                              style: TextStyle(color: theme.colorScheme.error),
                              textAlign: TextAlign.center),
                        ),
                      const SizedBox(height: 18),
                      FilledButton.icon(
                        onPressed: _busy ? null : _unlock,
                        icon: _busy
                            ? const SizedBox(
                                width: 18,
                                height: 18,
                                child:
                                    CircularProgressIndicator(strokeWidth: 2))
                            : const Icon(Icons.lock_open_rounded),
                        label: Text(_busy ? 'جارٍ التحقق…' : 'فتح اللوحة'),
                      ),
                    ],
                  ),
                ),
              ),
      ),
    );
  }

  Widget _buildPanel(BuildContext context) {
    final theme = Theme.of(context);
    final location = ref.watch(hasPreciseLocationProvider);
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
      children: [
        Card(
            child: Padding(
          padding: const EdgeInsets.all(16),
          child:
              Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text('حالة التطبيق',
                style: theme.textTheme.titleMedium
                    ?.copyWith(fontWeight: FontWeight.w900)),
            const SizedBox(height: 12),
            SettingsStatusRow(
                icon: Icons.my_location_rounded,
                title: 'الموقع الدقيق المحفوظ',
                status: location.when(
                    data: (value) => value ? 'موجود' : 'غير محفوظ',
                    loading: () => 'جارٍ الفحص',
                    error: (_, __) => 'تعذر الفحص')),
            FutureBuilder<int>(
              future:
                  PrayerNotificationService.instance.pendingPrayerAlertsCount(),
              builder: (context, snapshot) => SettingsStatusRow(
                icon: Icons.notifications_active_outlined,
                title: 'الإشعارات المجدولة',
                status: snapshot.hasData
                    ? '${snapshot.data}'
                    : snapshot.hasError
                        ? 'تعذر الفحص'
                        : 'جارٍ الفحص',
              ),
            ),
          ]),
        )),
        const SizedBox(height: 14),
        SettingsGroup(title: 'أدوات الصيانة', children: [
          SettingsTile(
            icon: Icons.wallpaper_rounded,
            title: 'إدارة الخلفيات المنشورة',
            subtitle: 'تفتح إدارة المالك الموثّقة لنشر الصور المضمنة أو إخفائها',
            onTap: _openWallpaperManagement,
          ),
          SettingsTile(
            icon: Icons.refresh_rounded,
            title: 'إعادة تحميل مواقيت الصلاة',
            subtitle: 'يجدد بيانات اليوم عند توفر اتصال بالخادم',
            onTap: () {
              ref.invalidate(prayerTimesProvider);
              ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('بدأ تحديث مواقيت الصلاة.')));
            },
          ),
          SettingsTile(
            icon: Icons.notifications_active_outlined,
            title: 'إعادة جدولة إشعارات الصلاة',
            subtitle: 'يعيد إنشاء نافذة التنبيهات وفق الإعدادات الحالية',
            onTap: () async {
              setState(() => _busy = true);
              try {
                final count = await syncPrayerNotifications(ref);
                if (context.mounted)
                  ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(content: Text('تمت جدولة $count إشعاراً.')));
              } catch (error) {
                if (context.mounted)
                  ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(content: Text('فشلت الجدولة: $error')));
              } finally {
                if (mounted) setState(() => _busy = false);
              }
            },
          ),
          SettingsTile(
            icon: Icons.library_books_outlined,
            title: 'فتح سجل المصادر',
            subtitle: 'مراجع المحتوى وحالات التراخيص',
            onTap: () => context.go('/sources'),
          ),
          SettingsTile(
            icon: Icons.verified_user_outlined,
            title: 'الخصوصية وأذونات الموقع',
            subtitle: 'عرض حالة البيانات والموقع',
            onTap: () => context.push('/settings/privacy'),
          ),
        ]),
        const SizedBox(height: 12),
        const SettingsNoticeCard(
          icon: Icons.info_outline_rounded,
          text:
              'هذه بوابة مطوّر محلية وليست نظام مصادقة خادم. حماية البيانات الحساسة أو أوامر الإدارة على الخادم تتطلب تحققاً من جهة الخادم، لأن ملفات التطبيق يمكن تحليلها بعد توزيعها.',
        ),
      ],
    );
  }
}
