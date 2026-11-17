from fastapi.testclient import TestClient

from app.core.security import hash_password
from app.db.session import SessionLocal
from app.main import app
from app.models import AdminSession, AdminUser, AdminAuditLog


def _login_as_owner(client: TestClient) -> str:
    with SessionLocal() as db:
        admin = db.query(AdminUser).filter_by(username="test-owner").first()
        if admin is None:
            admin = AdminUser(
                username="test-owner",
                email="owner@example.invalid",
                password_hash=hash_password("Wallpapers-Test-Password!2026"),
                role="owner",
                is_active=True,
                force_password_change=False,
            )
            db.add(admin)
            db.flush()
        admin.password_hash = hash_password("Wallpapers-Test-Password!2026")
        admin.role = "owner"
        admin.is_active = True
        admin.force_password_change = False
        db.query(AdminSession).filter_by(admin_id=admin.id).delete(synchronize_session=False)
        db.commit()
    response = client.post(
        "/api/v1/admin/auth/login",
        json={"username": "test-owner", "password": "Wallpapers-Test-Password!2026"},
    )
    assert response.status_code == 200, response.text
    return response.json()["csrf_token"]


def test_wallpaper_publication_is_owner_only_and_hides_drafts():
    with TestClient(app, base_url="https://testserver") as client:
        public_url = "/api/v1/public/wallpapers"
        admin_url = "/api/v1/admin/wallpapers"
        initial = client.get(public_url)
        assert initial.status_code == 200
        ids = {item["id"] for item in initial.json()["items"]}
        assert ids == {"karbala-shrine-night", "najaf-imam-ali-shrine"}
        assert all("source" not in key and "url" not in key for item in initial.json()["items"] for key in item)
        assert client.get(admin_url).status_code == 401

        csrf = _login_as_owner(client)
        headers = {"X-CSRF-Token": csrf}
        admin_rows = client.get(admin_url)
        assert admin_rows.status_code == 200
        assert {item["id"] for item in admin_rows.json()["items"]} == ids
        assert all(item["published"] for item in admin_rows.json()["items"])

        hidden_id = "karbala-shrine-night"
        csrf_failure = client.put(f"{admin_url}/{hidden_id}", json={"published": False})
        assert csrf_failure.status_code == 403
        unknown = client.put(f"{admin_url}/not-an-asset", json={"published": False}, headers=headers)
        assert unknown.status_code == 404
        invalid_payload = client.put(
            f"{admin_url}/{hidden_id}",
            json={"published": False, "asset_path": "https://example.invalid/image.jpg"},
            headers=headers,
        )
        assert invalid_payload.status_code == 422

        hidden = client.put(f"{admin_url}/{hidden_id}", json={"published": False}, headers=headers)
        assert hidden.status_code == 200
        assert hidden.json() == {"id": hidden_id, "published": False}
        assert hidden_id not in {item["id"] for item in client.get(public_url).json()["items"]}
        admin_state = client.get(admin_url).json()["items"]
        assert next(item for item in admin_state if item["id"] == hidden_id)["published"] is False

        restored = client.put(f"{admin_url}/{hidden_id}", json={"published": True}, headers=headers)
        assert restored.status_code == 200
        assert hidden_id in {item["id"] for item in client.get(public_url).json()["items"]}

    with SessionLocal() as db:
        actions = db.query(AdminAuditLog).filter(
            AdminAuditLog.resource_type == "bundled_wallpaper",
            AdminAuditLog.resource_id == "karbala-shrine-night",
        ).all()
        assert {row.action for row in actions} >= {"wallpaper.hidden", "wallpaper.published"}
