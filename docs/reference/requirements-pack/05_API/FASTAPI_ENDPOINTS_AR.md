# مواصفات واجهات FastAPI المقترحة

Base path: `/api/v1`

## المحتوى
- `GET /content` — قائمة المحتوى المنشور مع فلاتر `type`, `creator_id`, `q`, `page`, `limit`.
- `GET /content/{id}` — تفاصيل مادة منشورة.
- `POST /admin/content` — إضافة metadata ورفع/ربط ملف (مسؤول فقط).
- `PATCH /admin/content/{id}` — تعديل metadata.
- `POST /admin/content/{id}/submit-review` — إرسال للمراجعة.
- `POST /admin/content/{id}/approve` — موافقة مسؤول مخول بعد التحقق من الحقوق.
- `POST /admin/content/{id}/publish` — نشر فقط إذا الحقوق معتمدة.
- `POST /admin/content/{id}/archive` — أرشفة.

## المؤذنون والقراء والخطباء
- `GET /creators?role=muezzin|qari|khatib|radoud`
- `GET /creators/{id}`
- `POST /admin/creators`
- `PATCH /admin/creators/{id}`

## الإذاعات
- `GET /radio/stations`
- `GET /radio/stations/{id}`
- `POST /admin/radio/stations`
- `PATCH /admin/radio/stations/{id}`
- `POST /admin/radio/stations/{id}/health-check` — فحص endpoint الرسمي دون تخزين/إعادة توزيع المحتوى.

## حقوق الاستخدام
- `GET /admin/licenses`
- `POST /admin/licenses`
- `PATCH /admin/licenses/{id}`
- `POST /admin/licenses/{id}/expire`
- `GET /admin/content/{id}/rights-audit`

## مواقيت الصلاة
- `GET /prayer/cities?q=`
- `GET /prayer/profiles` — يعرض الملفات المعتمدة فقط للمستخدم.
- `GET /prayer/times?city_id=&date=&profile_id=`
- `GET /prayer/compare?city_ids=&date=&profile_ids=`
- `GET /admin/prayer/profiles` — قائمة جميع الملفات بما فيها المسودات.
- `POST /admin/prayer/profiles` — إنشاء ملف حساب.
- `POST /admin/prayer/profiles/{id}/verify` — توثيق الملف بمرفق مصدر.

## أخطاء وصلاحيات
- 401 عند غياب المصادقة.
- 403 عند غياب صلاحية الإدارة أو عدم سماح الترخيص بالعملية.
- 404 عند عدم وجود المادة.
- 409 عند محاولة نشر مادة بلا ترخيص صالح.
- 422 عند معاملات غير صحيحة.
- لا تُرجع API رابط تخزين ثابتاً عاماً للمحتوى غير المنشور؛ استخدم روابط موقعة قصيرة المدة.
