import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../features/home/home_screen.dart';
import '../features/sections/sections_screen.dart';
import '../features/qibla/qibla_screen.dart';
import '../features/prayer/prayer_screen.dart';
import '../features/tasbih/tasbih_screen.dart';
import '../features/radio/radio_screen.dart';
import '../features/radio/audio_library_screen.dart';
import '../features/settings/settings_screen.dart';
import '../features/settings/source_catalog_screen.dart';
import '../features/content/content_screen.dart';
import '../features/content/ahl_al_bayt_content_screen.dart';
import '../features/content/wallpapers_screen.dart';
import '../features/content/ziyara_library_screen.dart';
import '../features/content/quran_library_screen.dart';
import '../features/library/book_reader_screen.dart';
import '../features/favorites/favorites_screen.dart';
import '../features/content/quran_text_reader_screen.dart';
import '../features/governorate/governorate_selection_screen.dart';
import '../features/settings/settings_subpages.dart';
import '../features/settings/developer_panel_screen.dart';
import 'app_shell.dart';

final routerProvider = Provider<GoRouter>((ref) {
  return GoRouter(
    initialLocation: '/',
    routes: [
      GoRoute(
        path: '/setup',
        builder: (context, state) => GovernorateSelectionScreen(
          isChangeMode: state.uri.queryParameters['mode'] == 'change',
        ),
      ),
      GoRoute(
        path: '/settings/prayer',
        builder: (context, state) => const PrayerSettingsScreen(),
      ),
      GoRoute(
        path: '/settings/privacy',
        builder: (context, state) => const PrivacyDataScreen(),
      ),
      GoRoute(
        path: '/developer',
        builder: (context, state) => const DeveloperPanelScreen(),
      ),
      ShellRoute(
        builder: (context, state, child) =>
            AppShell(location: state.uri.path, child: child),
        routes: [
          GoRoute(path: '/', builder: (context, state) => const HomeScreen()),
          GoRoute(
              path: '/sections',
              builder: (context, state) => const SectionsScreen()),
          GoRoute(
              path: '/ahl-al-bayt',
              builder: (context, state) => const AhlAlBaytContentScreen()),
          GoRoute(
              path: '/wallpapers',
              builder: (context, state) => const WallpapersScreen()),
          GoRoute(
              path: '/qibla', builder: (context, state) => const QiblaScreen()),
          GoRoute(
              path: '/prayer',
              builder: (context, state) => const PrayerScreen()),
          GoRoute(
              path: '/tasbih',
              builder: (context, state) => const TasbihScreen()),
          GoRoute(
              path: '/settings',
              builder: (context, state) => const SettingsScreen()),
          GoRoute(
              path: '/sources',
              builder: (context, state) => const SourceCatalogScreen()),
          GoRoute(
              path: '/favorites',
              builder: (context, state) => const FavoritesScreen()),
          GoRoute(
              path: '/radio', builder: (context, state) => const RadioScreen()),
          GoRoute(
              path: '/quran',
              builder: (context, state) => const QuranLibraryScreen()),
          GoRoute(
              path: '/quran/:surahNumber',
              builder: (context, state) {
                final surahNumber =
                    int.tryParse(state.pathParameters['surahNumber'] ?? '') ??
                        0;
                final ayahNumber =
                    int.tryParse(state.uri.queryParameters['ayah'] ?? '');
                return QuranTextReaderScreen(
                    surahNumber: surahNumber, initialAyahNumber: ayahNumber);
              }),
          GoRoute(
              path: '/library',
              builder: (context, state) => const ContentStatusScreen(
                  title: 'المكتبة الإسلامية',
                  description: 'كتب العقائد والفقه والتفسير والحديث والسيرة.',
                  contentType: 'book')),
          GoRoute(
              path: '/hadith',
              builder: (context, state) => const ContentStatusScreen(
                  title: 'الروايات والأحاديث',
                  description:
                      'الروايات مع المصدر والسند والتقييم العلمي المستقل.',
                  contentType: 'hadith')),
          GoRoute(
              path: '/duas',
              builder: (context, state) => ContentStatusScreen(
                  title: 'الأدعية',
                  description: 'نصوص مأثورة مع توثيق المصدر.',
                  contentType: 'dua',
                  search: state.uri.queryParameters['search'])),
          GoRoute(
              path: '/visits',
              builder: (context, state) => ZiyaraLibraryScreen(
                  initialSearch: state.uri.queryParameters['search'])),
          GoRoute(
              path: '/visits/:volumeId',
              builder: (context, state) => ZiyaraVolumeTextScreen(
                  volumeId: state.pathParameters['volumeId'] ?? '')),
          GoRoute(
              path: '/events',
              builder: (context, state) => const ContentStatusScreen(
                  title: 'المناسبات الإسلامية',
                  description:
                      'التواريخ المحتملة ومصادرها وبيان مواضع الاختلاف.',
                  contentType: 'event')),
          GoRoute(
              path: '/ethics',
              builder: (context, state) => const ContentStatusScreen(
                  title: 'الأخلاقيات الإسلامية',
                  description: 'دروس منظمة واختبارات وتقدم تعليمي.',
                  contentType: 'ethics_lesson')),
          GoRoute(
              path: '/audio',
              builder: (context, state) => const AudioLibraryScreen()),
          GoRoute(
              path: '/book/:bookId',
              builder: (context, state) {
                final bookId =
                    int.tryParse(state.pathParameters['bookId'] ?? '') ?? 0;
                final extra =
                    state.extra is Map ? state.extra as Map : const {};
                final title = extra['title'] is String
                    ? extra['title'] as String
                    : 'قراءة الكتاب';
                final volumeCount = extra['volumeCount'] is num
                    ? (extra['volumeCount'] as num)
                        .toInt()
                        .clamp(1, 100)
                        .toInt()
                    : 1;
                return BookReaderScreen(
                    bookId: bookId, bookTitle: title, volumeCount: volumeCount);
              }),
        ],
      ),
    ],
  );
});
