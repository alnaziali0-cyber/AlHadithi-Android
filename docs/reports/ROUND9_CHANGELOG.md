# ROUND 9 — واجهة الهوية، الأقسام، القبلة، الأذان، واختيار الموقع المدمج

## ما تم تنفيذه

### الواجهة والخلفية
- أضيفت صورة المسجد ذات القباب البيضاء المرفقة إلى أصول التطبيق واستُخدمت كخلفية عامة في الوضع الفاتح.
- أبقي شعار `khazain_logo.jpg` كما هو دون تعديل، وتحوّل عرضه في الرئيسية إلى حجم صغير بجانب اسم التطبيق.
- أضيف التاريخ الهجري الحسابي إلى الترويسة، مع وسم صريح بأنه تقريبي بسبب اختلاف ثبوت الهلال محلياً.
- نُقلت شبكة الأقسام من الرئيسية إلى شاشة مستقلة `/sections`.
- أصبحت الرئيسية مختصرة: هوية التطبيق، التاريخ، مواقيت اليوم المتاحة، الصلاة القادمة والعد التنازلي، مشغل الأذان المحلي، واختصارات محدودة.

### التنقل
- أضيفت وجهات سفلية مستقلة: الرئيسية، الأقسام، الصلاة، القبلة، التسبيح، الإعدادات.
- أضيف مسار `/qibla` ومسار `/adhan` ومسار `/sections`.

### اختيار المحافظة
- أزيل تحويل GoRouter التلقائي لأول تشغيل إلى `/setup`.
- تعرض الرئيسية لوحة اختيار المحافظة كـ Bottom Sheet إلزامية عند أول تشغيل، وتمنع إغلاقها قبل التأكيد.
- تغيير الموقع لاحقاً من الرئيسية أو الإعدادات يستخدم اللوحة المدمجة نفسها.
- بقيت بيانات المحافظات والتحديد التلقائي والتخزين الآمن الموجودة في Round 8 دون استبدال.

### إعدادات التطبيق
- أعيد تنظيم الإعدادات إلى أزرار شبكية واضحة للمظهر، حجم الخط، الموقع، مواقيت الصلاة، الحساب، القبلة، الأذان، المفضلة، المصادر، والخصوصية.
- بقيت خيارات المظهر والخط والحساب وتعديل المواقيت والاهتزاز مرتبطة بالـ providers السابقة.

### السبحة
- أعيد تصميم عداد السبحة إلى زر دائري تفاعلي كبير مع حلقة تقدم، أهداف، أذكار جاهزة، ذكر مخصص، واهتزاز اختياري محفوظ.
- استمر حفظ العدد والهدف والذكر محلياً.

### القبلة
- أضيف حساب زاوية القبلة هندسياً إلى الكعبة من إحداثيات الموقع المختار.
- أضيفت واجهة بوصلة تعتمد `flutter_compass` وتعرض الفرق بين اتجاه الهاتف وزاوية القبلة. الحساب الجغرافي من الشمال الحقيقي مقابل مستشعر مغناطيسي؛ لم يضف تصحيح الانحراف المغناطيسي، ويظهر تنبيه بعدم اعتبار السهم قياساً دقيقاً.
- تستخدم البوصلة GPS المحفوظ بموافقة المستخدم، أو إحداثيات المدينة/المحافظة التقريبية عند عدم توفر GPS.
- تعرض حالة عدم توفر المستشعر بدلاً من إظهار اتجاه مضلل.

### ملف الأذان
- أضيف الملف المقدم من المستخدم إلى `assets/audio/adhan/`، وربط بصفحة مشغل مستقلة وبطاقة تشغيل في الرئيسية وأزرار من الصلاة والإعدادات.
- بيانات الملف الوصفية تحمل عنوان «أذان العتبة الحسينية – القارئ عادل الكربلائي»، والألبوم «صوتيات درر العراق»، والمدة نحو 4:55. هذه بيانات داخل الملف وليست تحققاً مستقلاً من الجهة المنتجة أو مالك الحقوق.
- التشغيل يدوي عند ضغط المستخدم. لم يتم ادعاء الجدولة التلقائية عند مواقيت الصلاة أو التشغيل في الخلفية.
- حقوق إعادة التوزيع لم تُثبت؛ أضيف توثيق واضح للمصدر كما قدمه مالك المشروع وضرورة الحصول على إذن قبل النشر العام.

### مصادر المحتوى الشيعي
- توسّع فهرس المصادر بثلاثة مراجع: Al-Islam.org، Shia Library (Ghadir Project)، وShia Research Library.
- سُجلت روابط الشروط وقيود الحقوق داخل فهرس الخادم.
- لم يتم كشط قواعد بيانات أو نسخ مكتبات كاملة؛ شروط بعض المصادر تمنع إعادة توزيع المحتوى أو جمعه آلياً دون إذن. لذلك تُستخدم هذه المصادر كروابط مرجعية إلى حين الحصول على موافقات مستقلة لكل محتوى.

## الملفات الجديدة
- `apps/mobile/assets/branding/mosque_background.jpg`
- `apps/mobile/assets/audio/adhan/adhan_al_ataba_adil_alkarbalaei.mp3`
- `apps/mobile/assets/audio/adhan/README_AR.md`
- `apps/mobile/lib/core/hijri_date.dart`
- `apps/mobile/lib/core/qibla_math.dart`
- `apps/mobile/lib/core/audio_providers.dart`
- `apps/mobile/lib/core/governorate_selection_sheet.dart`
- `apps/mobile/lib/features/sections/sections_screen.dart`
- `apps/mobile/lib/features/qibla/qibla_screen.dart`
- `apps/mobile/lib/features/audio/adhan_screen.dart`
- `docs/reports/ROUND9_VALIDATION_REPORT.md`
- `apps/mobile/test/core/hijri_date_test.dart`
- `apps/mobile/test/core/qibla_math_test.dart`
- `docs/PLATFORM_COMPASS_AUDIO_SETUP.md`

## الملفات المعدلة
- `apps/mobile/pubspec.yaml`
- `apps/mobile/lib/app/theme.dart`
- `apps/mobile/lib/app/khazain_app.dart`
- `apps/mobile/lib/app/app_shell.dart`
- `apps/mobile/lib/app/router.dart`
- `apps/mobile/lib/features/home/home_screen.dart`
- `apps/mobile/lib/features/governorate/governorate_selection_screen.dart`
- `apps/mobile/lib/features/settings/settings_screen.dart`
- `apps/mobile/lib/features/content/content_screen.dart`
- `apps/mobile/lib/features/sections/sections_screen.dart` (أضيف رابط فهرس المصادر إلى الأقسام)
- `apps/mobile/lib/features/settings/source_catalog_screen.dart`
- `apps/mobile/lib/features/tasbih/tasbih_screen.dart`
- `apps/mobile/lib/features/prayer/prayer_screen.dart`
- `services/api/app/data/source_catalog.json`
- `services/api/tests/test_api.py`

## ما لم يكتمل
- لم يتم تنفيذ جدولة الأذان الآلية أو الإشعارات المحلية.
- لم يتم تنفيذ تشغيل الخلفية/شاشة القفل للأذان.
- لم يتم إنشاء مجلدي Android وiOS، وهما غير موجودين في المصدر الوارد. لذلك لم تُضف الأذونات الأصلية فعلياً.
- التاريخ الهجري تقويمي حسابي تقريبي وليس إثباتاً شرعياً لبداية الشهر في العراق.
- ما زال حساب العصر غير متاح في Backend حسب فجوة Round 8؛ لم تُخترع قيمة.
- لم تُستورد كتب أو روايات أو تسجيلات من مواقع خارجية دون ترخيص. أضيفت بيانات مصادر وشروطها فقط.
- لا تزال موافقة صاحب حقوق ملف الأذان المقدم مطلوبة قبل إعادة توزيع الحزمة للعامة.

## التحقق
- نتيجة سلامة الأرشيف الوارد: اجتاز اختبار ZIP قبل التعديل.
- حزمة البوصلة المختارة وفق pub.dev: `flutter_compass 0.8.1`.
- يجب تشغيل `flutter pub get`, `dart format`, `flutter analyze`, `flutter test`، وفحوص Android/iOS بعد تجهيز SDK وملفات المنصة. لم يتم الادعاء باجتيازها في هذه البيئة.


## تقرير التحقق النهائي

راجع `docs/reports/ROUND9_VALIDATION_REPORT.md` لنتائج الاختبارات والقيود. اختبارات Backend الحالية: 31/31 ناجحة، و`compileall` وYAML/JSON والأصول وفحص الأقواس/الاستيرادات النسبية اجتازت الفحوص المحددة. لم تتوفر Flutter/Dart، لذلك لم يتم تشغيل analyzer أو Flutter tests أو بناء APK/IPA.
