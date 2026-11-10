# تقرير الجودة — AL-HADITHI 1.0.18

## فحوصات 1.0.18
- `python tools/source-preflight.py`: 42 فحصاً ناجحاً دون فشل.
- `python -m unittest discover -s scripts/tests -v`: 15 اختباراً ناجحاً، بما في ذلك مطابقة نص Tanzil Simple المرفق وإعادة بناء أصل القرآن المحلي.
- `bash -n tools/validate-apk.sh` و`bash -n tools/diagnose-install.sh`: نجاح فحص الصياغة.
- أضيف استعلام Room مراقب لعدد الآيات المخزنة وربطه عبر `StateFlow` إلى واجهة الإعدادات.
- راجعت توصيل مكونات SettingsScreen مع ViewModel وDataStore، بما فيها إعداد الأهداف اليومية، تعديل التاريخ الهجري، مجاميع التنبيهات، وفترات عدم الإزعاج.
- أضيفت اختبارات Repository لإثبات أن إعادة ضبط الخطوط لا تزيل أهداف المتابعة أو تقدم المستخدم، وأن إعادة الضبط الكاملة تحفظ سجل القراءة والعدّادات والموقع وبيانات المطور؛ لم يمكن تشغيل هذه الاختبارات عبر Gradle في بيئة التحرير.

## فحوص شاشة الإعدادات 1.0.18
- 42/42 فحصاً ساكناً ناجحاً، تتضمن وجود عناصر الإعدادات وربط DataStore/Room، الثيم الفعلي للنظام، عدم وجود أزرار إعادة ترتيب غير موصولة، حصر اللغات والخطوط الفعلية، وتدفق مرفق الدعم عبر SAF.
- تم فحص صياغة Bash لأدوات APK. لم يتم تشغيل مترجم Kotlin/Compose أو JUnit Android بسبب تعذر تنزيل Gradle؛ محاولة kotlinc المعزولة دون Android/Compose classpath لا تعدّ اختبار تجميع.

## حدود الاختبار
فشلت محاولة `./gradlew :app:testDebugUnitTest --offline --no-daemon` قبل دخول Gradle بسبب محاولة تنزيل `gradle-9.3.1-bin.zip` ثم `UnknownHostException: services.gradle.org`. لا يتوفر Android SDK/ADB/apksigner/zipalign أو keystore أصلي. لم يتم بناء APK، ولم يتم تشغيل Kotlin compiler أو JUnit/Robolectric، ولم يُختبر التطبيق على جهاز حقيقي. لا تعتبر نتائج Python/static preflight بديلاً عن Android build أو اختبار تشغيل.

## الوظائف غير المهيأة صراحةً
- لغة التطبيق الحالية: العربية فقط؛ بقية اللغات تحتاج ترجمة كاملة وموارد strings مستقلة.
- إشعارات الحديث اليومي والدعاء اليومي: لا توجد خدمة push/جدولة محتوى موثوقة مهيأة في هذا الإصدار.
- الدعم الفني: النموذج يفتح Android Share Sheet فقط، ولا يوجد backend أو بريد/رابط دعم مؤكد.
- سياسة الخصوصية وشروط الاستخدام: لا توجد وثائق عامة وروابط منشورة مهيأة في المشروع الحالي.
- إدارة الملفات: يعرض التطبيق عدد آيات Room وحالة صوت أذان مخصص، ولا يدعي وجود مدير تنزيلات شامل لكل المحتوى.
- الحساب الشخصي: تبقى إدارة مصادقة Firebase في مسار لوحة المطور الحالية؛ لا توجد صفحة ملف شخصي عامة مرتبطة بحساب المستخدم.

# تقرير الجودة — AL-HADITHI 1.0.17

## فحوصات 1.0.17 المنفذة

- `python tools/source-preflight.py`: **28 بنداً ناجحاً، 0 إخفاق**.
- `python -m unittest discover -s scripts/tests -v`: **15 اختبار Python ناجحاً** (مستورد القرآن، سلامة المصدر والبصمات).
- `python scripts/verify_location_access.py`: **12/12** من ضوابط الموقع.
- `bash -n tools/validate-apk.sh tools/diagnose-install.sh`: نجاح تحليل shell.
- Manifest وملفات Adaptive Icon XML: اجتازت XML parsing.
- Firebase Android package في `google-services.json` مطابق لـ `applicationId`.
- `applicationId` و`namespace` محفوظان، و`minSdk=24` و`targetSdk=36` و`versionCode=19`.
- لا توجد إعدادات ABI splits أو keystore أو APK/AAB ضمن حزمة المصدر.
- فحص بحثي ساكن لم يجد كلمة مرور ثابتة في ملفات Kotlin/XML؛ مسار Firebase Email/Password يحتفظ بكلمة المرور في الذاكرة أثناء الطلب فقط ولا يسجلها.
- صورة العلامة المرفقة مستخدمة في أيقونة التطبيق التكيفية وطبقة monochrome وأيقونات raster.
- أزيلت صورة «حول التطبيق»، وخيارات الخطوط وحجمها موجودة في مجموعة واحدة.
- اختبارات محاكاة لـ `tools/validate-apk.sh`: قبول APK مفرد صالح بنيوياً ورفض split و`debuggable=true`؛ واختبار محاكاة لمسار `adb install -r`.
- فحص صياغة Bash بعد آخر تعديل: ناجح.

## الفحوصات غير المنفذة والسبب

- `./gradlew :app:testDebugUnitTest` و`./gradlew :app:assembleDebug`: بدأ Wrapper بطلب Gradle 9.3.1 ثم فشل بـ `UnknownHostException: services.gradle.org`.
- Android SDK وADB و`apksigner` و`zipalign` و`aapt` و`apkanalyzer` غير مثبتة في بيئة التحرير.
- لا يتضمن المصدر keystore المالك، ولا يوجد APK إصدار سابق لمقارنة التوقيع.
- لم يتوفر Poco X7 Pro متصلاً عبر ADB؛ لا يوجد رمز `INSTALL_FAILED_*` أو سجل Package Manager من المحاولة السابقة.
- لم يتم نشر Firebase Rules أو إنشاء حساب المالك؛ مصادقة البريد تتطلب إعداد Firebase الفعلي.

## النتيجة

هذه حزمة **مصدر 1.0.17** وليست APK جاهزاً. تم إصلاح خطورة إصدار Release غير موقّع بصمت بإضافة فحص إلزامي لمتغيرات التوقيع. لم يتم إثبات نجاح تجميع Kotlin/Compose أو تثبيت APK أو تحديث التطبيق على جهاز حقيقي. لا تحذف النسخة المثبتة ولا تمسح بياناتها أثناء جمع رمز الخطأ.

## أدوات التحقق عند توفر بيئة Android

```bash
python tools/source-preflight.py
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
./tools/validate-apk.sh app/build/outputs/apk/release/app-release.apk
./tools/diagnose-install.sh app/build/outputs/apk/release/app-release.apk
```

---

