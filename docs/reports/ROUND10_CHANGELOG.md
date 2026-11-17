# ROUND 10 — الجدولة المحلية، الخط القرآني، ومراجعة المصادر

تاريخ العمل: 14 نوفمبر 2026

## ما أضيف ونُفذ في الشفرة

### تنبيهات مواقيت الصلاة
- أضيفت `flutter_local_notifications: ^19.5.0` و`timezone: ^0.10.1`، مع خدمة تهيئة محلية وطلب إذن الإشعار عند موافقة المستخدم من صفحة الإعدادات.
- أضيفت جدولة نافذة متجددة مدتها 14 يوماً (56 إشعاراً كحد أقصى) لتجنب تجاوز سقف iOS البالغ 64 إشعاراً معلّقاً.
- الأوقات المجدولة هي الفجر والظهر والمغرب والعشاء وفق مفاتيح البيانات المتاحة من محرك الحساب الحالي؛ لم يُخترع وقت للعصر.
- تأخذ الجدولة فروقات الدقائق المحلية، وتختار المنطقة الزمنية من المدينة/المحافظة، وتعيد التحديث عند عودة التطبيق أو تغير مواقيت اليوم/الموقع/طريقة الحساب/الفروقات.
- يفضّل exact alarms إذا منح النظام الإذن؛ وإلا تستخدم الخدمة الجدولة غير الدقيقة. تُلغى الإشعارات عندما يوقف المستخدم الخيار.
- أضيفت اختبارات وحدة لبناء جدول التنبيهات وتخزين الخيار.

**حد واضح:** الإشعار يستخدم صوت تنبيه النظام. لا يشغّل ملف MP3 كاملاً عند إغلاق التطبيق أو قفل الشاشة. ذلك يحتاج خدمة أصلية لكل منصة واختبارات وسياسات إضافية. كما أن Android/iOS غير موجودين في الأرشيف الوارد، لذلك أضيف ملف تعليمات تكامل منفصل ولم يتم الادعاء ببناء APK/IPA أو فحص جهاز حقيقي.

### القرآن والخط
- أضيف `AmiriQuran.ttf` إلى أصول Flutter وسُجل في `pubspec.yaml`، مع ملف رخصة SIL OFL 1.1 وحقوق المؤلف.
- أصبح عارض الآيات يستخدم الخط القرآني بحجم وتباعد مناسبين للرسم والضبط.
- أضيفت بطاقة مصدر Tanzil ورابطه وشروط النقل الحرفي في واجهة القرآن.
- **لم يُضمّن نص المصحف في هذه الجولة** لأن ملف النص/البنية المطلوب للاستيراد لم يكن ضمن ملفات المشروع، وأداة الاستيراد تشترط 6,236 آية و114 سورة ومراجعاً مؤهلاً واعتماداً مسجلاً. لذلك بقي حاجز منع العرض قائماً حتى لا يُعرض نص غير مراجع.

### الكتب والصوتيات والحقوق
- أضيفت إلى سجل المصادر: Tanzil، Amiri Quran، OpenITI، وAQQD.
- OpenITI مسجل كرخصة CC BY-NC-SA 4.0 مع قيد الاستخدام غير التجاري ومراجعة سجل كل نص/طبعة.
- AQQD مسجل كمجموعة بحثية تعلن CC0 1.0 لمقاطع قصيرة، لكن المصدر وسلسلة الحقوق لكل تسجيل يجب فحصهما؛ لم تُنسخ أي مقاطع صوتية إلى التطبيق.
- لا تزال إذاعات ومكتبات العتبات مصادر تواصل فقط إلى حين موافقة خطية خاصة بكل مادة.
- لم تُغيّر أو تُعدّل أو تُحذف نسخة MP3 التي قدمها مالك المشروع؛ حقوق إعادة توزيعها غير مثبتة.

## الملفات الجديدة
- `apps/mobile/assets/fonts/AmiriQuran.ttf`
- `apps/mobile/assets/fonts/AMIRI_OFL_LICENSE.txt`
- `apps/mobile/lib/core/prayer_notification_schedule.dart`
- `apps/mobile/lib/core/prayer_notification_service.dart`
- `apps/mobile/lib/core/prayer_notification_sync.dart`
- `apps/mobile/test/core/prayer_notification_schedule_test.dart`
- `docs/PLATFORM_NOTIFICATIONS_SETUP_AR.md`
- `docs/LICENSING_AND_CONTENT_POLICY_AR.md`
- `docs/reports/ROUND10_CHANGELOG.md`
- `docs/reports/ROUND10_VALIDATION_REPORT.md`

## الملفات المعدلة الرئيسية
- `apps/mobile/pubspec.yaml`, `lib/main.dart`, `lib/app/khazain_app.dart`
- `apps/mobile/lib/core/app_preferences.dart`, `providers.dart`
- `apps/mobile/lib/features/settings/settings_subpages.dart`, `settings_screen.dart`, `source_catalog_screen.dart`
- `apps/mobile/lib/features/content/content_screen.dart`, `quran_reader_screen.dart`
- سجلات المصادر JSON/CSV في `services/api`, `content/reference`, و`docs/reference/requirements-pack/02_SOURCES`
- اختبار كتالوج المصادر في `services/api/tests/test_api.py`

## القيود المفتوحة قبل النشر العام
1. توليد مجلدي Android/iOS ودمج الأذونات والمستقبلات/المستمعين كما في `docs/PLATFORM_NOTIFICATIONS_SETUP_AR.md`.
2. بناء APK/IPA وفحص `flutter analyze`, `flutter test` وتجارب الإشعارات بعد إعادة التشغيل على أجهزة فعلية.
3. تسليم نص Tanzil الرسمي مع البيانات البنيوية إلى المراجع ثم تسجيل اعتماد SHA-256 قبل تفعيل قارئ المصحف.
4. الحصول على ملفات أذان/تلاوات/مجالس كاملة من أصحاب الحقوق أو التحقق من رخصة كل ملف قبل إرفاقه.
5. اعتماد مواقيت الصلاة محلياً، واستكمال وقت العصر من مصدر جعفري موثق عند توفره.
