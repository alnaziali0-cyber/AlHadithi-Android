# إعداد التنبيهات المحلية — Android وiOS

تمت إضافة منطق جدولة تنبيهات محلي في Dart باستخدام `flutter_local_notifications: ^19.5.0` و`timezone: ^0.10.1`. لأن أرشيف المشروع الحالي لا يحتوي على مجلدي `apps/mobile/android` و`apps/mobile/ios`، فإن هذا الملف يحدد تكامل المنصة المطلوب بعد توليد ملفاتها؛ لا يُعد بديلاً عن البناء والاختبار على الأجهزة.

## ما يفعله كود التطبيق حالياً

- يطلب إذن التنبيهات بعد تفعيل المستخدم للخيار صراحةً.
- ينشئ نافذة متجددة لمدة 12 يوماً، بحد أقصى 60 إشعاراً (الفجر والظهر والعصر والمغرب والعشاء). تم اختيار 12 يوماً لأن iOS يحد الإشعارات المعلقة إلى 64؛ خمس صلوات × 14 يوماً تتجاوز هذا السقف.
- يطبّق فروقات الدقائق المحلية التي اختارها المستخدم، ويستخدم المنطقة الزمنية للمدينة/المحافظة مع `Asia/Baghdad` كقيمة احتياطية.
- يعيد بناء الجدول عند فتح التطبيق/عودته، وعند تغير مواقيت اليوم أو فروقات الدقائق أو تفعيل/تعطيل الخيار.
- إذا لم يُمنح إذن exact alarm، يحاول جدولة تنبيهات غير دقيقة بدلاً من تعطيل التنبيه كله.

## Android — مطلوب بعد توليد المنصة

1. شغّل `flutter create --platforms=android,ios .` من مجلد `apps/mobile` بعد مراجعة تعارضات الملفات، أو ولّد المنصات في نسخة عمل منفصلة ثم ادمجها.
2. أضف إلى `android/app/src/main/AndroidManifest.xml` داخل `<manifest>` الأذونات التالية. قد يضيف مكوّن الإشعارات بعض أذونات الإشعارات تلقائياً أثناء الدمج، لكن راجع الناتج النهائي ولا تعتمد على ذلك دون فحص الـ Manifest المدمج:

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
```

يطلب التطبيق إذن الموقع أثناء الاستخدام فقط عند ضغط المستخدم على «حدد الموقع تلقائياً». لا تضف `ACCESS_BACKGROUND_LOCATION`؛ لا توجد حاجة لتتبع الموقع في الخلفية.

3. أضف داخل `<application>`:

```xml
<receiver
    android:exported="false"
    android:name="com.dexterous.flutterlocalnotifications.ScheduledNotificationReceiver" />
<receiver
    android:exported="false"
    android:name="com.dexterous.flutterlocalnotifications.ScheduledNotificationBootReceiver">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
        <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
        <action android:name="android.intent.action.QUICKBOOT_POWERON" />
        <action android:name="com.htc.intent.action.QUICKBOOT_POWERON" />
    </intent-filter>
</receiver>
```

4. احتفظ بإذن `POST_NOTIFICATIONS` الذي يدمجه المكوّن الإضافي. لا تضف `USE_EXACT_ALARM` إلا بعد مراجعة سياسة متجر النشر؛ الإعداد هنا يستخدم `SCHEDULE_EXACT_ALARM` وطلب الإذن عند الحاجة.
5. وفق متطلبات إصدار 19.5.0، راجع `compileSdk` ليكون 35 على الأقل، وفَعّل Java API desugaring و`coreLibraryDesugaring` بالمكتبة التي تحددها وثائق النسخة. لا تفترض نجاح Gradle قبل مراجعة إصدار Android Gradle Plugin وملفات Kotlin/Gradle المولدة.
6. اختبر: تثبيت جديد، رفض إذن الإشعار، رفض exact alarm، قبول الإذن، إيقاف التطبيق، إعادة تشغيل الجهاز، تعديل المنطقة الزمنية، تغيير المدينة، انقطاع الإنترنت، وتحديث التطبيق.

## iOS — مطلوب بعد توليد المنصة

في `ios/Runner/Info.plist` أضف وصفاً واضحاً للموقع أثناء الاستخدام فقط:

```xml
<key>NSLocationWhenInUseUsageDescription</key>
<string>يُستخدم موقعك عند اختيار تحديد الموقع تلقائياً لعرض مواقيت الصلاة المناسبة لمدينتك.</string>
```

لا تضف `NSLocationAlwaysAndWhenInUseUsageDescription` ولا Background Modes للموقع؛ التطبيق لا يحتاج تتبع الموقع بالخلفية.

بعد توليد `ios/Runner/AppDelegate.swift`، راجع تكامل مركز الإشعارات في AppDelegate وفق مثال الإصدار 19.5.0، وأضف في `application(_:didFinishLaunchingWithOptions:)` بعد تسجيل Plugins:

```swift
if #available(iOS 10.0, *) {
  UNUserNotificationCenter.current().delegate = self as? UNUserNotificationCenterDelegate
}
```

وتأكد من استيراد `UserNotifications` عند الحاجة. لا تضف أذونات تشغيل موقع بالخلفية لهذا الغرض.

- أذونات التنبيه والصوت تُطلب من المستخدم عند تفعيل المفتاح فقط، ولا يطلبها التطبيق في بداية التشغيل. إذن GPS منفصل، ويُطلب فقط عند اختيار الموقع التلقائي؛ يتم فحص تفعيل خدمة الموقع وحالة الإذن الفعلية، ويستخدم التطبيق `LocationAccuracy.bestForNavigation` ويعتبر دقة أسوأ من 500 متر غير كافية للاختيار التلقائي. لا يمكن أن يضمن التطبيق دقة GPS إذا كانت خدمة الموقع أو أجهزة الاستشعار أو صلاحيات النظام معطلة.
- يحتفظ iOS بحد أقصى 64 إشعاراً مجدولاً؛ لذلك لا تتجاوز النافذة الحالية 60 إشعاراً. تُعاد تعبئة النافذة عند فتح التطبيق/عودته؛ لا توجد خدمة تحديث خلفي يومي مضمونة حتى الآن، لذلك قد تنفد المواعيد بعد مرور 12 يوماً دون فتح التطبيق. راجع السلوك عند تغيّر التوقيت الصيفي والمنطقة الزمنية.
- اختبر على جهاز حقيقي: منح/رفض الإذن، Focus/Do Not Disturb، إغلاق التطبيق، تغيير المنطقة الزمنية، وتحديث الجهاز.

## قيود مهمة

- هذا النظام يرسل **إشعاراً محلياً بصوت الإشعار الافتراضي للنظام**. لا يشغّل ملف MP3 لأذان كامل لمدة 4:55 عند إغلاق التطبيق. ذلك يتطلب مكوّناً أصلياً وخدمة خلفية ملائمة وسياسات النظام/المتجر واختبارات مستقلة، ولا يمكن ضمانه عبر مجرد جدولة Notification.
- بعض إصدارات Android المعدلة لدى الشركات قد توقف مهام الخلفية أو تؤخرها، لذا قد يلزم استثناء التطبيق من تحسين البطارية يدوياً. وحتى في الأنظمة المدعومة لا تُعد هذه الجدولة بديلاً عن اختبار الجهاز الفعلي.
- أُضيف وقت العصر إلى نتيجة الحساب بمعامل الظل القياسي 1 (Standard shadow factor)، وهو المعامل الذي يصفه توثيق PrayTimes لطريقة الجعفري. يجب مقارنته مع التوقيت المحلي للمرجع/الجهة الدينية قبل الاعتماد عليه؛ يظل الوقت حساباً فلكياً لا اعتماداً شرعياً نهائياً.
- أُزيل مشغّل الأذان اليدوي وملف MP3 من هذا الإصدار؛ لا يوجد تشغيل صوتي يدوي للأذان داخل التطبيق.

## مراجع التنفيذ

- توثيق النسخة 19.5.0: https://pub.dev/packages/flutter_local_notifications/versions/19.5.0
- توثيق المنطقة الزمنية: https://pub.dev/packages/timezone
