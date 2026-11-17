import hashlib
import hmac
import secrets
from datetime import datetime, timedelta, timezone
from argon2 import PasswordHasher
from argon2.exceptions import VerifyMismatchError, VerificationError
from app.core.config import get_settings

_hasher = PasswordHasher(time_cost=3, memory_cost=65536, parallelism=2, hash_len=32, salt_len=16)


def hash_password(password: str) -> str:
    if len(password) < 14 or len(password) > 256:
        raise ValueError("Password must contain 14 to 256 characters")
    if not (any(c.islower() for c in password) and any(c.isupper() for c in password)
            and any(c.isdigit() for c in password) and any(not c.isalnum() for c in password)):
        raise ValueError("Password must include upper-case, lower-case, numeric, and symbol characters")
    return _hasher.hash(password)


def verify_password(password_hash: str, password: str) -> bool:
    try:
        return _hasher.verify(password_hash, password)
    except (VerifyMismatchError, VerificationError, ValueError):
        return False


def new_secret() -> str:
    return secrets.token_urlsafe(48)


def keyed_hash(value: str) -> str:
    secret = get_settings().app_secret or "development-only-not-for-production"
    return hmac.new(secret.encode(), value.encode(), hashlib.sha256).hexdigest()


def constant_time_equal(left: str, right: str) -> bool:
    return hmac.compare_digest(left.encode(), right.encode())


def session_expiry() -> datetime:
    return datetime.now(timezone.utc) + timedelta(hours=8)


def fingerprint(value: str | None) -> str | None:
    if not value:
        return None
    return hashlib.sha256(value.encode("utf-8", "ignore")).hexdigest()
