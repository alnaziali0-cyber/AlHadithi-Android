import pytest

from app.core.config import Settings


def production_settings(**overrides):
    values = {
        "app_env": "production",
        "app_secret": "s" * 48,
        "database_url": "postgresql+psycopg://khazain:password@db:5432/khazain",
        "cookie_secure": True,
        "allowed_origins": "https://app.example.org,https://admin.example.org",
    }
    values.update(overrides)
    return Settings(_env_file=None, **values)


def test_production_settings_accept_secure_postgresql_configuration():
    production_settings().validate_production()


@pytest.mark.parametrize(
    "overrides",
    [
        {"app_secret": "short"},
        {"database_url": "sqlite:////tmp/production.db"},
        {"database_url": "postgresql://khazain:password@db:5432/khazain"},
        {"cookie_secure": False},
        {"allowed_origins": ""},
        {"allowed_origins": "http://app.example.org"},
        {"allowed_origins": "https://app.example.org/admin"},
        {"allowed_origins": "https://user:pass@app.example.org"},
        {"app_secret": "replace-with-a-random-32-byte-or-longer-secret"},
    ],
)
def test_production_settings_reject_insecure_or_ambiguous_values(overrides):
    with pytest.raises(RuntimeError):
        production_settings(**overrides).validate_production()


def test_upload_and_cache_limits_are_bounded():
    with pytest.raises(ValueError):
        Settings(_env_file=None, max_upload_bytes=0)
    with pytest.raises(ValueError):
        Settings(_env_file=None, public_api_cache_seconds=10)
