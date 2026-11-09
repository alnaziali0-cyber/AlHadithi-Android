# تقرير اختبار الجودة — الحديثي 1.0.8

## تغيير قارئ القرآن
- تم تحويل نص السورة من بطاقات منفصلة إلى `ClickableText` واحد داخل مساحة تمرير واحدة.
- لا تُستخدم `GlassCard` أو `Surface` لكل آية؛ تظهر الآيات متتابعة في فقرة قرآنية واحدة مع فواصل مسافة وعلامات أرقام داخلية.
- يحافظ الضغط على الآية على سلوك فتح التفاصيل، مع نقل الحفظ والنسخ والمشاركة إلى لوحة التفاصيل بدلاً من تكرار الأزرار أسفل كل آية.
- تمت إضافة اختبار وحدات للتحقق من نص السورة المتصل، وتعليقات الآيات، وبدايات الآيات، والسورة الفارغة.
- تم تحسين حساب موضع الانتقال للآية بحيث يأخذ مساحة العنوان والهوامش في صفحة القراءة بالحسبان.

## حدود التحقق
- لم تتوفر بيئة Android Gradle مكتملة في هذه الجلسة: لا يوجد Gradle executable أو `gradle-wrapper.jar` أو Android SDK (`ANDROID_HOME`). لذلك لم يمكن تشغيل `:app:testDebugUnitTest` أو `:app:assembleDebug`.
- لا يمكن تأكيد خلو التطبيق من جميع أخطاء وقت التشغيل أو اعتماد APK دون بناء Android فعلي وتجربة على جهاز.
- شُغّلت فحوصات المصدر والأرشيف وملفات الموارد. اجتاز تحليل 9 ملفات JSON و12 ملف XML، وفحص توازن الأقواس، والفحوصات الساكنة لوجود تدفق نص متصل واحد واستمرار الوظائف. أضيفت اختبارات JUnit للمسار الجديد لتشغيلها في بيئة Android Gradle.

## أوامر التحقق في بيئة البناء
```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

# اختبار جودة الحديثي 1.0.7

إضافات الإصدار: فحص ملفات Noto Naskh العربية الفعلية، وتصحيح نسخ القرآن والعمل دون اتصال. لم يتم تنفيذ Android Gradle build أو اختبارات Robolectric في بيئة التحرير بسبب غياب Gradle Wrapper/Android SDK. خط الرقعة ونص KFGQPC/QPC Hafs الكامل لم يدمجا بعد.

# تقرير فحص الجودة — AL-HADITHI v1.0.3

## التعديلات
- أذونات وقت التشغيل: `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION`, `POST_NOTIFICATIONS`.
- وصول التنبيهات الدقيقة: `SCHEDULE_EXACT_ALARM` مع طلب إذن خاص من إعدادات النظام.
- مستقبل إشعار فعلي ينشئ Notification عند دخول وقت الصلاة.
- جدولة 35 يوماً للصلوات الخمس، مع استخدام أوقات API للشهر الحالي وحساب محلي لبقية الأيام.
- إعادة جدولة بعد `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED` وتغيّر وصول التنبيهات الدقيقة.
- تخزين إحداثيات المستخدم بدلاً من الاعتماد الدائم على مركز المحافظة.
- إضافة معايرة يدوية بالدقائق واختبارات لها.

## الفحوصات المنفذة
- AndroidManifest.xml: تم تحليله بنجاح بواسطة XML parser.
- التصريحات المطلوبة للأذونات والمستقبلات: موجودة.
- ملفات Kotlin المستقلة لحساب المواقيت: تم تجميعها بنجاح باستخدام `kotlinc`.
- تمت تجربة حاسبة المواقيت برمجياً على إحداثيات اختبارية، وتحقق اختلاف الحساب مع اختلاف الإحداثيات وتطبيق offset بالدقيقة.
- ملفات الصوت المحلية الفارغة: لا توجد ملفات صفرية البايت في `res/raw`.

## حدود التحقق
- لم يكن Gradle أو Android SDK أو `gradle-wrapper.jar` متوفراً في بيئة التحرير؛ لذلك لم يمكن تشغيل Android Gradle Plugin أو الاختبارات الكاملة أو إنتاج APK.
- لم يمكن اختبار منح الأذونات أو إشعارات AlarmManager على جهاز مادي في هذه البيئة. يلزم اختبار APK على Android 12 و13 وما بعدهما، مع إعادة تشغيل الهاتف، وتعطيل/منح إذن التنبيهات الدقيقة، وتغيير الموقع والوقت.
- قد تتأخر الإشعارات إذا لم يمنح المستخدم إذن التنبيهات الدقيقة أو إذا قيّد النظام نشاط التطبيق. وقد يمنع Android التنبيه بعد فرض إيقاف التطبيق حتى فتحه مجدداً.
- الصوت في الإشعار هو صوت إشعار النظام، وليس تلاوة أذان كاملة.

## ملاحظات دقة المواقيت
- الطريقة الافتراضية هي الجعفرية (AlAdhan method 0).
- لا يمكن الجزم بأن الفجر يجب أن يكون 04:59 لكل جهاز ومكان؛ تعتمد النتيجة على الموقع الفعلي والتاريخ والطريقة والتقويم المحلي.
- شاشة الضبط اليدوي تسمح بتطبيق فرق محلي. مثال حسابي فقط: من 05:08 إلى 04:59 = −9 دقائق. يجب اعتماد تقويم مسجد أو جهة محلية موثوقة قبل تثبيت هذا الفرق.

## حالة الإصدار
نسخة مصدر مطوّرة. لم يُعتمد APK للنشر بعد نجاح بناء Android واختبارات الجهاز الفعلية.


## تحديث 1.0.4 — مكتبة القرّاء

- أضيفت ملفات فهرس القرّاء والسور ومصادر التنزيل وبيان الحقوق إلى أصول التطبيق.
- أضيفت ثلاثة سجلات قارئ من الحزمة: شهريار پرهيزگار، حامد شاكرنژاد، وكريم منصوري.
- حامد: ربط 30 رابط جزء مباشر بالمشغل Media3، وربط التنزيل دون اتصال بمفتاح الجزء الصحيح. اختيار سورة يشغّل الجزء الذي تبدأ فيه السورة (وليس ملف سورة منفرداً).
- شهريار وكريم: يفتح زر التشغيل صفحة المصدر، لعدم وجود روابط MP3 مباشرة موثقة في الفهرس.
- لم يتم تضمين أي تسجيل صوتي؛ حقوق إعادة التوزيع لم تثبت.
- تعذر تنفيذ Gradle build واختبارات Android في هذه البيئة لغياب Gradle وملف gradle-wrapper.jar وAndroid SDK. يلزم بناء المشروع واختباره على Android قبل اعتماد APK.


## QA Addendum — 1.0.12

- Python importer unit tests: 4 passed (`python3 -m unittest discover -s scripts/tests -v`). Test XML fixtures are synthetic structure fixtures only and are not Quran content.
- Radio frequency catalog has 3 JUnit test cases added; not executed because the Android Gradle test environment could not be initialized.
- Static review confirms `quran_uthmani_full.json` remains absent, so the app must not be described as having a full first-install offline Quran in this release.
- Android Gradle build / APK / device smoke test: not run; Gradle distribution could not be downloaded in this environment and Android SDK is unavailable.


## QA Addendum — 1.0.13

- Python importer: 7 tests passed, including both Tanzil quarter metadata layouts, boundary range validation, verbatim ayah preservation, and sajda-marker/metadata agreement. Fixtures are synthetic and contain no real Quran corpus.
- Kotlin cache integrity: new 7-test JUnit suite added. The pure validator was also compiled with `kotlinc` in an isolated harness; 3 assertions passed. Android JUnit was not run because the Gradle distribution and Android SDK are unavailable.
- XML/JSON resource syntax: 12 XML and 9 JSON files parse successfully.
- Quran cache precedence changed: a validated complete bundled corpus is checked before old Room cache rows; incomplete/invalid cached rows are not returned as successful full-sura results; failed asset probing is memoized, and coroutine cancellation is rethrown instead of being swallowed as a normal failure.
- No full Quran asset is included; no APK or physical-device verification was performed.


## QA Addendum — 1.0.14

- Uploaded metadata XML parsed: 114 surahs, 30 juz, 240 hizb quarters, 7 manzils, 556 rukus, 604 pages, 15 sajdah positions.
- Uploaded Tanzil Simple v1.1 text parsed: exactly 6,236 ayah records; 114 surahs; no duplicates, gaps, empty text, or malformed records. 30 blank/comment/footer lines are not ayah records.
- Per-surah ayah counts and zero-based `start` offsets match metadata for all 114 surahs.
- Sajdah marks in the text match all 15 metadata positions exactly.
- Generated Android asset `quran_simple_full.json`: 1,444,945 bytes; full 114/6,236; source text comparison 6,236/6,236 exact. Quarter and sajda structural values sourced from metadata.
- Original Simple copyright block preserved verbatim in `licenses/TANZIL_QURAN_SIMPLE_NOTICE.txt`; metadata attribution notice and SHA-256 manifest added.
- Python unit tests: 15 passed, including 5 Simple parser tests, 7 XML-importer tests, and 3 integration tests that rebuild the actual uploaded corpus, compare the full JSON and verify the verbatim license block/checksums. Android JUnit/Robolectric/Gradle build not run because the environment has no working Gradle distribution/Android SDK.
- Reader avoids a duplicate separate basmala when the first source ayah already starts with basmala. Offline search prioritizes bundled Simple data over previous cached network data.

- Kotlin `shouldDisplaySeparateBasmala` helper compiled with `kotlinc` in an isolated harness and passed 5 assertions, including the distinct tashkeel used by Tanzil Simple in surahs 95 and 97.
- A Gradle attempt `./gradlew :app:testDebugUnitTest --offline` was made and failed with `UnknownHostException: services.gradle.org` because Gradle 9.3.1 is not cached; Android SDK environment variables are also unset.
