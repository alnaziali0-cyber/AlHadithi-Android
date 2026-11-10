# خدمة إشعارات الحديثي

خلفية مستقلة ترسل إشعارات FCM الحقيقية (إعلانات التطبيق) إلى الأجهزة المسجّلة.

## المتطلبات
1. حساب خدمة Firebase بصلاحية:
   - Firebase Cloud Messaging API (إرسال)
   - Cloud Datastore / Firestore (قراءة وكتابة)
2. تفعيل **Cloud Messaging API** في مشروع Firebase.
3. تفعيل **Firebase Storage** لحفظ صور الإعلانات.

## التشغيل
```bash
npm install
cp .env.example .env      # املأ GOOGLE_APPLICATION_CREDENTIALS
npm start                  # تشغيل مستمر
npm run once               # تنفيذ دورة واحدة فقط
npm run dry-run            # دورة تجريبية بلا إرسال
```

## كيف يعمل
- يقرأ `ads` حيث `sendPush == true` و `pushedAt == null`.
- يقرأ الأجهزة من `notification_devices` (يسجّلها التطبيق عبر FCM).
- يرسل الإشعار بـ `channelId: alhadithi_ads` وأيقونة `ic_notification_alhadithi` (الشعار الرسمي أحادي اللون).
- يسجّل كل محاولة في `notification_log` مع حالة الإرسال الفعلية من FCM.
- يحدّث `pushedAt` بعد الإرسال لمنع التكرار، ويحذف الأجهزة ذات الرموز غير الصالحة.
