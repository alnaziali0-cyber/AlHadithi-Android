# ربط Quran Foundation API داخل خزائن الهدى

المراجع الرسمية:
- https://api-docs.quran.com/
- https://api-docs.quran.com/docs/quickstart/
- https://api-docs.quran.com/docs/tutorials/content-sync/getting-started/
- https://api-docs.quran.com/legal/developer-terms/

## ضوابط التنفيذ

1. أنشئ تطبيقاً في Developer Console واحصل على بيانات اعتماد الخادم.
2. خزّن `client_secret` في الخادم فقط، ولا تضعه داخل Android/iOS أو Git.
3. اجعل التطبيق يتصل بخادمك الوسيط، والخادم هو من يتصل بـ Content APIs.
4. استخدم Content Sync حصراً للموارد المتاحة للمزامنة دون اتصال؛ لا تنشئ نسخة Offline من استجابات API الاعتيادية إذا كان المورد متاحاً عبر Content Sync.
5. نفّذ مزامنة جديدة خلال 7 أيام عندما يتوفر الاتصال، وطبّق تغييرات المصدر.
6. لا تبيع أو تعِد توزيع المحتوى الخام كحزمة مستقلة أو API خاص بك دون ترخيص تجاري مكتوب.
7. أبقِ النص القرآني كما هو ولا تعدّله.

مجموعات الموارد التي توثقها Content Sync تشمل: quran_core، mushafs، translations، word_by_word_translations، word_by_word_transliterations، tafsirs، recitations، chapter_recitations، articles.
