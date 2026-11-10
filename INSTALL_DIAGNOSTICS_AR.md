# دليل تشخيص تثبيت AL-HADITHI على Android

هذا الدليل لا يحذف التطبيق الحالي ولا يمسح بياناته. لا تستخدم `adb uninstall` أو `pm clear` أثناء التشخيص. أداة المقارنة تسحب `base.apk` مؤقتاً إن سمحت صلاحيات ADB، ولا تعدّل ملفات التطبيق المثبت.

## بيانات الحزمة التي يجب الحفاظ عليها

- `applicationId`: `com.aistudio.alhadithi.iqshia`
- `namespace`: `com.example` (مساحة أسماء Kotlin/موارد، ولا تستبدل `applicationId` بها)
- `minSdk`: 24
- `targetSdk`: 36
- إصدار المصدر الحالي: `1.0.18`, `versionCode=20`
- لا توجد ABI splits في إعدادات التطبيق؛ الناتج المقصود APK واحد شامل للـ ABIs التي تحتويها المكتبات المضمّنة.

## سبب خطر تغليف تم إصلاحه

كان إعداد Release يسمح بترك `signingConfig` غير معيّن إذا لم تتوفر متغيرات التوقيع، ما قد ينتج APK غير موقّع. أضيفت مهمة `verifyReleaseSigningInputs` لإيقاف `packageRelease`/`assembleRelease`/`bundleRelease` قبل التغليف عندما لا يتوفر keystore قابل للقراءة مع كلمات المرور من متغيرات البيئة. لا ينشئ المشروع مفتاحاً جديداً ولا يضمّن أسرار التوقيع.

متغيرات بيئة البناء المطلوبة فقط:

- `KEYSTORE_PATH`
- `STORE_PASSWORD`
- `KEY_PASSWORD`
- `KEY_ALIAS` (اختياري؛ القيمة الافتراضية `upload`)

لا تضع هذه القيم في ملفات Kotlin أو Gradle أو Git أو داخل APK.

## فحص APK بعد البناء

```bash
./tools/validate-apk.sh app/build/outputs/apk/release/app-release.apk
```

يفحص سلامة الحاوية، `AndroidManifest.xml`، `resources.arsc`، `classes.dex`، التوقيع بواسطة `apksigner`، المحاذاة بواسطة `zipalign`، ومعرّف الحزمة وSDK عبر `apkanalyzer` أو `aapt`. يبحث عن `apkanalyzer` أيضاً داخل `cmdline-tools`، ويرفض APK إذا كان Manifest يشير إلى split/feature APK أو `testOnly` أو `debuggable=true`، أو إذا لم يطابق معرّف الحزمة المتوقع.

## تشخيص التثبيت على Poco X7 Pro

فعّل USB debugging، صِل الهاتف، وافق على بصمة RSA لهذا الكمبيوتر، ثم:

```bash
adb devices -l
./tools/diagnose-install.sh app/build/outputs/apk/release/app-release.apk
```

يقرأ السكربت معلومات النسخة المثبتة، ويحاول سحب `base.apk` إلى مجلد مؤقت لمقارنة بصمة شهادة التوقيع مع APK الوارد إذا توفرت `apksigner`. بعدها يستخدم `adb install -r` فقط؛ لا يحذف النسخة ولا يطلب downgrade ولا يمسح بياناتها، ويلتقط أسطر Package Manager ذات الصلة. إذا كانت هناك أجهزة متعددة، حدّد الجهاز عبر `ANDROID_SERIAL`.

## تفسير الرموز الشائعة

| رمز Package Manager | الدلالة المحتملة | ما يجب التحقق منه |
|---|---|---|
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | شهادة APK الجديد لا تطابق شهادة النسخة المثبتة | قارن `apksigner --print-certs` بالنسخة القديمة؛ لا تحذف التطبيق ولا تغيّر المفتاح قبل نسخ البيانات وموافقة المالك |
| `INSTALL_FAILED_VERSION_DOWNGRADE` | `versionCode` الجديد أقل من المثبت | ارفع `versionCode` مع إبقاء `applicationId` ثابتاً |
| `INSTALL_FAILED_NO_MATCHING_ABIS` | لا توجد مكتبة native متوافقة مع ABI الجهاز | افحص `ro.product.cpu.abilist` ومجلد `lib/` داخل APK |
| `INSTALL_FAILED_OLDER_SDK` | `minSdk` أعلى من SDK الجهاز | لا تغيّر `minSdk` إلا بعد التحقق من توافق dependencies فعلياً |
| `INSTALL_FAILED_TEST_ONLY` | Manifest يضع `testOnly=true` | تأكد من بناء Release وليس APK اختبار |
| `INSTALL_PARSE_FAILED_*` أو `INVALID_APK` | APK تالف أو ناقص أو Manifest/Resources غير صالح | شغّل `validate-apk.sh` وافحص الملف الأصلي دون إعادة تسميته |
| `INSTALL_FAILED_INSUFFICIENT_STORAGE` | مساحة الجهاز غير كافية | افحص المساحة المتاحة دون حذف بيانات التطبيق |
| `INSTALL_FAILED_VERIFICATION_FAILURE` / `USER_RESTRICTED` | سياسة أمان أو مصدر تثبيت محظور | افحص سياسة الجهاز ومصدر التثبيت؛ لا تعطّل الحماية |

## لماذا لا يوجد تشخيص نهائي للخطأ السابق حتى الآن؟

لا تتضمن نسخة المصدر APK الذي فشل تثبيته، ولا نسخة مثبتة قابلة لسحب شهادة توقيعها، ولا keystore الخاص بالمالك، ولا سجل `adb install` من الهاتف. لذلك لا يمكن الجزم بأن سبب المحاولة السابقة كان التوقيع أو ABI أو إصدار النظام. يلزم حفظ رمز الخطأ الفعلي من السكربت أعلاه قبل تغيير أي شيء آخر.
