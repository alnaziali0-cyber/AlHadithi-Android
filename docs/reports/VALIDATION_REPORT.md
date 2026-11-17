> **تنبيه تاريخي:** هذا تقرير الجولة السابعة بتاريخ 14 نوفمبر 2026، ويعرض حالة المشروع في تلك الجولة فقط. تم تجاوزه بتقرير Round 20 الحالي `docs/reports/ROUND20_AUDIT_AND_REPAIRS_AR.md`؛ لا تستخدم أعداد الاختبارات أو وصف الأصول أو حدود المنصات أدناه بوصفها حالة التسليم الحالية.

# تقرير التحقق — الجولة السابعة — 2026-11-14

## نتائج تم تنفيذها فعلياً

- **اختبارات Backend:** 31/31 ناجحة باستخدام `PYTHONPATH=. python -m pytest -q` من `services/api`.
- **اختبار خصوصية GPS:** واجهة POST `/api/v1/public/prayer/times/coordinates` تقبل الإحداثيات في جسم الطلب، وتعيد `cache_status=not_cached_user_coordinates` ولا تُدرج في جدول `PrayerCache`؛ اختبار قاعدة البيانات لهذا السلوك ناجح. تطبيق الهاتف لا يضع الإحداثيات في URL أو SharedPreferences أو payload الكاش المحلي؛ يستخدم رمزاً عشوائياً في مفتاح الكاش ويحذف المدخلات السابقة عند تغيير الموقع.
- **Python compileall:** ناجح لخدمة API واختباراتها.
- **Pubspec YAML:** تحليل الملف ناجح، وتأكد وجود اعتماديات `geolocator`, `geocoding`, `flutter_secure_storage` وأصل JSON المضمّن.
- **Governorate catalogue JSON:** صالح ويحتوي 19 معرفاً ثابتاً فريداً، وأسماء عربية/إنجليزية، وأسماء بديلة للمطابقة، وإحداثيات ضمن الحدود العددية.
- **Dart structural scan:** فحص توازن الأقواس/المحددات نجح في 29 ملف Dart مصدر/اختبار. هذا فحص بنيوي فقط وليس بديلاً عن Dart analyzer أو compiler.
- **اختبارات Flutter المضافة:** 20 اختبار وحدة/واجهة موزعة على 6 ملفات اختبار، تشمل سياسة التوجيه، عدم وجود اختيار افتراضي، اختيار الجار، الدوران الدائري، التخزين الآمن عبر fake، rollback، قائمة المحافظات، fallback دون API، والتحقق من الدولة والمحافظة. لم تُشغّل بسبب غياب Flutter/Dart.
- **الشعار:** SHA-256 للشعار الأصلي مطابق تماماً للجولة السادسة: `3d9f1d8df96be3d04ca130afd54e384f51b39235a272e9b0a60f1591b0417820`.
- **المحافظة:** لم تُضف محافظة افتراضية؛ زر التأكيد غير فعال حتى اختيار المستخدم أو نجاح اقتراح GPS.

## الملفات الأساسية المضافة

- `apps/mobile/lib/features/governorate/governorate_selection_screen.dart`
- `apps/mobile/lib/features/governorate/governorate_wheel_picker.dart`
- `apps/mobile/lib/core/services/location_detection_service.dart`
- `apps/mobile/lib/core/governorate_repository.dart`
- `apps/mobile/lib/core/location_coordinates_store.dart`
- `apps/mobile/lib/core/initial_setup_route_policy.dart`
- `apps/mobile/assets/data/iraq_governorates.json`
- `docs/PLATFORM_LOCATION_SETUP.md`

## حدود لم تُختبر

- Flutter وDart غير مثبتين في بيئة التنفيذ؛ لم يمكن تشغيل `flutter pub get`, `dart format`, `flutter analyze`, `flutter test` أو بناء APK/AAB/IPA. لا يمكن اعتماد ملفات Dart على أنها compile-clean حتى تنفيذ هذه الأوامر.
- مجلدا `apps/mobile/android` و`apps/mobile/ios` غير موجودين في مدخل المشروع. لذلك أضيفت تعليمات AndroidManifest وInfo.plist، ولم تُضف صلاحيات Native إلى ملفات غير موجودة ولم يُختبر تسجيل plugins على جهاز.
- لم يُختبر GPS أو Reverse Geocoding على جهاز Android/iOS فعلي أو محاكي؛ سلوك المنصة يعتمد على خدمات النظام ومزود geocoder.
- لم تُختبر Keychain/Android Keystore فعلياً؛ الكود يستخدم `flutter_secure_storage`، لكن تسجيل الإضافة الأصلية يتطلب توليد المنصات وتشغيل `pub get`.
- لا يُعد مستطيل الإحداثيات اختبار حدود جغرافية دقيقاً؛ هو فحص معقولية فقط، ويظل التحقق من رمز/اسم الدولة عبر Reverse Geocoding إلزامياً.
- يجب على Reverse Proxy وAPM في الإنتاج إخفاء معاملات `latitude` و`longitude` من السجلات.

## قرار الإصدار

**الميزة مدمجة في مصدر Flutter وBackend، لكنها غير معتمدة للنشر بعد.** لا بد من إنشاء ملفات Android/iOS، تطبيق صلاحيات الاستخدام أثناء فتح التطبيق فقط، حل الاعتماديات، تشغيل اختبارات Flutter وتحليل Dart، ثم اختبار حالات GPS/رفض الإذن/GPS المغلق/خارج العراق/انقطاع الشبكة على أجهزة أو محاكيات حقيقية.
