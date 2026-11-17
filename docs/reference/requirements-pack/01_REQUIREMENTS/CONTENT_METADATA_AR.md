# حقول البيانات الموحدة للمحتوى

الحقول الأساسية لكل مادة: `id`, `title`, `content_type`, `description`, `language`, `creator_id`, `organization_id`, `source_url`, `storage_key`, `mime_type`, `duration_seconds`, `file_size_bytes`, `checksum_sha256`, `license_id`, `rights_status`, `review_status`, `published_at`, `created_at`, `updated_at`.

## قواعد التسجيل
- لا تستخدم اسم الملف وحده كعنوان للمادة.
- خزّن اسم الملف الأصلي واسم التخزين الداخلي منفصلين.
- خزّن SHA-256 للتحقق من سلامة الملف ومنع التكرار.
- سجّل صاحب الحقوق والمصدر والوثيقة الداعمة.
- اجعل `broadcast_allowed`, `download_allowed`, `offline_allowed`, `commercial_allowed` حقولاً مستقلة؛ لا تستنتج أحدها من الآخر.
- تاريخ انتهاء الترخيص يجب أن يمنع العرض أو يوقفه تلقائياً عند تجاوزه إلى أن يراجع المسؤول الحالة.
