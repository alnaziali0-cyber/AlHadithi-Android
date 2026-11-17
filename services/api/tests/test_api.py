import os
import tempfile
from pathlib import Path

_TEST_DB = Path(tempfile.mkdtemp()) / "api-test.db"
os.environ["APP_ENV"] = "test"
os.environ["DATABASE_URL"] = f"sqlite:///{_TEST_DB}"
os.environ["APP_SECRET"] = "test-secret-value-longer-than-thirty-two-characters"
os.environ["ADMIN_BOOTSTRAP_USERNAME"] = "test-owner"
os.environ["ADMIN_BOOTSTRAP_PASSWORD"] = "Initial-Test-Password!2026"
os.environ["ADMIN_BOOTSTRAP_EMAIL"] = "owner@example.invalid"
os.environ["COOKIE_SECURE"] = "false"
os.environ["ALLOWED_ORIGINS"] = "http://testserver"
os.environ["MEDIA_ROOT"] = str(_TEST_DB.parent / "media")

from fastapi.testclient import TestClient
from app.main import app
from app.db.session import SessionLocal
from app.models import PrayerCache


def test_public_endpoints_and_governorate_seed():
    with TestClient(app) as client:
        response = client.get('/api/v1/public/governorates')
        assert response.status_code == 200
        rows = response.json()
        assert len(rows) == 19
        assert any(row['id'] == 'halabja' for row in rows)
        times = client.get('/api/v1/public/prayer/times', params={'governorate_id': 'babil', 'day': '2026-11-13'})
        assert times.status_code == 200
        assert times.json()['method']['id'] == 'jafari_qom'
        assert times.json()['times']['islamic_midnight']
        cached_times = client.get('/api/v1/public/prayer/times', params={'governorate_id': 'babil', 'day': '2026-11-13'})
        assert cached_times.status_code == 200
        assert cached_times.json()['cache_status'] == 'cached'
        quran = client.get('/api/v1/public/quran/status')
        assert quran.status_code == 200
        assert quran.json()['status'] == 'not_ready'
        radio = client.get('/api/v1/public/radio')
        assert radio.status_code == 200
        assert radio.json()['status'] == 'not_configured'



def test_user_supplied_gps_coordinates_are_not_persisted_in_prayer_cache():
    latitude = 32.46367
    longitude = 44.41963
    with TestClient(app) as client:
        response = client.post(
            "/api/v1/public/prayer/times/coordinates",
            json={
                "latitude": latitude,
                "longitude": longitude,
                "day": "2026-11-14",
                "method": "jafari_qom",
            },
        )
        assert response.status_code == 200
        payload = response.json()
        assert payload["cache_status"] == "not_cached_user_coordinates"
        assert payload["location"]["latitude"] == latitude
        assert payload["location"]["longitude"] == longitude

    with SessionLocal() as db:
        assert db.query(PrayerCache).filter(
            PrayerCache.cache_key.contains(f"coordinates:{latitude:.4f}:{longitude:.4f}")
        ).count() == 0


def test_user_coordinate_endpoint_rejects_out_of_range_coordinates():
    with TestClient(app) as client:
        response = client.post(
            "/api/v1/public/prayer/times/coordinates",
            json={"latitude": 95, "longitude": 44, "day": "2026-11-14"},
        )
        assert response.status_code == 422


def test_requirements_pack_city_catalog_and_provenance_are_integrated():
    with TestClient(app) as client:
        response = client.get("/api/v1/public/prayer/cities")
        assert response.status_code == 200
        rows = response.json()
        assert len(rows) == 20
        assert len({row["governorate_id"] for row in rows}) == 19
        assert all(row["verified"] is False for row in rows)
        assert all(row["coordinate_accuracy"] == "approximate_city_center" for row in rows)
        assert all(row["coordinate_source"] for row in rows)

        salah_al_din = client.get(
            "/api/v1/public/prayer/cities",
            params={"governorate_id": "saladin"},
        )
        assert salah_al_din.status_code == 200
        assert {row["city_id"] for row in salah_al_din.json()} == {"IQ-SD-TIK", "IQ-SD-SAM"}

        city_times = client.get(
            "/api/v1/public/prayer/times",
            params={"city_id": "IQ-BB-HIL", "day": "2026-11-13", "method": "jafari_qom"},
        )
        assert city_times.status_code == 200
        payload = city_times.json()
        assert payload["location"]["city_id"] == "IQ-BB-HIL"
        assert payload["location"]["name_ar"] == "الحلة"
        assert payload["location"]["verified"] is False
        assert payload["location"]["coordinates_quality"] == "approximate_city_center"
        assert payload["method"]["officially_verified"] is False
        cached_city_times = client.get(
            "/api/v1/public/prayer/times",
            params={"city_id": "IQ-BB-HIL", "day": "2026-11-13", "method": "jafari_qom"},
        )
        assert cached_city_times.status_code == 200
        assert cached_city_times.json()["cache_status"] == "cached"
        assert cached_city_times.json()["location"]["city_id"] == "IQ-BB-HIL"

        governorate_times = client.get(
            "/api/v1/public/prayer/times",
            params={"governorate_id": "babil", "day": "2026-11-13", "method": "jafari_qom"},
        )
        assert governorate_times.status_code == 200
        assert governorate_times.json()["location"]["name_ar"] == "بابل"
        assert governorate_times.json()["location"]["city_id"] is None

        conflict = client.get(
            "/api/v1/public/prayer/times",
            params={"city_id": "IQ-BB-HIL", "governorate_id": "babil", "day": "2026-11-13"},
        )
        assert conflict.status_code == 422

        methods = client.get("/api/v1/public/prayer/methods")
        assert methods.status_code == 200
        assert all(item["officially_verified"] is False for item in methods.json())



def test_public_source_catalog_is_informational_and_never_grants_rights():
    with TestClient(app) as client:
        response = client.get("/api/v1/public/reference/sources")
        assert response.status_code == 200
        rows = response.json()
        assert len(rows) == 20
        assert all("contact_email" not in row for row in rows)
        radio = next(row for row in rows if row["source_id"] == "SRC-RADIO-IMAMHUSSAIN-FM")
        assert radio["license_status"] == "not_granted_contact_required"
        assert "لا يمنح" in radio["notes"]
        alislam = next(row for row in rows if row["source_id"] == "SRC-ALISLAM-AHLULBAYT-LIBRARY")
        assert alislam["license_status"] == "not_granted_contact_required"
        alfeker = next(row for row in rows if row["source_id"] == "SRC-ALFEKER-AHLULBAYT-LIBRARY")
        assert alfeker["license_status"] == "reference_only_permission_for_reuse_unconfirmed"
        assert "لم يُنزّل الأرشيف" in alfeker["notes"]
        assert "إذن صريح" in alfeker["notes"]
        shia_library = next(row for row in rows if row["source_id"] == "SRC-SHIA-LIBRARY-GHADIR")
        assert shia_library["license_status"] == "reference_only_permission_for_reuse_unconfirmed"
        tanzil = next(row for row in rows if row["source_id"] == "SRC-TANZIL-QURAN-UTHMANI")
        assert tanzil["license_status"] == "cc_by_3_0_verbatim_no_changes"
        assert "يمنع تغيير النص" in tanzil["notes"]
        quran_ws = next(row for row in rows if row["source_id"] == "SRC-QURAN-WS-KFGQPC-HAFS")
        assert quran_ws["license_status"] == "cc_by_4_0_quranws_with_kfgqpc_software_terms"
        assert "QURAN_SOURCE_NOTICE_AR.md" in quran_ws["notes"]
        aqqd = next(row for row in rows if row["source_id"] == "SRC-AQQD-QIRAAT-AUDIO-DATASET")
        assert "review_required" in aqqd["license_status"]
        assert "لم تُنزّل" in aqqd["notes"]
        quranenc = next(row for row in rows if row["source_id"] == "SRC-QURANENC-TRANSLATIONS")
        assert quranenc["license_status"] == "redistribution_allowed_with_publisher_conditions"
        assert "لا يعني اعتماداً علمياً" in quranenc["notes"]
        hadeethenc = next(row for row in rows if row["source_id"] == "SRC-HADEETHENC-TRANSLATIONS")
        assert hadeethenc["license_status"] == "redistribution_allowed_with_publisher_conditions_review_required"
        assert "لم تُضمّن قواعد" in hadeethenc["notes"]


def test_prayer_city_catalog_search_is_parameterized_and_bounded():
    with TestClient(app) as client:
        response = client.get("/api/v1/public/prayer/cities", params={"q": "الحلة"})
        assert response.status_code == 200
        rows = response.json()
        assert len(rows) == 1
        assert rows[0]["city_id"] == "IQ-BB-HIL"

def test_admin_password_rotation_csrf_and_content_publication():
    with TestClient(app) as client:
        login = client.post('/api/v1/admin/auth/login', json={'username': 'test-owner', 'password': 'Initial-Test-Password!2026'})
        assert login.status_code == 200
        csrf = login.json()['csrf_token']
        me = client.get('/api/v1/admin/auth/me')
        assert me.status_code == 200 and me.json()['force_password_change'] is True

        denied = client.post('/api/v1/admin/content', json={'content_type': 'announcement', 'title': 'test', 'status': 'published'}, headers={'X-CSRF-Token': csrf})
        assert denied.status_code == 403

        changed = client.post('/api/v1/admin/auth/change-password', json={
            'current_password': 'Initial-Test-Password!2026',
            'new_password': 'Rotated-Test-Password!2026-Strong',
        }, headers={'X-CSRF-Token': csrf})
        assert changed.status_code == 200

        login = client.post('/api/v1/admin/auth/login', json={'username': 'test-owner', 'password': 'Rotated-Test-Password!2026-Strong'})
        assert login.status_code == 200
        csrf = login.json()['csrf_token']
        missing_csrf = client.post('/api/v1/admin/content', json={'content_type': 'announcement', 'title': 'إعلان اختبار', 'status': 'published'})
        assert missing_csrf.status_code == 403
        mismatched_csrf = client.post(
            '/api/v1/admin/content',
            json={'content_type': 'announcement', 'title': 'إعلان اختبار', 'status': 'published'},
            headers={'X-CSRF-Token': 'not-the-session-token'},
        )
        assert mismatched_csrf.status_code == 403
        created = client.post('/api/v1/admin/content', json={
            'content_type': 'announcement', 'title': 'إعلان اختبار موثق',
            'body_markdown': 'محتوى اختباري.', 'status': 'published',
        }, headers={'X-CSRF-Token': csrf})
        assert created.status_code == 201
        published = client.get('/api/v1/public/content', params={'content_type': 'announcement'})
        assert published.status_code == 200
        assert published.json()[0]['title'] == 'إعلان اختبار موثق'


def test_radio_cannot_be_verified_or_enabled_without_documented_reuse_permission():
    with TestClient(app) as client:
        csrf = _login_as_test_owner(client)
        headers = {"X-CSRF-Token": csrf}

        missing_evidence = client.put("/api/v1/admin/radio", json={
            "name": "إذاعة اختبار الحقوق",
            "description": "سجل اختباري لا يمثل محطة حقيقية",
            "stream_url": "https://stream.example.invalid/live.mp3",
            "official_source_url": "https://radio.example.invalid/",
            "rights_evidence": None,
            "is_verified": True,
            "is_enabled": True,
        }, headers=headers)
        assert missing_evidence.status_code == 422

        too_short_evidence = client.put("/api/v1/admin/radio", json={
            "name": "إذاعة اختبار الحقوق",
            "stream_url": "https://stream.example.invalid/live.mp3",
            "official_source_url": "https://radio.example.invalid/",
            "rights_evidence": "إذن",
            "is_verified": True,
            "is_enabled": True,
        }, headers=headers)
        assert too_short_evidence.status_code == 422

        valid = client.put("/api/v1/admin/radio", json={
            "name": "إذاعة اختبار الحقوق",
            "description": "سجل اختباري لا يمثل محطة حقيقية",
            "stream_url": "https://stream.example.invalid/live.mp3",
            "official_source_url": "https://radio.example.invalid/",
            "rights_evidence": "إذن اختباري موثق: REF-TEST-001؛ نطاقه بث تجريبي فقط؛ لا يمثل موافقة حقيقية.",
            "is_verified": True,
            "is_enabled": True,
        }, headers=headers)
        assert valid.status_code == 200, valid.text

        admin_record = client.get("/api/v1/admin/radio")
        assert admin_record.status_code == 200
        assert admin_record.json()["rights_evidence"].startswith("إذن اختباري موثق")
        assert admin_record.json()["rights_reviewed_at"]

        public_record = client.get("/api/v1/public/radio")
        assert public_record.status_code == 200
        assert public_record.json()["status"] == "available"
        # Internal permission evidence is not exposed through the public endpoint.
        assert "rights_evidence" not in public_record.json()

        disabled = client.put("/api/v1/admin/radio", json={
            "name": "إذاعة اختبار الحقوق",
            "description": "تعطيل سجل الاختبار",
            "stream_url": "https://stream.example.invalid/live.mp3",
            "official_source_url": "https://radio.example.invalid/",
            "rights_evidence": None,
            "is_verified": False,
            "is_enabled": False,
        }, headers=headers)
        assert disabled.status_code == 200
        assert client.get("/api/v1/public/radio").json()["status"] == "not_configured"

        # Even if a legacy or manually edited DB row bypasses the CMS validator,
        # the public endpoint must fail closed without persisted evidence/date.
        from app.models import RadioChannel
        with SessionLocal() as db:
            row = db.query(RadioChannel).first()
            row.is_verified = True
            row.is_enabled = True
            row.rights_evidence = "            "
            row.rights_reviewed_at = row.updated_at
            db.commit()
        assert client.get("/api/v1/public/radio").json()["status"] == "not_configured"


def test_admin_login_rejects_untrusted_browser_origin():
    with TestClient(app) as client:
        response = client.post(
            '/api/v1/admin/auth/login',
            json={'username': 'test-owner', 'password': 'Initial-Test-Password!2026'},
            headers={'Origin': 'https://attacker.invalid'},
        )
        assert response.status_code == 403


def test_readiness_probe_confirms_database_connectivity():
    with TestClient(app) as client:
        response = client.get('/ready')
        assert response.status_code == 200
        assert response.json()['status'] == 'ready'
        assert response.json()['database'] == 'connected'


def test_quran_text_route_fails_closed_until_reviewed_import():
    with TestClient(app) as client:
        response = client.get('/api/v1/public/quran/surahs')
        assert response.status_code == 503


def test_dashboard_assets_and_security_headers_are_served():
    with TestClient(app) as client:
        dashboard = client.get('/admin')
        assert dashboard.status_code == 200
        assert 'Content-Security-Policy' in dashboard.headers
        assert 'KHAZAIN AL-HUDA' in dashboard.text
        assert 'المكتبة والمحتوى الموثّق' in dashboard.text
        assert 'style="' not in dashboard.text
        script = client.get('/admin-assets/admin.js')
        assert script.status_code == 200
        assert 'textContent' in script.text
        assert client.get('/admin-assets/../../.env').status_code == 404


def test_domain_catalog_exposes_only_published_and_provenance_gated_records():
    from uuid import uuid4
    from app.db.session import SessionLocal
    from app.models import (
        AudioFile, Book, BookPage, Dua, EthicsLesson, Hadith, HadithGrading,
        HadithSource, IslamicEvent, Visit,
    )

    marker = f"catalog-{uuid4().hex}"
    with SessionLocal() as db:
        source = HadithSource(
            title=f"Source {marker}", author="محقق اختباري", edition="طبعة اختبار",
            source_url="https://example.invalid/source", citation_policy="توثيق اختباري",
        )
        db.add(source)
        db.flush()
        book = Book(
            title=f"Book {marker}", author="مؤلف اختبار", description="وصف الكتاب",
            source_url="https://example.invalid/book", rights_status="permission_granted",
            rights_evidence="إذن نشر اختباري موثق", offline_access_allowed=True,
            publication_status="published",
        )
        hidden_book = Book(title=f"Hidden {marker}", rights_status="unverified", publication_status="published")
        missing_evidence_book = Book(title=f"No evidence {marker}", rights_status="licensed", publication_status="published", source_url="https://example.invalid/no-evidence")
        draft_book = Book(title=f"Draft {marker}", rights_status="licensed", publication_status="draft")
        db.add_all([book, hidden_book, missing_evidence_book, draft_book])
        db.flush()
        db.add_all([
            BookPage(book_id=book.id, volume_number=1, page_number=1, text_content="صفحة اختبار موثقة"),
            BookPage(book_id=book.id, volume_number=1, page_number=2, text_content="صفحة استكمال اختبار موثقة"),
            BookPage(book_id=missing_evidence_book.id, volume_number=1, page_number=1, text_content="يجب ألا تظهر هذه الصفحة"),
        ])
        missing_evidence_book_id = missing_evidence_book.id

        hadith = Hadith(
            source_id=source.id, speaker="الإمام (نموذج اختبار)", text=f"Hadith {marker}",
            chain_text="سند اختباري منفصل", hadith_number="1", publication_status="published",
        )
        db.add(hadith)
        db.flush()
        db.add(HadithGrading(
            hadith_id=hadith.id, grading="غير محسوم", scholar="باحث اختباري",
            methodology="منهجية اختبار", citation="إحالة اختبارية",
        ))
        db.add_all([
            Dua(title=f"Dua {marker}", text="نص دعاء اختباري", source_reference="المصدر: صفحة 1", publication_status="published"),
            Dua(title=f"Unreferenced dua {marker}", text="يجب ألا يظهر", source_reference="", publication_status="published"),
            Visit(title=f"Visit {marker}", text="نص زيارة اختباري", source_reference="مصدر الزيارة، صفحة 2", publication_status="published"),
            IslamicEvent(title=f"Event {marker}", description="وصف مناسبة", event_type="ذكرى", date_certainty="disputed_or_local", historical_sources=[{"title": "مصدر تاريخي اختباري"}], is_published=True),
            IslamicEvent(title=f"Unreferenced event {marker}", description="لا مصدر", event_type="ذكرى", historical_sources=[], is_published=True),
            EthicsLesson(slug=marker, title=f"Ethics {marker}", level="beginner", body_markdown="درس اختباري", publication_status="published"),
            AudioFile(title=f"Audio {marker}", category="majlis", mime_type="audio/mpeg", source_url="https://example.invalid/audio.mp3", rights_status="licensed", rights_evidence="إذن بث اختباري موثق", streaming_allowed=True, is_published=True),
            AudioFile(title=f"Downloadable audio {marker}", category="quran", mime_type="audio/mpeg", source_url="https://example.invalid/stream.mp3", download_url="https://example.invalid/download.mp3", rights_status="permission_granted", rights_evidence="إذن بث وتنزيل وتخزين اختباري", streaming_allowed=True, download_allowed=True, offline_cache_allowed=True, is_published=True),
            AudioFile(title=f"Unlicensed audio {marker}", category="majlis", mime_type="audio/mpeg", source_url="https://example.invalid/blocked.mp3", rights_status="unverified", rights_evidence="", streaming_allowed=True, is_published=True),
        ])
        db.commit()
        book_id = book.id

    with TestClient(app) as client:
        books = client.get('/api/v1/public/catalog/book', params={'search': marker}).json()
        assert books['total'] == 1
        assert books['items'][0]['title'] == f"Book {marker}"
        assert books['items'][0]['offline_access_allowed'] is True
        pages = client.get(f'/api/v1/public/books/{book_id}/pages')
        assert pages.status_code == 200
        assert pages.json()['book']['offline_access_allowed'] is True
        assert pages.json()['items'][0]['text_content'] == "صفحة اختبار موثقة"
        resumed_pages = client.get(f'/api/v1/public/books/{book_id}/pages', params={
            'volume_number': 1, 'start_page_number': 2,
        })
        assert resumed_pages.status_code == 200
        assert resumed_pages.json()['items'][0]['page_number'] == 2
        assert resumed_pages.json()['items'][0]['text_content'] == "صفحة استكمال اختبار موثقة"
        blocked_pages = client.get('/api/v1/public/books/999999/pages')
        assert blocked_pages.status_code == 404
        blocked_unproven_pages = client.get(f'/api/v1/public/books/{missing_evidence_book_id}/pages')
        assert blocked_unproven_pages.status_code == 404

        hadiths = client.get('/api/v1/public/catalog/hadith', params={'search': marker}).json()
        assert hadiths['total'] == 1
        assert hadiths['items'][0]['chain_text'] == "سند اختباري منفصل"
        assert hadiths['items'][0]['gradings'][0]['scholar'] == "باحث اختباري"
        assert hadiths['items'][0]['gradings'][0]['grading'] == "غير محسوم"

        duas = client.get('/api/v1/public/catalog/dua', params={'search': marker}).json()
        assert duas['total'] == 1
        assert duas['items'][0]['source_reference'] == "المصدر: صفحة 1"
        visits = client.get('/api/v1/public/catalog/visit', params={'search': marker}).json()
        assert visits['total'] == 1
        events = client.get('/api/v1/public/catalog/event', params={'search': marker}).json()
        assert events['total'] == 1
        assert events['items'][0]['date_certainty'] == 'disputed_or_local'
        ethics = client.get('/api/v1/public/catalog/ethics_lesson', params={'search': marker}).json()
        assert ethics['total'] == 1
        audio = client.get('/api/v1/public/catalog/audio', params={'search': marker}).json()
        assert audio['total'] == 2
        normal_audio = next(item for item in audio['items'] if item['title'] == f"Audio {marker}")
        downloadable_audio = next(item for item in audio['items'] if item['title'] == f"Downloadable audio {marker}")
        assert normal_audio['can_download'] is False
        assert normal_audio['can_cache_offline'] is False
        assert downloadable_audio['can_download'] is True
        assert downloadable_audio['can_cache_offline'] is True
        assert downloadable_audio['download_url'] == "https://example.invalid/download.mp3"
        assert client.get('/api/v1/public/catalog/not-a-section').status_code == 404
        assert client.get('/api/v1/public/catalog/book', params={'limit': 1000}).status_code == 422


def _login_as_test_owner(client):
    from app.core.security import hash_password
    from app.db.session import SessionLocal
    from app.models import AdminSession, AdminUser

    with SessionLocal() as db:
        admin = db.query(AdminUser).filter_by(username="test-owner").one()
        admin.password_hash = hash_password("CMS-Test-Password!2026-Strong")
        admin.force_password_change = False
        db.query(AdminSession).filter_by(admin_id=admin.id).delete(synchronize_session=False)
        db.commit()
    login = client.post("/api/v1/admin/auth/login", json={
        "username": "test-owner", "password": "CMS-Test-Password!2026-Strong",
    })
    assert login.status_code == 200, login.text
    return login.json()["csrf_token"]


def test_admin_catalog_cms_validates_provenance_rights_and_review_workflow():
    from uuid import uuid4

    with TestClient(app) as client:
        unauthenticated = client.get("/api/v1/admin/catalog/book")
        assert unauthenticated.status_code == 401
        csrf = _login_as_test_owner(client)
        write_headers = {"X-CSRF-Token": csrf}

        invalid_book = client.post("/api/v1/admin/catalog/book", json={
            "title": "كتاب غير موثق", "publication_status": "published",
            "rights_status": "unverified", "rights_evidence": "", "source_url": None,
        }, headers=write_headers)
        assert invalid_book.status_code == 422

        invalid_offline_book = client.post("/api/v1/admin/catalog/book", json={
            "title": "كتاب دون إذن تخزين دون اتصال", "publication_status": "draft",
            "rights_status": "unverified", "offline_access_allowed": True,
        }, headers=write_headers)
        assert invalid_offline_book.status_code == 422

        marker = uuid4().hex[:12]
        book_payload = {
            "title": f"CMS Book {marker}", "author": "مؤلف اختباري",
            "description": "وصف سجل اختباري", "volume_count": 1, "rights_status": "permission_granted",
            "rights_evidence": "إذن اختبار غير صالح للاستخدام خارج بيئة الاختبار",
            "source_url": "https://example.invalid/cms-book", "offline_access_allowed": True,
            "publication_status": "draft",
        }
        created_book = client.post("/api/v1/admin/catalog/book", json=book_payload, headers=write_headers)
        assert created_book.status_code == 201, created_book.text
        assert created_book.json()["record"]["offline_access_allowed"] is True
        book_id = created_book.json()["id"]

        out_of_range_pages = client.post(f"/api/v1/admin/books/{book_id}/pages", json={
            "pages": [{"volume_number": 2, "page_number": 1, "text_content": "مجلد غير مسجل"}],
        }, headers=write_headers)
        assert out_of_range_pages.status_code == 422
        oversized_batch = client.post(f"/api/v1/admin/books/{book_id}/pages", json={
            "pages": [{"volume_number": 1, "page_number": index + 1, "text_content": "x"} for index in range(251)],
        }, headers=write_headers)
        assert oversized_batch.status_code == 422
        duplicate_pages = client.post(f"/api/v1/admin/books/{book_id}/pages", json={
            "pages": [
                {"volume_number": 1, "page_number": 1, "text_content": "صفحة اختبار أولى"},
                {"volume_number": 1, "page_number": 1, "text_content": "صفحة مكررة"},
            ],
        }, headers=write_headers)
        assert duplicate_pages.status_code == 422
        inserted_pages = client.post(f"/api/v1/admin/books/{book_id}/pages", json={
            "pages": [
                {"volume_number": 1, "page_number": 1, "text_content": "نص صفحة اختبار واحد"},
                {"volume_number": 1, "page_number": 2, "text_content": "نص صفحة اختبار اثنين"},
            ],
        }, headers=write_headers)
        assert inserted_pages.status_code == 201
        conflicting_import = client.post(f"/api/v1/admin/books/{book_id}/pages", json={
            "pages": [
                {"volume_number": 1, "page_number": 3, "text_content": "يجب ألا يبقى هذا الصف"},
                {"volume_number": 1, "page_number": 1, "text_content": "صفحة موجودة مسبقاً"},
            ],
        }, headers=write_headers)
        assert conflicting_import.status_code == 409
        page_inventory = client.get(f"/api/v1/admin/books/{book_id}/pages")
        assert page_inventory.status_code == 200
        assert page_inventory.json()["total"] == 2
        updated_page = client.put(f"/api/v1/admin/books/{book_id}/pages/1/2", json={
            "text_content": "نص الصفحة بعد المراجعة",
        }, headers=write_headers)
        assert updated_page.status_code == 200
        published_book_payload = {**book_payload, "publication_status": "published"}
        published_book = client.put(f"/api/v1/admin/catalog/book/{book_id}", json=published_book_payload, headers=write_headers)
        assert published_book.status_code == 200, published_book.text
        assert published_book.json()["record"]["offline_access_allowed"] is True
        public_pages = client.get(f"/api/v1/public/books/{book_id}/pages")
        assert public_pages.status_code == 200
        assert public_pages.json()["book"]["offline_access_allowed"] is True
        assert public_pages.json()["items"][1]["text_content"] == "نص الصفحة بعد المراجعة"

        source = client.post("/api/v1/admin/catalog/hadith_source", json={
            "title": f"مصدر حديثي {marker}", "author": "مؤلف المصدر",
            "edition": "الطبعة الاختبارية", "source_url": "https://example.invalid/source",
            "citation_policy": "يشار إلى الجزء والصفحة أو رقم الرواية",
        }, headers=write_headers)
        assert source.status_code == 201, source.text
        source_id = source.json()["id"]

        invalid_hadith = client.post("/api/v1/admin/catalog/hadith", json={
            "source_id": source_id, "speaker": "منسوب للاختبار", "text": "نص اختباري",
            "publication_status": "published",
        }, headers=write_headers)
        assert invalid_hadith.status_code == 422
        hadith = client.post("/api/v1/admin/catalog/hadith", json={
            "source_id": source_id, "speaker": "منسوب للاختبار", "text": f"رواية {marker}",
            "chain_text": "سند كما ورد في المصدر الاختباري", "hadith_number": "42",
            "topic": "موضوع اختباري", "publication_status": "published",
        }, headers=write_headers)
        assert hadith.status_code == 201, hadith.text
        hadith_id = hadith.json()["id"]
        grading = client.post(f"/api/v1/admin/hadith/{hadith_id}/gradings", json={
            "grading": "غير محسوم", "scholar": "باحث اختباري",
            "methodology": "منهجية اختبار معلنة", "citation": "إحالة اختبارية",
        }, headers=write_headers)
        assert grading.status_code == 201, grading.text

        invalid_dua = client.post("/api/v1/admin/catalog/dua", json={
            "title": "دعاء اختباري", "text": "نص اختباري", "publication_status": "published",
        }, headers=write_headers)
        assert invalid_dua.status_code == 422
        dua = client.post("/api/v1/admin/catalog/dua", json={
            "title": f"دعاء {marker}", "text": "نص دعاء اختباري", "source_id": source_id,
            "source_reference": "المصدر الاختباري، الصفحة 15", "publication_status": "published",
        }, headers=write_headers)
        assert dua.status_code == 201, dua.text

        invalid_visit = client.post("/api/v1/admin/catalog/visit", json={
            "title": "زيارة اختبارية", "text": "نص اختباري", "publication_status": "published",
        }, headers=write_headers)
        assert invalid_visit.status_code == 422
        visit = client.post("/api/v1/admin/catalog/visit", json={
            "title": f"زيارة {marker}", "text": "نص زيارة اختباري",
            "source_reference": "المصدر الاختباري، الصفحة 20", "publication_status": "published",
        }, headers=write_headers)
        assert visit.status_code == 201, visit.text

        invalid_event = client.post("/api/v1/admin/catalog/event", json={
            "title": "مناسبة اختبارية", "event_type": "ذكرى", "is_published": True,
            "hijri_month": 1, "hijri_day": 1, "historical_sources": [],
        }, headers=write_headers)
        assert invalid_event.status_code == 422
        event = client.post("/api/v1/admin/catalog/event", json={
            "title": f"مناسبة {marker}", "event_type": "ذكرى",
            "hijri_month": 1, "hijri_day": 1, "date_certainty": "disputed_or_local",
            "historical_sources": [{"title": "مصدر تاريخي اختباري", "locator": "الجزء 1، الصفحة 2"}],
            "is_published": True,
        }, headers=write_headers)
        assert event.status_code == 201, event.text

        invalid_lesson = client.post("/api/v1/admin/catalog/ethics_lesson", json={
            "slug": f"ethics-{marker}", "title": "درس اختباري", "body_markdown": "نص الدرس",
            "publication_status": "published",
        }, headers=write_headers)
        assert invalid_lesson.status_code == 422
        lesson = client.post("/api/v1/admin/catalog/ethics_lesson", json={
            "slug": f"ethics-{marker}", "title": f"درس {marker}", "body_markdown": "نص درس اختباري",
            "hadith_id": hadith_id, "publication_status": "published",
        }, headers=write_headers)
        assert lesson.status_code == 201, lesson.text

        invalid_audio = client.post("/api/v1/admin/catalog/audio", json={
            "title": "صوت اختباري", "category": "quran", "mime_type": "audio/mpeg",
            "source_url": "https://example.invalid/audio.mp3", "is_published": True,
            "rights_status": "unverified", "rights_evidence": "",
        }, headers=write_headers)
        assert invalid_audio.status_code == 422
        download_only_audio = client.post("/api/v1/admin/catalog/audio", json={
            "title": "صوت قابل للتنزيل فقط", "category": "quran", "mime_type": "audio/mpeg",
            "source_url": "https://example.invalid/stream.mp3", "download_url": "https://example.invalid/download.mp3",
            "is_published": True, "rights_status": "permission_granted", "rights_evidence": "إذن اختبار",
            "download_allowed": True,
        }, headers=write_headers)
        assert download_only_audio.status_code == 422
        audio = client.post("/api/v1/admin/catalog/audio", json={
            "title": f"صوت {marker}", "category": "quran", "mime_type": "audio/mpeg",
            "source_url": "https://example.invalid/audio.mp3", "is_published": True,
            "rights_status": "permission_granted", "rights_evidence": "إذن اختباري موثق",
            "streaming_allowed": True,
        }, headers=write_headers)
        assert audio.status_code == 201, audio.text

        public_hadith = client.get("/api/v1/public/catalog/hadith", params={"search": marker})
        assert public_hadith.status_code == 200
        assert public_hadith.json()["total"] == 1
        assert public_hadith.json()["items"][0]["gradings"][0]["grading"] == "غير محسوم"
        assert client.get("/api/v1/public/catalog/dua", params={"search": marker}).json()["total"] == 1
        assert client.get("/api/v1/public/catalog/visit", params={"search": marker}).json()["total"] == 1
        assert client.get("/api/v1/public/catalog/event", params={"search": marker}).json()["total"] == 1
        assert client.get("/api/v1/public/catalog/ethics_lesson", params={"search": marker}).json()["total"] == 1
        assert client.get("/api/v1/public/catalog/audio", params={"search": marker}).json()["total"] == 1

        admin_list = client.get("/api/v1/admin/catalog/book")
        assert admin_list.status_code == 200 and admin_list.json()["total"] >= 1
        archived = client.delete(f"/api/v1/admin/catalog/book/{book_id}", headers=write_headers)
        assert archived.status_code == 200
        assert client.get(f"/api/v1/public/books/{book_id}/pages").status_code == 404
        assert client.get("/api/v1/admin/catalog/unknown").status_code == 404


def test_admin_catalog_requires_csrf_for_mutations_and_rejects_unknown_fields():
    with TestClient(app) as client:
        _login_as_test_owner(client)
        no_csrf = client.post("/api/v1/admin/catalog/book", json={"title": "بدون CSRF"})
        assert no_csrf.status_code == 403
        csrf = client.cookies.get("kh_admin_csrf")
        assert csrf
        unknown_field = client.post("/api/v1/admin/catalog/book", json={
            "title": "حقل زائد", "is_admin": True,
        }, headers={"X-CSRF-Token": csrf})
        assert unknown_field.status_code == 422


def test_device_favorites_and_book_reading_progress_are_scoped_and_provenance_gated():
    from uuid import uuid4
    from app.db.session import SessionLocal
    from app.models import Book, BookPage

    marker = uuid4().hex[:12]
    with SessionLocal() as db:
        book = Book(
            title=f"Favorite Book {marker}", author="مؤلف اختباري",
            description="كتاب اختبار للمفضلة وموضع القراءة",
            source_url="https://example.invalid/favorite-book",
            rights_status="permission_granted",
            rights_evidence="دليل حقوق اختباري لاستخدام الاختبار فقط",
            publication_status="published", volume_count=2,
        )
        unverified = Book(
            title=f"Unverified Book {marker}", source_url="https://example.invalid/unverified",
            rights_status="unverified", rights_evidence=None, publication_status="published",
        )
        db.add_all([book, unverified])
        db.flush()
        db.add_all([
            BookPage(book_id=book.id, volume_number=1, page_number=1, text_content="صفحة أولى للاختبار"),
            BookPage(book_id=book.id, volume_number=1, page_number=2, text_content="صفحة ثانية للاختبار"),
            BookPage(book_id=book.id, volume_number=2, page_number=1, text_content="صفحة أولى من المجلد الثاني"),
        ])
        book_id = book.id
        unverified_id = unverified.id
        db.commit()

    first_device = "device_key_for_favorites_test_0001"
    second_device = "device_key_for_favorites_test_0002"
    with TestClient(app) as client:
        assert client.get("/api/v1/user/favorites").status_code == 422
        assert client.get("/api/v1/user/favorites", headers={"X-Device-Key": "short"}).status_code == 422

        blocked = client.post("/api/v1/user/favorites", headers={"X-Device-Key": first_device}, json={
            "entity_type": "book", "entity_id": str(unverified_id),
        })
        assert blocked.status_code == 404

        public_pages = client.get(f"/api/v1/public/books/{book_id}/pages")
        assert public_pages.status_code == 200
        assert public_pages.json()["book"]["offline_access_allowed"] is False

        payload = {"entity_type": "book", "entity_id": str(book_id)}
        saved = client.post("/api/v1/user/favorites", headers={"X-Device-Key": first_device}, json=payload)
        assert saved.status_code == 200 and saved.json()["saved"] is True
        repeated = client.post("/api/v1/user/favorites", headers={"X-Device-Key": first_device}, json=payload)
        assert repeated.status_code == 200
        first_list = client.get("/api/v1/user/favorites", headers={"X-Device-Key": first_device}).json()
        second_list = client.get("/api/v1/user/favorites", headers={"X-Device-Key": second_device}).json()
        assert first_list["total"] == 1
        assert first_list["items"][0]["title"] == f"Favorite Book {marker}"
        assert second_list["total"] == 0

        from app.core.security import keyed_hash
        from app.models import Favorite
        with SessionLocal() as db:
            db.add_all([
                Favorite(
                    user_id=None, device_key_hash=keyed_hash(second_device),
                    entity_type="book", entity_id=str(900000000 - index),
                )
                for index in range(100)
            ])
            db.commit()
        capped = client.post("/api/v1/user/favorites", headers={"X-Device-Key": second_device}, json=payload)
        assert capped.status_code == 409

        from sqlalchemy.exc import IntegrityError
        from app.models import Favorite
        with SessionLocal() as db:
            favorite_row = db.query(Favorite).filter_by(
                user_id=None, entity_type="book", entity_id=str(book_id),
            ).one()
            assert favorite_row.device_key_hash != first_device
            assert len(favorite_row.device_key_hash) == 64
            db.add(Favorite(
                user_id=None, device_key_hash=favorite_row.device_key_hash,
                entity_type="book", entity_id=str(book_id),
            ))
            try:
                db.commit()
                raise AssertionError("The device favorite unique index did not reject a duplicate")
            except IntegrityError:
                db.rollback()

        denied_extra = client.post("/api/v1/user/favorites", headers={"X-Device-Key": first_device}, json={
            "entity_type": "book", "entity_id": str(book_id), "admin": True,
        })
        assert denied_extra.status_code == 422

        invalid_progress = client.put(
            f"/api/v1/user/books/{book_id}/reading-progress",
            headers={"X-Device-Key": first_device},
            json={"volume_number": 1, "page_number": 99},
        )
        assert invalid_progress.status_code == 404
        invalid_volume = client.put(
            f"/api/v1/user/books/{book_id}/reading-progress",
            headers={"X-Device-Key": first_device},
            json={"volume_number": 3, "page_number": 1},
        )
        assert invalid_volume.status_code == 422

        saved_progress = client.put(
            f"/api/v1/user/books/{book_id}/reading-progress",
            headers={"X-Device-Key": first_device},
            json={"volume_number": 1, "page_number": 2},
        )
        assert saved_progress.status_code == 200, saved_progress.text
        assert saved_progress.json()["page_number"] == 2
        updated_progress = client.put(
            f"/api/v1/user/books/{book_id}/reading-progress",
            headers={"X-Device-Key": first_device},
            json={"volume_number": 1, "page_number": 1},
        )
        assert updated_progress.status_code == 200
        restored = client.get(
            f"/api/v1/user/books/{book_id}/reading-progress",
            headers={"X-Device-Key": first_device},
        )
        assert restored.status_code == 200
        assert restored.json()["progress"]["page_number"] == 1
        isolated = client.get(
            f"/api/v1/user/books/{book_id}/reading-progress",
            headers={"X-Device-Key": second_device},
        )
        assert isolated.status_code == 200 and isolated.json()["progress"] is None

        removed = client.delete(
            f"/api/v1/user/favorites/book/{book_id}", headers={"X-Device-Key": first_device},
        )
        assert removed.status_code == 200 and removed.json()["removed"] is True
        assert client.get("/api/v1/user/favorites", headers={"X-Device-Key": first_device}).json()["total"] == 0


def test_reviewed_quran_reader_route_requires_complete_import_and_returns_verified_structure():
    """A synthetic, test-only dataset exercises the route contract; it is never shipped."""
    from datetime import datetime, timezone, timedelta
    from scripts.import_tanzil import AYAH_COUNTS
    from app.db.session import SessionLocal
    from app.models import AdminUser, QuranAyah, QuranImport, QuranSurah

    with TestClient(app) as client:
        assert client.get('/api/v1/public/quran/surahs/1/ayahs').status_code == 503
        assert client.get('/api/v1/public/quran/surahs/0/ayahs').status_code == 422
        assert client.get('/api/v1/public/quran/surahs/1/ayahs', params={'limit': 301}).status_code == 422

    with SessionLocal() as db:
        owner = db.query(AdminUser).filter_by(username='test-owner').one()
        index = 0
        surahs = [
            QuranSurah(number=number, name_ar=f'سورة اختبار {number}', name_en=f'Test Surah {number}', ayah_count=count)
            for number, count in enumerate(AYAH_COUNTS, start=1)
        ]
        ayahs = []
        for surah_number, count in enumerate(AYAH_COUNTS, start=1):
            for ayah_number in range(1, count + 1):
                ayahs.append(QuranAyah(
                    surah_number=surah_number,
                    ayah_number=ayah_number,
                    text_uthmani='نص اختبار غير قرآني لا يُستخدم في التطبيق',
                    juz_number=min(30, (index * 30) // 6236 + 1),
                    hizb_number=min(60, (index * 60) // 6236 + 1),
                    page_number=min(604, (index * 604) // 6236 + 1),
                    sajdah=False,
                ))
                index += 1
        db.add_all(surahs)
        db.flush()
        db.add_all(ayahs)
        db.add(QuranImport(
            source_name='Synthetic test-only fixture',
            source_version='test-only',
            license_text='Synthetic fixture; not Qur\'anic text and not for application use',
            text_sha256='a' * 64,
            surah_count=114,
            ayah_count=6236,
            approved_by=owner.id,
            reviewer_name='Test Fixture Only',
            reviewer_credential='Not a scholarly review',
            review_notes='Test data only. Never publish or ship.',
            reviewed_at=datetime.now(timezone.utc),
        ))
        db.commit()

    try:
        with TestClient(app) as client:
            first_page = client.get('/api/v1/public/quran/surahs/2/ayahs', params={'limit': 2, 'offset': 0})
            assert first_page.status_code == 200, first_page.text
            payload = first_page.json()
            assert payload['surah']['number'] == 2
            assert payload['total'] == AYAH_COUNTS[1]
            assert len(payload['items']) == 2
            assert [item['ayah_number'] for item in payload['items']] == [1, 2]
            assert all(item['text_uthmani'] == 'نص اختبار غير قرآني لا يُستخدم في التطبيق' for item in payload['items'])
            bookmark = client.post('/api/v1/user/favorites', headers={'X-Device-Key': 'quran_bookmark_test_device_key_0001'}, json={
                'entity_type': 'quran_ayah', 'entity_id': '2:2',
            })
            assert bookmark.status_code == 200, bookmark.text
            saved_favorites = client.get('/api/v1/user/favorites', headers={'X-Device-Key': 'quran_bookmark_test_device_key_0001'})
            assert saved_favorites.status_code == 200
            assert saved_favorites.json()['items'][0]['entity_id'] == '2:2'

            with SessionLocal() as db:
                pending_import = QuranImport(
                    source_name='Newer pending test-only import',
                    source_version='pending-test-only',
                    license_text='Test-only pending snapshot',
                    text_sha256='b' * 64,
                    surah_count=114,
                    ayah_count=6236,
                    imported_at=datetime.now(timezone.utc) + timedelta(seconds=5),
                )
                db.add(pending_import)
                db.commit()
                pending_import_id = pending_import.id
            hidden_after_new_pending_import = client.get('/api/v1/user/favorites', headers={'X-Device-Key': 'quran_bookmark_test_device_key_0001'})
            assert hidden_after_new_pending_import.status_code == 200
            assert hidden_after_new_pending_import.json()['items'][0]['available'] is False
            blocked_during_pending_import = client.post('/api/v1/user/favorites', headers={'X-Device-Key': 'quran_bookmark_test_device_key_0001'}, json={
                'entity_type': 'quran_ayah', 'entity_id': '2:3',
            })
            assert blocked_during_pending_import.status_code == 404
            with SessionLocal() as db:
                db.query(QuranImport).filter_by(id=pending_import_id).delete(synchronize_session=False)
                db.commit()

            last_page = client.get('/api/v1/public/quran/surahs/2/ayahs', params={'limit': 2, 'offset': AYAH_COUNTS[1] - 1})
            assert last_page.status_code == 200
            assert [item['ayah_number'] for item in last_page.json()['items']] == [AYAH_COUNTS[1]]

            with SessionLocal() as db:
                missing = db.query(QuranAyah).filter_by(surah_number=2, ayah_number=2).one()
                db.delete(missing)
                db.commit()
            integrity_failure = client.get('/api/v1/public/quran/surahs/2/ayahs')
            assert integrity_failure.status_code == 503
            with SessionLocal() as db:
                db.add(QuranAyah(
                    surah_number=2, ayah_number=2,
                    text_uthmani='نص اختبار غير قرآني لا يُستخدم في التطبيق',
                    juz_number=1, hizb_number=1, page_number=1, sajdah=False,
                ))
                db.commit()
    finally:
        with SessionLocal() as db:
            db.query(QuranAyah).delete(synchronize_session=False)
            db.query(QuranSurah).delete(synchronize_session=False)
            db.query(QuranImport).delete(synchronize_session=False)
            db.commit()
