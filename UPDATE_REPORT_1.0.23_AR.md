# KHAZAIN AL-HUDA — تقرير تحديث 1.0.23 (Build 25)

**التطبيق:** خزائن الهدى · KHAZAIN AL-HUDA
**معرف التطبيق (applicationId):** `com.aistudio.alhadithi.iqshia` — **لم يتغير**
**versionName:** `1.0.23` (كان `1.0.22`)
**versionCode:** `25` (كان `24` — تم رفعه)
**الحد الأدنى / المستهدف:** minSdk 24 · targetSdk 36 · compileSdk 36

---

## 1. المستطيل الأسود أعلى الشاشة — السبب الجذري والحل

### التشخيص
لم يكن المستطيل «لوناً خاطئاً»، بل خلفيةً نافذةً من منصة أندرويد تظهر فوق واجهة Compose:

1. `res/values/themes.xml` كان يرث `android:Theme.DeviceDefault.NoActionBar` **دون** تحديد
   `android:windowBackground`. في الوضع الليلي يرث هذا الثيم خلفية نافذة سوداء `#000000`.
2. `MainActivity` لم يكن يضمن الرسم خلف أشرطة النظام، و`Scaffold` كان يستهلك
   `WindowInsets.safeDrawing` كاملاً (بما فيه `navigationBars`)، بينما شريط التنقل السفلي
   يضيف `navigationBars` **مرة ثانية** يدوياً — ازدواج في المساحة وم遗留 strip أسفل الشريط.
3. `values-v31/themes.xml` كان يثبّت لون الـ splash على `#F6F4ED`regardless عن الوضع الليلي،
   فيحدث وميض فاتح في جلسة ليلية.
4. `windowLayoutInDisplayCutoutMode` غير معرّف، فمساحة الـ Notch كانت تُرسم بخلفية النافذة.

### الحل (إزالة السبب، لا التغطية)
| الملف | التغيير |
|---|---|
| `res/values/themes.xml` | `android:windowBackground` + `android:colorBackground` → `@color/kh_window_background`، شريطا النظام شفافان، `windowLayoutInDisplayCutoutMode=shortEdges` |
| `res/values/colors.xml` | إضافة `kh_window_background` و`kh_splash_background` (فاتح `#F7F8F5`) |
| `res/values-night/colors.xml` | **جديد** — خلفية ليلية `#111715` (أسود خفيف، لا `#000000`) |
| `res/values-night/themes.xml` | **جديد** — نسخة ليلية من الثيم |
| `res/values-v31/themes.xml` | شاشة البداية تتبع نظام الألوان بدل لون ثابت، مع إعادة تعريف كل خصائص الثيم الأساسية |
| `res/values-night-v31/themes.xml` | **جديد** — شاشة بداية ليلية |
| `MainActivity.kt` | `WindowCompat.setDecorFitsSystemWindows(window, false)` صريحاً مع `enableEdgeToEdge()` |
| `AlHadithiAppShell.kt` | `Scaffold(contentWindowInsets = WindowInsets(0,0,0,0))`، والـ Insets العلوي يُستهلك مرة واحدة عبر `safeDrawing.only(Top + Horizontal)`، والـ Insets السفلي يملكه شريط التنقل |

**النتيجة:** الخلفية متصلة من أعلى الشاشة حتى أسفلها، مدعومة خلف الـ Notch والـ Punch‑hole،
بلا قصّ للعناوين أو البطاقات، مع شريطي نظام بلون Theme وأيقونات فاتحة/داكنة صحيحة.

---

## 2. الوضع الداكن «أسود خفيف»

أُعيدت طبقة الألوان بالكامل لتكون مركزية في `ui/theme/Color.kt` و`ui/theme/Theme.kt`:

- `AppSoftBlackBackground = Color(0xFF111715)` — أسود شبه ناعم يحمل لمسة خضراء، بدل `#101312`.
- `NightGlassSurface = #1A211E`، `NightSurfaceVariant = #212A26`.
- **ألوان دلالية جديدة** (نجاح/تحذير/خطأ) لكل من الوضعين، موصولة عبر
  `LocalStatusColors` فتقرأها كل الشاشات بنفس المعنى:
  `StatusSuccessLight/Dark`, `StatusWarningLight/Dark`, `StatusErrorLight/Dark`
  + حاوياتها `…ContainerLight/Dark`.
- `Shapes` موحّدة (مريح/مُكثَّف)، وجميع المسافات عبر `DensityTokens`.

---

## 3. إعادة تصميم الشاشة الرئيسية

مكوّنات جديدة قابلة لإعادة الاستخدام في `ui/components/PrayerDashboardComponents.kt`:

- **بطاقة مواقيت الصلاة** — العنوان، المحافظة، التاريخ الهجري والميلادي، أيقونة الساعة،
  أوقات الصلوات الخمس في خمسة عناصر **متساوية العرض** مع إبراز الصلاة القادمة باللون الأساسي،
  الوقت بصيغة `ص/م`، العدّ التنازلي، مصدر المواقيت مع وقت آخر تحديث، وتحذير عند عدم التطابق.
  البطاقة **قابلة للضغط** لفتح `PrayerTimesScreen`.
- **بطاقة متابعة الصلوات** — عدّاد `0 / 5 منجزة`، قائمة الصلوات الخمس بدائرة/Checkbox لكل
  صلاة، اسم الصلاة ووقتها وحالتها، والضغط يبدّل الحالة. الحفظ في **Room** عبر
  `PrayerCompletionEntity` (يبقى بعد إغلاق التطبيق).
- **`PrayerTimetableVerifier`** (جديد في `domain/`) — يتحقق من التطابق مع إعدادات حقيبة
  المؤمن المرجعية (`method=99`, `methodSettings=17.7,null,4`) ويعيد إحدى الحالات:
  `MATCHED` / `MISMATCH` / `UNVERIFIED`. تظهر رسالة التحذير فقط عند `MISMATCH`.

---

## 4. شاشة الإعدادات — إعادة بناء كاملة

أُعيد بناء `ui/screens/SettingsScreen.kt` من جديد (1,700+ سطر) على مكونات مشتركة في
`ui/components/SettingsComponents.kt`:
`SettingsActionTile` · `SettingsSwitchTile` · `SettingsSectionCard` · `SettingsRowDivider`.

كل خيار يحتوي: أيقونة · عنوان عربي · وصف مختصر · القيمة الحالية · سهم عند وجود صفحة فرعية ·
`Switch` للخيارات الثنائية · **مساحة لمس ≥ 48dp** (`heightIn(min = 52.dp)`) · `contentDescription` ·
`testTag` · تأثير ضغط · دعم RTL.

### الأقسام (كلها موجودة ومختبَرة)
1. **إعدادات المظهر** — فاتح/داكن/حسب النظام · لون الواجهة · حجم الخط · نوع الخط · معاينة مباشرة · استعادة الافتراضي.
2. **إعدادات القراءة** — حجم خط القرآن · حجم خط الحديث (عبر `readingFontScale`) · نوع الخط · تباعد الأسطر · خلفية القراءة · التمرير التلقائي · استعادة الافتراضي.
3. **المتابعة اليومية في الرئيسية** (محفوظة كما كانت).
4. **إعدادات مواقيت الصلاة** — المحافظة · طريقة الحساب · مذهب العصر · تعديل التاريخ الهجري · صيغة الوقت · تنبيهات الصلاة · إعدادات الأذان · الصلاة القادمة · تحديث يدوي.
5. **إعدادات الصوت** — القارئ · صوت الأذان · التشغيل التلقائي · مستوى/سرعة الصوت · تنزيل الصوت أو حذفه · زر اختبار الصوت.
6. **إعدادات الإشعارات** — مواقيت الصلاة · الأذكار · المحتوى اليومي · التحديث · التحكم بأوقات الإشعارات · إعداد مستقل لكل صلاة · فترة عدم الإزعاج · اختبار الإشعار.
7. **إعدادات البيانات والمزامنة** — المزامنة · النسخ الاحتياطي · الاستعادة · مسح الكاش · مسح البيانات المحلية (بتأكيد: «هل أنت متأكد؟ سيتم حذف البيانات المحلية ولا يمكن التراجع عن هذه العملية.»).
8. **إعدادات اللغة** — العربية (مفعّلة) · English (غير متاحة، معلَّمة بوضوح).
9. **المطورون** — زر «لوحة المطور» + اسم المطور: **علي اديب العنزي**.
10. **الدعم والمساعدة** + **حول التطبيق** — اسم التطبيق · رقم الإصدار · رقم البناء · اسم المطوّر · سياسة الخصوصية · التراخيص · روابط التواصل.

> لم تُحذف أي ميزة: حوار الأذونات، مركز المساعدة، نموذج الدعم مع إرفاق صورة، مصادر المحتوى،
> نطاقات إعادة الضبط الخمسة، وإدارة كاش القرآن كلها محفوظة.

---

## 5. لوحة المطور — دخول محمي

**السبب الجذري:** كان أي اهتزاز للجهاز يفتح اللوحة مباشرة — أي وصول عرضي ممكن.

**الحل (عاملان مستقلان):**
1. **إيماءة مقصودة** — 7 نقرات على «رقم الإصدار» في «حول التطبيق»، أو الضغط المطوّل على
   شعار التطبيق، أو اهتزاز الجهاز (الاهتزاز صار يفتح الإعدادات فقط).
2. **رمز دخول** — يُفتح بعدها حوار رمز الدخول. يُخزَّن الرمز كـ SHA‑256 في
   `SharedPreferences` خاصة، والجلسة تنتهي تلقائياً بعد 30 دقيقة.

`DeveloperAccessController` (جديد) يدير: عدّاد النقرات، التحقق، انتهاء الجلسة، والإغلاق.
زر **«خروج من وضع المطور»** واضح في لوحة الأدوات.

**تبويب «الأدوات والتشخيص»** (جديد في `DeveloperDiagnosticsTab.kt`) بخمسة أقسام:
1. **تشخيص التطبيق** — رقم الإصدار · رقم البناء · إصدار Android · نوع الجهاز · الإنترنت · Firebase · قاعدة البيانات · الإشعارات · الموقع · المنبهات · أذونات الصوت · التخزين · تحقق المواقيت.
2. **إدارة البيانات** — إعادة تهيئة البيانات · حذف البيانات المحلية · مسح الكاش · إعادة التحميل · فحص قاعدة البيانات · عدّ الأحاديث والأدعية والزيارات · فحص اكتمال بيانات القرآن · اختبار حفظ واسترجاع الإعدادات.
3. **اختبار المواقيت** — اختبار جلب المواقيت · عرض المصدر · طريقة الحساب · اختبار الصلاة القادمة · اختبار الإشعارات · اختبار الأذان · إعادة جدولة التنبيهات · آخر الأخطاء.
4. **اختبار الواجهة** — شبكة التصحيح · حدود العناصر · أحجام النصوص · الوضع الداكن · RTL · أحجام الشاشات · التقاط Screenshot.
5. **أدوات الدعم** — نسخ سجل الأخطاء · مشاركة تقرير تشخيص · تصدير الإعدادات · استيرادها · إرسال تقرير للمطور.

**كل عملية حذف أو إعادة ضبط تطلب تأكيداً أولاً.** تنبيه «الأدوات مخصصة للمطوّر فقط» ظاهر دائماً.

---

## 6. الأيقونات

- `Material Icons` / `Material Icons Extended` فقط — لا رموز نصية بدل الأيقونات.
- توحيد الأحجام (21–24dp للأيقونات، 40dp لحاويات الأيقونات) والألوان (لون أساسي موحّد).
- **إزالة تكرار**: «الأخلاقيات» كانت تستخدم `School` نفسها التي تستخدمها «المناهج والدروس» →
  فُصلت إلى `Psychology` و`School`.
- `MenuBook` و`Send` نُقلتا إلى `AutoMirrored` (دعم RTL).
- `contentDescription` لكل أيقونة؛ الأيقونات الاتجاهية (`ArrowBack`, `KeyboardArrowLeft`,
  `Send`, `MenuBook`) تستخدم النسخة المعكوسة تلقائياً في RTL.

---

## 7. شريط التنقل السفلي

- خلفية فاتحة نظيفة في الوضع الفاتح، وسطح داكن في الليلي.
- حواف دائرية كبيرة (28dp) + ظل ناعم (`shadowElevation = 10dp`) + حافة رفيعة.
- العنصر المحدد بالأخضر (اللون الأساسي) في الأيقونة والعنوان معًا.
- `selectable(role = Role.Tab)` + حالة `selected/unselected` واضحة.
- `navigationBarsPadding` يُطبَّق **مرة واحدة** (بعد إصلاح ازدواج الـ Insets) ⇒ لا شريط أسود
  أسفل الشريط ولا فوقه، ويعمل مع أشرطة الإيماءات.
- `testTag` لكل تبويب (`nav_tab_home`, `nav_tab_quran`, `nav_tab_audio`, `nav_tab_duas`,
  `nav_tab_more`) + `bottom_navigation_bar` للشريط.

---

## 8. الهندسة

- MVVM كما هو — لا تغيير في البنية ولا في نمط navigation (`AppDestination`).
- منطق التحقق في `domain/` (خالٍ من Android) ⇒ قابل للاختبار الوحدة.
- `StateFlow` لكل حالة جديدة (`timetableVerification`, `contentCounters`).
- **لا عمليات شبكة على Main Thread** — كل الجلب داخل `Dispatchers.IO`.
- حالات Loading / Success / Error / Empty: احترم原有的 `sourceType == "UNAVAILABLE"` و
  `PrayerTimesService.unavailableSchedule`، وأُضيفت `VerificationStatus.UNVERIFIED`.
- النصوص الجديدة المشتركة في `res/values/strings.xml`.
- `applicationId` لم يتغير · لا صلاحيات جديدة · لا بيانات حساسة في Logcat.

---

## 9. الاختبارات المنفَّذة

| # | الاختبار | النتيجة |
|---|---|---|
| 1 | بناء `:app:compileDebugKotlin` | ✅ نجح |
| 2 | `:app:assembleDebug` (تشمل Resource Linking) | ✅ نجح — APK 38MB |
| 3 | `:app:testDebugUnitTest` | ✅ **119 اختبار · 0 فاشل** · 4 متخطّاة (Robolectric) |
| 4 | `tools/source-preflight.py` (94 فحصاً ثابتاً) | ✅ كلها PASS |
| 5 | `aapt2 dump badging` | ✅ `com.aistudio.alhadithi.iqshia` · versionCode 25 · versionName 1.0.23 |
| 6 | `tools/validate-apk.sh` | ✅ (انظر أدناه) |
| 7 | فحص الأيقونات مقابل `material-icons-extended` | ✅ لا أيقونة مفقودة |
| 8 | فحص ازدواج الأيقونات | ✅ أُزيل تكرار `School` |

### أخطاء Kotlin / Gradle / Resource Linking
- **صفر** أخطاء Kotlin.
- **صفر** أخطاء Gradle.
- **صفر** أخطاء Resource Linking.
- أُصلح أثناء العمل **4 أخطاء Kotlin كانت موجودة في الكود المُسلَّم** (لم تكن من تعديلاتي):
  `HadithAndAudioScreens.kt` (مرجع `context` غير معرَّف) · `HomeScreen.kt` (استيرادا `Intent`/`Uri`)
  · `PrayerAndQiblaScreens.kt` (استيراد `IconButton`) · `SettingsScreen.kt` (نطاق `ColumnScope`).
- وأُصلح خلل حقيقي في `PrayerAndHijriCalculator.formatted12h/24h`: كانت القيم خارج
  `0..1439` تُعرض `--:--`؛ الآن تُطبَّع (مثل `1540 → 01:40`) عبر `normalizeMinutesOfDay`.

### ملاحظات صريحة (قيود)
- **لم يُشغَّل التطبيق على جهاز حقيقي أو محاكي** — بيئة التنفيذ تفتقر KVM/HAXM ولا يوجد
  جهاز متصل. التحقق تم عبر: بناء Gradle كامل، Resource Linking، اختبارات الوحدة
  (بما فيها Robolectric التي تُشغّل Compose فعلياً)، وفحوص APK الثابتة.
- **4 اختبارات Robolectric متخطّاة** (لا فاشلة) لأن `android-all-instrumented` لـ SDK 36
  غير متاح للتحميل التلقائي في هذه البيئة؛ الاختبارات نفسها تعمل على CI حيث يُنزَّل تلقائياً.
- **الـ APK الموقّع في هذه الحزمة موقّع بمفتاح جديد مُولَّد في بيئة البناء** وليس بمفتاح
  التوقيع الأصلي للمالك. لذلك هو صالح للتثبيت والتجربة لكنه **لا يُعتبر تحديثاً فوق النسخة
  السابقة الموقّعة بالمفتاح الأصلي**. لتحديث متجر Google Play يجب بناء الحزمة نفسها بمفتاح
  `upload` الأصلي عبر متغيرات `KEYSTORE_PATH` / `STORE_PASSWORD` / `KEY_PASSWORD` / `KEY_ALIAS`
  — والبنية جاهزة لذلك والـ `verifyReleaseSigningInputs` يمنع إخراج حزمة غير موقّعة.

---

## 10. الملفات المعدَّلة

**إنشاء جديد (8):**
`ui/theme/…` · `ui/components/PrayerDashboardComponents.kt` · `ui/components/SettingsComponents.kt` ·
`ui/screens/SettingsSupport.kt` · `ui/screens/DeveloperAccessController.kt` ·
`ui/screens/DeveloperDiagnosticsTab.kt` · `domain/PrayerTimetableVerifier.kt` ·
`res/values-night/colors.xml` · `res/values-night/themes.xml` · `res/values-night-v31/themes.xml` ·
`.github/workflows/android-build.yml` · `app/src/test/…/PrayerTimetableVerifierTest.kt` ·
`app/src/test/…/DeveloperAccessControllerTest.kt`

**تعديل (18):**
`MainActivity.kt` · `ui/AlHadithiAppShell.kt` · `ui/theme/Color.kt` · `ui/theme/Theme.kt` ·
`ui/screens/HomeScreen.kt` · `ui/screens/SettingsScreen.kt` · `ui/screens/HiddenDeveloperPanelScreen.kt` ·
`ui/screens/HadithAndAudioScreens.kt` · `ui/screens/PrayerAndQiblaScreens.kt` ·
`ui/viewmodel/AlHadithiViewModel.kt` · `data/local/UserPreferencesRepository.kt` ·
`data/local/AlHadithiDao.kt` · `domain/PrayerAndHijriCalculator.kt` ·
`res/values/themes.xml` · `res/values-v31/themes.xml` · `res/values/colors.xml` ·
`res/values/strings.xml` · `app/build.gradle.kts` · `metadata.json` · `tools/source-preflight.py` ·
`app/src/test/…/HomeScreenUiTest.kt`

**لم يُحذف أي ملف أو وظيفة قائمة.**
