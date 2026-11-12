# التقرير الهندسي — AL-HADITHI 1.0.19 (Build 21)

## الملخص

أضيفت قنوات التواصل التي قدمها مالك المشروع إلى مجموعة «الدعم والمساعدة» داخل تطبيق Android الأصلي Kotlin/Jetpack Compose. لم يتم إنشاء مشروع أو شاشة مستقلة، وتم الحفاظ على بنية الإعدادات الحالية.

## القنوات

| القناة | العنوان المعتمد | الفعل عند الضغط |
|---|---|---|
| البريد | `alhadithiiraq@gmail.com` | `ACTION_SENDTO` إلى `mailto:` مع موضوع افتراضي |
| Instagram | `@alhadithi_iraq` | فتح `https://www.instagram.com/alhadithi_iraq/` |
| TikTok | `@.al.hadithi` | فتح `https://www.tiktok.com/@.al.hadithi` |

رابط Instagram جرى تطبيعه من الرابط المقدم بإزالة معامل التتبع `cplk` والإبقاء على اسم الحساب. رابط TikTok أضيف إليه مخطط `https://` مع الحفاظ على المسار والمعرف.

## تغييرات المصدر

1. `app/src/main/java/com/example/domain/SupportChannels.kt` — مصدر مركزي لبيانات الاتصال، ودالة تحقق allow-list صارمة لروابط HTTPS.
2. `app/src/main/java/com/example/ui/screens/SettingsScreen.kt` — إضافة ثلاثة صفوف اتصال بأيقونات Email وCameraAlt وMusicNote، وربط كل صف بـ Android Intent مناسب، ومعالجة حالة عدم وجود تطبيق مستلم برسالة للمستخدم.
3. `app/src/test/java/com/example/domain/SupportChannelsTest.kt` — اختبارات JUnit لعناوين الاتصال وروابط الحسابات والرفض الأمني.
4. `app/build.gradle.kts` — رفع `versionCode` من 20 إلى 21 و`versionName` من 1.0.18 إلى 1.0.19.
5. `tools/source-preflight.py` وREADME وCHANGELOG وQA — تحديث فحوص الإصدار والتوثيق.

## أيقونات وتجربة الاستخدام

- البريد: `Icons.Outlined.Email`.
- Instagram: `Icons.Outlined.CameraAlt` كرمز كاميرا واضح.
- TikTok: `Icons.Outlined.MusicNote` كرمز موسيقي.
- كل صف يبين اسم القناة ومعرفها، ويملك وصفاً دلالياً للأيقونة ومؤشر فتح.
- تستخدم الأيقونات Material Icons Extended الموجودة بالفعل ضمن اعتماديات التطبيق؛ لم تتم إضافة مكتبة جديدة. <https://developer.android.com/develop/ui/compose/resources>

## السلوك والأمان

- البريد يستخدم `ACTION_SENDTO` و`mailto:`؛ لا يرسل الرسالة تلقائياً، بل يفتح تطبيق البريد ليختار المستخدم ويتحقق قبل الإرسال. لا يتطلب صلاحية جهات الاتصال أو صلاحية جديدة.
- روابط الشبكات تستخدم HTTPS فقط مع مطابقة المضيف والمسار المحددين. ترفض الدالة الروابط ذات المخطط HTTP، أو host مشابه، أو user-info، أو port غير قياسي، أو query أو fragment، أو مسار حساب آخر.
- عند غياب تطبيق/متصفح قادر على معالجة Intent، تظهر رسالة واضحة بدلاً من تعطل الشاشة.
- الروابط العامة لا تحتاج Backend. نماذج البلاغات القديمة لا تزال مشاركة يدوية في Android وليست نظام تذاكر أو تخزيناً سحابياً.

## التوافق والمحافظة على المشروع

- بقي `applicationId=com.aistudio.alhadithi.iqshia` و`namespace=com.example` و`minSdk=24` و`targetSdk=36`.
- لم تتغير Room أو DataStore أو Firebase أو قواعد التفضيلات.
- تم رفع الإصدار إلى `versionCode=21` و`versionName=1.0.19`.
- لا يوجد APK أو keystore جديد في الحزمة.

## نتائج الفحص

- Source preflight: **48/48 PASS**.
- Kotlin compiler smoke tests لملف إعدادات الروابط: **9/9 PASS**.
- أضيفت 4 اختبارات JUnit إلى المشروع، لكن تعذر تشغيل Gradle Wrapper؛ فشل DNS عند تنزيل `gradle-9.3.1-bin.zip` من `services.gradle.org` (`UnknownHostException`).
- لا يتوفر Android SDK/جهاز ADB في هذه الجلسة، ولذلك لم يتم اختبار فتح تطبيقات التواصل على جهاز أو بناء APK.

## ما لم يُدّعَ اكتماله

- لا يوجد Backend لاستقبال البلاغات أو تذاكر الدعم.
- لا يوجد تحقق ميداني من أن الحسابات الاجتماعية مفتوحة/متاحة من هاتف المستخدم؛ الحسابات أضيفت كما قدمها المستخدم.
- لم يتم بناء APK أو توقيعه أو اختباره على Android بسبب تعذر تنزيل Gradle وغياب Android SDK/keystore.
