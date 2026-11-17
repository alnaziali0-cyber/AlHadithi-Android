from datetime import datetime, timezone
from fastapi import Depends, HTTPException, Request, status
from sqlalchemy.orm import Session
from app.db.session import SessionLocal
from app.models import AdminSession, AdminUser
from app.core.security import keyed_hash, constant_time_equal


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def current_admin(request: Request, db: Session = Depends(get_db)) -> AdminUser:
    token = request.cookies.get("kh_admin_session")
    if not token:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Authentication required")
    session = db.query(AdminSession).filter(AdminSession.token_hash == keyed_hash(token)).first()
    now = datetime.now(timezone.utc)
    if not session or session.revoked_at is not None:
        raise HTTPException(status_code=401, detail="Invalid session")
    expiry = session.expires_at
    if expiry.tzinfo is None:
        expiry = expiry.replace(tzinfo=timezone.utc)
    if expiry <= now:
        session.revoked_at = now
        db.commit()
        raise HTTPException(status_code=401, detail="Session expired")
    admin = db.get(AdminUser, session.admin_id)
    if not admin or not admin.is_active:
        raise HTTPException(status_code=403, detail="Administrator disabled")
    if admin.role != "owner":
        raise HTTPException(status_code=403, detail="Insufficient administrator role")
    request.state.admin_session = session
    request.state.admin_user = admin
    return admin


def require_admin_csrf(request: Request, admin: AdminUser = Depends(current_admin)) -> AdminUser:
    session = request.state.admin_session
    cookie_token = request.cookies.get("kh_admin_csrf", "")
    header_token = request.headers.get("x-csrf-token", "")
    if not cookie_token or not header_token or not constant_time_equal(cookie_token, header_token):
        raise HTTPException(status_code=403, detail="CSRF validation failed")
    if not constant_time_equal(keyed_hash(cookie_token), session.csrf_hash):
        raise HTTPException(status_code=403, detail="CSRF token is not bound to this session")
    return admin
