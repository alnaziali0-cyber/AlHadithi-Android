# تقرير الجودة — AL-HADITHI 1.0.16

## فحوصات المصدر والبيانات المنفذة

- تحليل 13 ملف XML و12 ملف JSON: جميعها صالحة تركيبياً.
- مقارنة معرفات المحافظات في Kotlin وقواعد Firestore: 18 معرفاً فريداً ومطابقة 18/18.
- التحقق من ملفات الأصول: `adhan_default.mp3` وملف شعار المستخدم و`quran_simple_full.json` موجودة وغير فارغة.
- فحص مسار المواقيت: لا يوجد `AlAdhanApiService.kt` ولا مراجع `PrayerNetworkClient`؛ جميع المسارات الحية للجدول والتنبيهات تستدعي `PrayerTimesService`.
- فحص نصوص التهيئة الإنتاجية: لم يُعثر على كلمة مرور ثابتة ضمن مصدر الإنتاج؛ توجد بيانات اختبار emulator منفصلة في `src/test` لا تستخدم بيانات المطور.
- فحص Tanzil Simple: 114 سورة و6,236 آية؛ تطابق كل نص آية مع الملف النصي المرفوع حرفياً.
- اختبارات أداة القرآن: `python -m unittest discover -s scripts/tests -v` — 15 اختباراً ناجحاً.
- ملف الأذان: MPEG Layer III، 44.1kHz، Stereo، حجم 4,746,032 بايت.

## الفحوصات غير المنفذة

- `./gradlew :app:testDebugUnitTest` و`:app:assembleDebug`: لم يكتمل تنزيل Gradle 9.3.1 بسبب `UnknownHostException: services.gradle.org`؛ Android SDK غير متوفر في بيئة التحرير.
- Firebase Rules Emulator: لم يُشغّل لأن `@firebase/rules-unit-testing` غير مثبت ولم يتوفر محاكي Firebase.
- لم يتم نشر `firestore.rules` أو `firestore.indexes.json` ولم يتم إنشاء سجلات إنتاجية في `prayer_times_daily`.
- لم يتم تشغيل تطبيق Android على جهاز فعلي، لذا لا يوجد تحقق فعلي من بث WebView أو foreground adhan service أو AlarmManager.

## أمر التحقق المحلي

```bash
python -m unittest discover -s scripts/tests -v
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

لا تعتبر نجاح فحوصات البيانات والموارد بديلاً عن بناء APK واختبار الجهاز.

## 1.0.16 — Location permission and precise GPS
- [x] One centralized Android runtime permission launcher.
- [x] Fine and coarse location requested together.
- [x] One-shot prompt marker persists locally and is excluded from backup/device transfer.
- [x] No background-location permission declared.
- [x] No direct `lastLocation` use remains in app source.
- [x] Current high-accuracy location path validates timestamp, coordinates and reported accuracy.
- [x] Pure Kotlin policy smoke test: 8/8 assertions passed.
- [x] Manifest/source static guardrails: 12/12 checks passed.
- [ ] Android Gradle compilation/device permission UI tests — blocked by Gradle distribution DNS failure and missing Android SDK.
