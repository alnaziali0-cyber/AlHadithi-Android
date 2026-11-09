# التقرير الهندسي — AL-HADITHI 1.0.17 (Build 19)

## 1) الملخص التنفيذي

تم فحص مصدر Android المرفق في `AlHadithi-1.0.16-source.zip`، وإجراء تعديلات على هوية التطبيق وإعداداته وحماية تغليف Release، وإضافة أدوات تشخيص قابلة للتشغيل على جهاز Android عند توفر Platform Tools.

**النتيجة:** حزمة مصدر محدثة، وليست APK. لم يمكن بناء أو توقيع APK أو تثبيته في بيئة التحرير؛ كما لا يمكن تحديد رمز خطأ محاولة التثبيت السابقة دون APK الذي فشل أو سجل Package Manager من الهاتف.

## 2) أدلة فحص بنية Android

| الخاصية | نتيجة الفحص |
|---|---|
| التقنية | Android Native — Kotlin + Jetpack Compose + Material 3 |
| `applicationId` | `com.aistudio.alhadithi.iqshia` |
| `namespace` | `com.example` |
| Firebase package | مطابق لـ `applicationId` في `app/google-services.json` |
| `minSdk` | 24 |
| `targetSdk` | 36 |
| `compileSdk` | API 36 minor 1 |
| `versionName` | 1.0.17 |
| `versionCode` | 19 (أعلى من 18 في مصدر 1.0.16) |
| Gradle Wrapper | 9.3.1 |
| Android Gradle Plugin | 9.1.1 |
| Kotlin Gradle Plugin | 2.2.10 |
| ABI splits | غير مفعلة؛ لم يُضبط `abiFilters` أو `splits` |
| `testOnly` | غير موجود في Manifest المصدر |
| Release debuggable | `false` صراحةً |
| Release keystore | غير مرفق، ولا توجد قيم توقيع ثابتة في المصدر |

لم أغير `applicationId` أو `namespace` أو `minSdk` أو `targetSdk` أو تقنية المشروع. معرّف الحزمة في إعداد Firebase يطابق معرّف التطبيق، فلا يوجد دليل على mismatch في هذا الجانب.

## 3) سبب App not installed — ما أمكن إثباته وما لم يمكن

### خطر تغليف فعلي اكتُشف وأُصلح

كان `app/build.gradle.kts` يسمح بمتابعة مهمة Release حتى عندما لا تتوفر متغيرات توقيع Release؛ في هذه الحالة قد ينتج ملف APK غير موقع. هذا عيب في حماية خط الإصدار، ويمكن أن يؤدي إلى عدم قبول Package Manager لملف التوزيع.

تمت إضافة مهمة `verifyReleaseSigningInputs` وربطها بمهام `packageRelease` و`assembleRelease` و`bundleRelease`. يتطلب Release وجود الملف الحالي عبر `KEYSTORE_PATH` وإعداد `STORE_PASSWORD` و`KEY_PASSWORD` في بيئة البناء. `KEY_ALIAS` اختياري وقيمته الافتراضية `upload`. لا ينشئ النظام keystore جديداً ولا يضمّن مفاتيح التوقيع أو كلمات مرورها.

### السبب الأصلي لمحاولة الهاتف غير مثبت

لا تحتوي الملفات المتاحة على:

- ملف APK الذي ظهرت له رسالة `App not installed`.
- APK سابق أو `base.apk` من النسخة المثبتة لمقارنة شهادة التوقيع.
- keystore المستخدم في الإصدار الذي ثُبّت سابقاً.
- مخرجات `adb install -r` أو رمز `INSTALL_FAILED_*` من Poco X7 Pro.
- اتصال ADB بجهاز حقيقي أو Android SDK/Build Tools.

لذلك لا يمكن الجزم إن كانت المحاولة السابقة بسبب توقيع غير متطابق، APK غير موقع، downgrade، ABI، SDK، أو خطأ Parse. لم يُستخدم `adb uninstall` أو `pm clear`، ولم تُمسح بيانات المستخدم.

### لماذا لم أغيّر Gradle/SDK؟

تكوين المشروع متناسق مع مصفوفة التوافق المنشورة: AGP 9.1.1 يتطلب Gradle 9.3.1 وKGP 2.2.10، ويدعم API 37 وما دون. فشل الاستدعاء الفعلي لـ `./gradlew --version` وقع أثناء محاولة تنزيل `https://services.gradle.org/distributions/gradle-9.3.1-bin.zip` برسالة `UnknownHostException: services.gradle.org`، أي قبل بدء Gradle أو ترجمة Kotlin. لذلك لم أخفض الإصدارات بشكل تخميني.

## 4) الإصلاحات والتغييرات

### تغليف التطبيق

- رفع `versionCode` من 18 إلى 19، و`versionName` من 1.0.16 إلى 1.0.17.
- إبقاء `applicationId` والـ `namespace` كما هما.
- الإبقاء على min/target SDK والقيم الأصلية.
- تثبيت `isDebuggable=false` في Release.
- فشل واضح ومبكر إذا طُلب Release من دون إعداد مفتاح توقيع قابل للقراءة.
- لا توجد إعدادات splits؛ المقصود إخراج APK واحد من `assembleRelease` عند اكتمال بيئة البناء.

### الواجهة والهوية

- تكييف الصورة المرفقة إلى علامة شفافة عالية الدقة لأيقونة التطبيق.
- تحديث Adaptive Icon وخلفيتها وطبقة Android Monochrome.
- تحديث صور أيقونة raster للأجهزة قبل API 26.
- ضبط لوحة الوضع الداكن إلى خلفيات خضراء داكنة وسطح فحمي ولمسات زمردية قريبة من الصورة المرجعية، مع بقاء الوضع الفاتح.
- إعادة تنظيم ترويسة مجموعات الإعدادات بأيقونات وعناوين موحدة.
- جمع خط الواجهة وخط القراءة وخيارات حجم النص والتباعد في مجموعة «الخطوط وحجم النص».
- حذف صورة «حول التطبيق» من الشاشة وإزالة الملف الأبيض غير المستخدم لتقليل الموارد.

### دخول لوحة المطوّر

- تهيئة البريد المصرح به مسبقاً في حقل البريد لتقليل الكتابة.
- إزالة فتح اختيار Google التلقائي عند دخول اللوحة؛ أصبح Email/Password هو المسار الافتراضي، مع بقاء زر Google اختيارياً.
- كلمة المرور لا تُخزّن ولا تُكتب في Kotlin أو Gradle أو ملفات المشروع.
- التحقق يبقى عبر Firebase Authentication وبريد موثق، كما تقيد قواعد Firestore عمليات المطوّر بالبريد الموثق.
- لا يمكن من حزمة المصدر وحدها إنشاء حساب Firebase أو تعيين كلمة مروره أو تفعيل مزود Email/Password؛ يجب إجراء ذلك في مشروع Firebase الذي يطابق `google-services.json`.
- كلمة المرور التي أُرسلت في المحادثة ينبغي تدويرها قبل التوزيع، لأنها لم تعد سراً.

### دعم اللغات

واجهة التطبيق الحالية ذات نصوص عربية كثيرة مكتوبة مباشرة داخل Kotlin. لم أضف اختياراً شكلياً للغات غير مترجمة؛ العربية هي اللغة المدعومة فعلياً في هذا المصدر. الترجمة الكاملة تحتاج نقل جميع النصوص إلى Android string resources وإضافة موارد `values-en` ثم اختبار اتجاه الواجهة.

## 5) أدوات التشخيص الجديدة

- `tools/source-preflight.py`: فحص ثابت لمعرّف التطبيق، SDK، Firebase package، الأيقونة، Manifest، غياب كلمة مرور ثابتة، ووجود حماية توقيع Release.
- `tools/validate-apk.sh`: يتحقق من بنية ZIP والـ Manifest وDEX والموارد والتوقيع `apksigner` والمحاذاة `zipalign` ومعرّف الحزمة وSDK، ويرفض split APK و`testOnly=true` و`debuggable=true`.
- `tools/diagnose-install.sh`: يجمع SDK وABI ويعرض معلومات الحزمة الحالية بشكل قراءة فقط، ويحاول مقارنة شهادة APK الوارد بشهادة `base.apk` المثبت، ثم يستخدم `adb install -r` لالتقاط رمز Package Manager دون إزالة التطبيق أو مسح بياناته.
- `INSTALL_DIAGNOSTICS_AR.md`: يشرح رموز `INSTALL_FAILED_*` والخطوة الصحيحة لكل حالة.

## 6) نتائج التحقق المنفذة

- `python tools/source-preflight.py`: **28 checks passed, 0 failed**.
- `python -m unittest discover -s scripts/tests -v`: **15 Python tests passed**.
- `python scripts/verify_location_access.py`: **12/12 checks passed**.
- `bash -n tools/validate-apk.sh tools/diagnose-install.sh`: نجح فحص صياغة Bash.
- اختبارات محاكاة لأداة APK: قبلت بنية APK مفرداً صحيح البنية، ورفضت Manifest يحوي split أو `debuggable=true`؛ واختبار محاكاة لـ ADB أكد أن مسار `install -r` لا يستدعي إزالة التطبيق أو مسح البيانات. هذه اختبارات للسكربتات وليست اختباراً على Poco.
- XML parsing لملفات الأيقونات والـ Adaptive Icon: ناجح.
- فحص تواجد شعار التطبيق والأيقونة الأحادية والرستر: ناجح.
- فحص عدم وجود كلمات مرور ثابتة في ملفات Kotlin/XML: لم يجد تعييناً حرفياً لكلمة مرور.
- لم يُعثر على ملف `.jks` أو `.keystore` أو APK/AAB داخل حزمة المصدر.

## 7) الفحوصات التي لم تُنفذ

- لا Gradle distribution cache: Wrapper حاول التنزيل وفشل DNS.
- لا `ANDROID_HOME` أو `ANDROID_SDK_ROOT`، ولا أدوات `adb` و`apksigner` و`zipalign` و`aapt` و`apkanalyzer`.
- لذلك لم تُنفذ اختبارات Gradle/JUnit أو `assembleDebug` أو `assembleRelease`.
- لم يتم توقيع APK ولم تُفحص شهادة APK نهائي.
- لم يُختبر Clean Install أو Update Install أو تشغيل التطبيق على Poco X7 Pro.
- لا يمكن اختبار Firebase Auth/Rules أو إنشاء المستخدم من دون إعداد Firebase فعلي وصلاحياته.

## 8) تعليمات المتابعة الآمنة

1. احتفظ بنسخة بيانات التطبيق المثبت على الهاتف ولا تحذفه.
2. جهز keystore الأصلي الذي وقّعت به النسخة المثبتة؛ لا تنشئ مفتاحاً جديداً لتحديث نفس `applicationId`.
3. ابنِ Debug للاختبار، ثم Release بعد ضبط متغيرات التوقيع من بيئة آمنة.
4. شغّل `tools/validate-apk.sh` على APK الناتج.
5. صِل Poco X7 Pro وشغّل `tools/diagnose-install.sh`. احتفظ بسطر `INSTALL_FAILED_*` كاملاً.
6. إذا كان التوقيع غير متطابق، لا تحذف التطبيق قبل تصدير بيانات المستخدم وموافقة المالك؛ تغيير المفتاح قد يجعل التحديث غير ممكن مع الحفاظ على البيانات.
7. اختبر Clean Install على محاكي منفصل، ثم Update Install على جهاز الاختبار الذي يحتوي النسخة السابقة، ثم اختبر التشغيل والموارد والصوت والتنقل.

**حالة التسليم:** Source Release 1.0.17 فقط. لا يوجد APK موقّع ضمن هذه الحزمة، ولا ادعاء بنجاح تثبيت فعلي.
