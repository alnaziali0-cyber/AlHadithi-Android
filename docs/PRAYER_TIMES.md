# Prayer-time engine

## Default calculation preset

The API's default is `jafari_qom`, exposed in every response with its parameter values and source link:

- Fajr: solar depression 16°.
- Maghrib: solar depression 4° below the horizon after apparent sunset.
- Isha: solar depression 14°.
- Sunrise/sunset: conventional apparent solar-center altitude −0.833°.
- Dhuhr: local solar noon from longitude and equation of time.
- Asr: standard shadow-factor 1; the Asr altitude is computed from `arctan(1 / (1 + tan(|latitude − solar declination|)))`. The PrayTimes method documentation identifies the Standard shadow-factor-1 method with Jafari. This is a published calculation convention, not a binding Ja'fari ruling; compare the result with an approved local timetable.
- Islamic midnight: midpoint from apparent sunset to the following Fajr.
- A second `islamic_midnight_latest_by` value is also returned (midpoint from calculated Maghrib to the following Fajr) so the distinction is explicit rather than silently conflated.

A separately exposed `tehran` preset uses Fajr 17.7°, Maghrib 4.5° and Isha 14°. The angle sets are documented by TheShia calculation-method reference. The code computes the solar events locally with a NOAA fractional-year approximation; it does not depend on that site's API uptime.

## Meaning and limits

These are configurable calculation conventions, not a universal ruling for all Ja'fari maraji'. The user must be able to see the selected method and understand how Maghrib and the two midnight markers were calculated. A local authority's published timetable may include a different precaution, elevation, horizon, twilight definition or manual adjustment.

Governorate centers are approximate coordinates, not a substitute for a precise user-selected location. The Round 7 mobile implementation adds opt-in foreground GPS and reverse geocoding; Android/iOS permission entries still require the native platform folders to be generated and configured. The engine fails closed when a required solar event is undefined; high-latitude fallback rules are not implemented because the current target list is Iraq.

Before release, compare at least 30 days across all 19 governorates with a locally approved Ja'fari timetable and have a qualified reviewer sign off the chosen convention and adjustments. Differences must be documented and tested; do not add hard-coded offsets to make a single day's data match.


## Round 6 city-level source integration

The requirements pack supplied twenty approximate city-center rows covering nineteen governorates. The list includes Tikrit and Samarra under Salah al-Din. The records are imported with `verified=false` and `coordinate_accuracy=approximate_city_center`; they are not GPS positions for a mosque and must be license-reviewed before public redistribution.

The API serves this catalogue at `/api/v1/public/prayer/cities` and calculates a city-based day at `/api/v1/public/prayer/times?city_id=...`. The cache namespace includes the city identity so two nearby locations cannot return each other's localized metadata.

Both built-in angle profiles are explicitly marked `officially_verified=false`. The source pack mentions the Nojumi calculator as a comparison/contact reference and explicitly warns that the observed values are not a universal or officially approved profile. No reference profile has been approved in this release.

## اختيار المحافظة والموقع في تطبيق الهاتف

- عند أول تشغيل، يمنع موجّه التطبيق الوصول إلى الواجهة الرئيسية قبل حفظ معرّف محافظة ثابت.
- كتالوج المحافظات التسع عشرة موجود في `apps/mobile/assets/data/iraq_governorates.json` ويُحدّث من واجهة `/api/v1/public/governorates` عند توفر الشبكة؛ تُستخدم النسخة المضمّنة أو آخر نسخة مخزنة عند تعذر الاتصال.
- تحديد GPS يبدأ فقط بعد ضغط المستخدم على زر التحديد. يطلب إذن الاستخدام أثناء فتح التطبيق فقط، ثم ينفذ Reverse Geocoding.
- لا يكفي وقوع الإحداثيات داخل مستطيل تقريبي للعراق؛ يجب أن تؤكد نتيجة Reverse Geocoding رمز الدولة `IQ` أو اسم العراق، ثم تطابق المحافظة مع معرف ثابت.
- إذا تعذر تحديد الدولة أو المحافظة، أو رُفض الإذن، أو أُغلقت خدمات الموقع، يبقى الاختيار اليدوي متاحاً ولا تُختار بغداد أو أي محافظة تلقائياً.
- عند اختيار GPS، تحفظ الإحداثيات الدقيقة في التخزين الآمن على الجهاز. حساب الأيام المقبلة عبر API التطبيق الحالي يرسلها في POST body ولا يحفظها في SharedPreferences أو جدول `PrayerCache`؛ يستخدم الكاش المحلي بصمة عشوائية محفوظة في التخزين الآمن، ويُحذف كاش GPS السابق عند تغيير الموقع. عند الاختيار اليدوي تُستخدم إحداثيات مركز المحافظة أو المدينة التقريبية، واختيار مدينة يمسح إحداثيات GPS الدقيقة من التخزين الآمن.
- **Round 21 — مواقيت اليوم من MisrQuran:** عند اختيار GPS، يرسل التطبيق `lat` و`lng` إلى `https://api.misrquran.gov.eg/api/PrayerTimes/GetTodaysTimes` ضمن استعلام HTTPS، بعد موافقة المستخدم الصريحة على هذه الوجهة. العميل المخصص لا يضيف `X-Device-Key` ولا يسجل الإحداثيات؛ ولا تتضمنها مفاتيح الكاش المحلي أو حمولة النتيجة المحفوظة. مع ذلك، قد تراها سجلات مزود الخدمة أو بنيته التحتية وفق سياساته. عند الاختيار اليدوي تُرسل إحداثيات مركز المحافظة/المدينة المختارة. يعرض التطبيق مواقيت اليوم من هذه الخدمة، بينما تُحسب الأيام الـ11 التالية محلياً. يُحوّل `currentServerUTCTime` وحقول الساعة UTC إلى `Asia/Baghdad` مرة واحدة؛ ويُرفض الرد غير الصالح أو ذو التاريخ المختلف، ثم يُستخدم كاش API للموقع/اليوم نفسه بوصفه قديماً، وبعده الحساب المحلي. الخدمة غير موثقة كمرجع فقهي؛ لا يُنشأ منتصف الليل الإسلامي إذا غاب عن ردها، ولا يُفترض أن `sunset` و`maghrib` مختلفان.
- ملفات الحساب الفلكي الحالية غير معتمدة مرجعياً؛ تحديد الموقع لا يجعل طريقة الحساب معتمدة.
- إعداد أذونات Android وiOS موثق في `docs/PLATFORM_LOCATION_SETUP.md`. ملفات المنصات الأصلية غير موجودة في الأرشيف الحالي، لذا لا يمكن اعتبار أذونات Native مفعلة حتى إنشاء المنصات وتطبيقها واختبارها.

Reference for the Asr shadow-factor convention: https://praytimes.org/docs/manual
