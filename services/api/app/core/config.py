from functools import lru_cache
from pathlib import Path
from pydantic import field_validator
from urllib.parse import urlparse
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    app_env: str = "development"
    app_secret: str = ""
    database_url: str = "sqlite:///./khazain_dev.db"
    admin_bootstrap_username: str = ""
    admin_bootstrap_password: str = ""
    admin_bootstrap_email: str = ""
    cookie_secure: bool = True
    allowed_origins: str = ""
    media_root: str = "./var/media"
    max_upload_bytes: int = 15 * 1024 * 1024
    public_api_cache_seconds: int = 300

    @property
    def origins(self) -> list[str]:
        return [value.strip().rstrip("/") for value in self.allowed_origins.split(",") if value.strip()]

    @property
    def media_path(self) -> Path:
        path = Path(self.media_root).expanduser().resolve()
        path.mkdir(parents=True, exist_ok=True)
        return path

    @field_validator("max_upload_bytes")
    @classmethod
    def validate_upload_limit(cls, value: int) -> int:
        if not 1 <= value <= 100 * 1024 * 1024:
            raise ValueError("MAX_UPLOAD_BYTES must be between 1 byte and 100 MiB")
        return value

    @field_validator("public_api_cache_seconds")
    @classmethod
    def validate_cache_ttl(cls, value: int) -> int:
        if not 60 <= value <= 86400:
            raise ValueError("PUBLIC_API_CACHE_SECONDS must be between 60 and 86400")
        return value

    def validate_production(self) -> None:
        if self.app_env.lower() != "production":
            return
        if len(self.app_secret) < 32 or self.app_secret in {
            "replace-with-a-random-32-byte-or-longer-secret",
            "replace-with-at-least-32-random-bytes",
            "development-only-not-for-production",
        }:
            raise RuntimeError("APP_SECRET must be a unique secret of at least 32 characters in production")
        if not self.database_url.startswith("postgresql+psycopg://"):
            raise RuntimeError("Production requires PostgreSQL through the installed psycopg 3 driver")
        if not self.cookie_secure:
            raise RuntimeError("COOKIE_SECURE must be true in production")
        if not self.origins:
            raise RuntimeError("ALLOWED_ORIGINS must be explicit in production")
        for origin in self.origins:
            parsed = urlparse(origin)
            if parsed.scheme != "https" or not parsed.netloc or parsed.path or parsed.query or parsed.fragment or parsed.username or parsed.password:
                raise RuntimeError("Every production ALLOWED_ORIGINS entry must be an HTTPS origin without a path or credentials")


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()
