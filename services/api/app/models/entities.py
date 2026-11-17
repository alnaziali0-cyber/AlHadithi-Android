from datetime import date, datetime, timezone
from sqlalchemy import Boolean, CheckConstraint, Date, DateTime, ForeignKey, Index, Integer, JSON, String, Text, UniqueConstraint, false, text
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.db.base import Base


def utcnow() -> datetime:
    return datetime.now(timezone.utc)


class Governorate(Base):
    __tablename__ = "governorates"
    id: Mapped[str] = mapped_column(String(32), primary_key=True)
    name_ar: Mapped[str] = mapped_column(String(100), nullable=False)
    name_en: Mapped[str] = mapped_column(String(100), nullable=False)
    latitude: Mapped[float] = mapped_column(nullable=False)
    longitude: Mapped[float] = mapped_column(nullable=False)
    timezone: Mapped[str] = mapped_column(String(64), nullable=False, default="Asia/Baghdad")
    coordinates_quality: Mapped[str] = mapped_column(String(32), nullable=False, default="governorate_center_approximate")


class PrayerCity(Base):
    __tablename__ = "prayer_cities"
    city_id: Mapped[str] = mapped_column(String(32), primary_key=True)
    governorate_id: Mapped[str] = mapped_column(ForeignKey("governorates.id", ondelete="RESTRICT"), nullable=False)
    governorate_name_ar: Mapped[str] = mapped_column(String(100), nullable=False)
    city_name_ar: Mapped[str] = mapped_column(String(100), nullable=False)
    city_name_en: Mapped[str] = mapped_column(String(100), nullable=False)
    latitude: Mapped[float] = mapped_column(nullable=False)
    longitude: Mapped[float] = mapped_column(nullable=False)
    timezone: Mapped[str] = mapped_column(String(64), nullable=False, default="Asia/Baghdad")
    coordinate_source: Mapped[str] = mapped_column(String(250), nullable=False)
    coordinate_accuracy: Mapped[str] = mapped_column(String(32), nullable=False, default="approximate_city_center")
    verified: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    __table_args__ = (
        Index("ix_prayer_cities_governorate", "governorate_id"),
        CheckConstraint("latitude >= -90 AND latitude <= 90", name="ck_prayer_city_latitude"),
        CheckConstraint("longitude >= -180 AND longitude <= 180", name="ck_prayer_city_longitude"),
    )


class AdminUser(Base):
    __tablename__ = "admin_users"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    username: Mapped[str] = mapped_column(String(80), unique=True, nullable=False)
    email: Mapped[str] = mapped_column(String(254), nullable=False)
    password_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    role: Mapped[str] = mapped_column(String(24), nullable=False, default="owner")
    is_active: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    force_password_change: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)
    last_login_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    sessions: Mapped[list["AdminSession"]] = relationship(back_populates="admin", cascade="all, delete-orphan")


class AdminSession(Base):
    __tablename__ = "admin_sessions"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    admin_id: Mapped[int] = mapped_column(ForeignKey("admin_users.id", ondelete="CASCADE"), nullable=False)
    token_hash: Mapped[str] = mapped_column(String(64), unique=True, nullable=False)
    csrf_hash: Mapped[str] = mapped_column(String(64), nullable=False)
    expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    revoked_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)
    last_seen_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)
    ip_fingerprint: Mapped[str | None] = mapped_column(String(64))
    user_agent_fingerprint: Mapped[str | None] = mapped_column(String(64))
    admin: Mapped[AdminUser] = relationship(back_populates="sessions")


class LoginThrottle(Base):
    __tablename__ = "login_throttles"
    key_hash: Mapped[str] = mapped_column(String(64), primary_key=True)
    failures: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    window_started_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    blocked_until: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))


class AdminAuditLog(Base):
    __tablename__ = "admin_audit_logs"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    admin_id: Mapped[int | None] = mapped_column(ForeignKey("admin_users.id", ondelete="SET NULL"))
    action: Mapped[str] = mapped_column(String(120), nullable=False)
    resource_type: Mapped[str] = mapped_column(String(80), nullable=False)
    resource_id: Mapped[str | None] = mapped_column(String(100))
    details: Mapped[dict] = mapped_column(JSON, default=dict, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)
    ip_fingerprint: Mapped[str | None] = mapped_column(String(64))


class ContentItem(Base):
    __tablename__ = "content_items"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    content_type: Mapped[str] = mapped_column(String(32), nullable=False)
    title: Mapped[str] = mapped_column(String(180), nullable=False)
    body_markdown: Mapped[str] = mapped_column(Text, nullable=False, default="")
    image_asset_id: Mapped[int | None] = mapped_column(ForeignKey("media_assets.id", ondelete="SET NULL"))
    target_url: Mapped[str | None] = mapped_column(String(2048))
    button_label: Mapped[str | None] = mapped_column(String(60))
    status: Mapped[str] = mapped_column(String(20), nullable=False, default="draft")
    starts_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    ends_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    created_by: Mapped[int] = mapped_column(ForeignKey("admin_users.id", ondelete="RESTRICT"), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, onupdate=utcnow, nullable=False)
    __table_args__ = (Index("ix_content_publication", "status", "starts_at", "ends_at"),)


class MediaAsset(Base):
    __tablename__ = "media_assets"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    original_name: Mapped[str] = mapped_column(String(255), nullable=False)
    storage_name: Mapped[str] = mapped_column(String(100), unique=True, nullable=False)
    mime_type: Mapped[str] = mapped_column(String(80), nullable=False)
    byte_size: Mapped[int] = mapped_column(Integer, nullable=False)
    sha256: Mapped[str] = mapped_column(String(64), nullable=False)
    created_by: Mapped[int] = mapped_column(ForeignKey("admin_users.id", ondelete="RESTRICT"), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)


class RadioChannel(Base):
    __tablename__ = "radio_channels"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    name: Mapped[str] = mapped_column(String(160), nullable=False)
    description: Mapped[str] = mapped_column(Text, nullable=False, default="")
    stream_url: Mapped[str | None] = mapped_column(String(2048))
    official_source_url: Mapped[str | None] = mapped_column(String(2048))
    # A checked URL is not a reuse licence. Keep the operator's evidence separate.
    rights_evidence: Mapped[str | None] = mapped_column(Text)
    rights_reviewed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    is_verified: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    is_enabled: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, onupdate=utcnow, nullable=False)


class PrayerCache(Base):
    __tablename__ = "prayer_cache"
    cache_key: Mapped[str] = mapped_column(String(100), primary_key=True)
    payload: Mapped[dict] = mapped_column(JSON, nullable=False)
    valid_until: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)


class QuranImport(Base):
    __tablename__ = "quran_imports"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    source_name: Mapped[str] = mapped_column(String(120), nullable=False)
    source_version: Mapped[str] = mapped_column(String(40), nullable=False)
    license_text: Mapped[str] = mapped_column(Text, nullable=False)
    text_sha256: Mapped[str] = mapped_column(String(64), nullable=False)
    surah_count: Mapped[int] = mapped_column(Integer, nullable=False)
    ayah_count: Mapped[int] = mapped_column(Integer, nullable=False)
    imported_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)
    approved_by: Mapped[int | None] = mapped_column(ForeignKey("admin_users.id", ondelete="SET NULL"))
    reviewer_name: Mapped[str | None] = mapped_column(String(180))
    reviewer_credential: Mapped[str | None] = mapped_column(String(250))
    review_notes: Mapped[str | None] = mapped_column(Text)
    reviewed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))


class QuranSurah(Base):
    __tablename__ = "quran_surahs"
    number: Mapped[int] = mapped_column(Integer, primary_key=True)
    name_ar: Mapped[str] = mapped_column(String(80), nullable=False)
    name_en: Mapped[str] = mapped_column(String(80), nullable=False)
    ayah_count: Mapped[int] = mapped_column(Integer, nullable=False)
    revelation_type: Mapped[str | None] = mapped_column(String(20))


class QuranAyah(Base):
    __tablename__ = "quran_ayahs"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    surah_number: Mapped[int] = mapped_column(ForeignKey("quran_surahs.number", ondelete="CASCADE"), nullable=False)
    ayah_number: Mapped[int] = mapped_column(Integer, nullable=False)
    text_uthmani: Mapped[str] = mapped_column(Text, nullable=False)
    juz_number: Mapped[int | None] = mapped_column(Integer)
    hizb_number: Mapped[int | None] = mapped_column(Integer)
    page_number: Mapped[int | None] = mapped_column(Integer)
    sajdah: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    __table_args__ = (UniqueConstraint("surah_number", "ayah_number", name="uq_quran_ayah"), Index("ix_quran_text_search", "surah_number", "ayah_number"))


class AppSetting(Base):
    __tablename__ = "app_settings"
    key: Mapped[str] = mapped_column(String(120), primary_key=True)
    value: Mapped[dict] = mapped_column(JSON, nullable=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, onupdate=utcnow, nullable=False)

# Optional account and preference models; anonymous device-only use remains first-class.
class AppUser(Base):
    __tablename__ = "users"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    email: Mapped[str | None] = mapped_column(String(254), unique=True)
    password_hash: Mapped[str | None] = mapped_column(String(255))
    display_name: Mapped[str | None] = mapped_column(String(100))
    is_active: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)
    deleted_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))


class UserPreference(Base):
    __tablename__ = "user_preferences"
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), primary_key=True)
    governorate_id: Mapped[str | None] = mapped_column(ForeignKey("governorates.id", ondelete="SET NULL"))
    theme_mode: Mapped[str] = mapped_column(String(12), nullable=False, default="system")
    prayer_method: Mapped[str] = mapped_column(String(40), nullable=False, default="jafari_qom")
    preferences: Mapped[dict] = mapped_column(JSON, nullable=False, default=dict)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, onupdate=utcnow, nullable=False)


class PrayerSetting(Base):
    __tablename__ = "prayer_settings"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int | None] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), unique=True)
    device_key_hash: Mapped[str | None] = mapped_column(String(64), unique=True)
    method_id: Mapped[str] = mapped_column(String(40), nullable=False, default="jafari_qom")
    manual_adjustments: Mapped[dict] = mapped_column(JSON, nullable=False, default=dict)
    midnight_definition: Mapped[str] = mapped_column(String(40), nullable=False, default="sunset_to_next_fajr")
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, onupdate=utcnow, nullable=False)


class PushDevice(Base):
    __tablename__ = "push_devices"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int | None] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"))
    token_hash: Mapped[str] = mapped_column(String(64), unique=True, nullable=False)
    platform: Mapped[str] = mapped_column(String(12), nullable=False)
    is_enabled: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)
    last_seen_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)


class BookCategory(Base):
    __tablename__ = "book_categories"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    slug: Mapped[str] = mapped_column(String(80), unique=True, nullable=False)
    name_ar: Mapped[str] = mapped_column(String(120), nullable=False)
    sort_order: Mapped[int] = mapped_column(Integer, nullable=False, default=0)


class Book(Base):
    __tablename__ = "books"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    category_id: Mapped[int | None] = mapped_column(ForeignKey("book_categories.id", ondelete="SET NULL"))
    title: Mapped[str] = mapped_column(String(250), nullable=False)
    author: Mapped[str | None] = mapped_column(String(180))
    editor: Mapped[str | None] = mapped_column(String(180))
    publisher: Mapped[str | None] = mapped_column(String(180))
    volume_count: Mapped[int | None] = mapped_column(Integer)
    description: Mapped[str] = mapped_column(Text, nullable=False, default="")
    source_url: Mapped[str | None] = mapped_column(String(2048))
    rights_status: Mapped[str] = mapped_column(String(24), nullable=False, default="unverified")
    rights_evidence: Mapped[str | None] = mapped_column(Text)
    offline_access_allowed: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    publication_status: Mapped[str] = mapped_column(String(20), nullable=False, default="draft")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)
    __table_args__ = (Index("ix_books_category_status", "category_id", "publication_status"),)


class BookPage(Base):
    __tablename__ = "book_pages"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    book_id: Mapped[int] = mapped_column(ForeignKey("books.id", ondelete="CASCADE"), nullable=False)
    volume_number: Mapped[int] = mapped_column(Integer, nullable=False, default=1)
    page_number: Mapped[int] = mapped_column(Integer, nullable=False)
    text_content: Mapped[str] = mapped_column(Text, nullable=False)
    __table_args__ = (UniqueConstraint("book_id", "volume_number", "page_number", name="uq_book_page"),)


class HadithSource(Base):
    __tablename__ = "hadith_sources"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    title: Mapped[str] = mapped_column(String(250), nullable=False)
    author: Mapped[str | None] = mapped_column(String(180))
    edition: Mapped[str | None] = mapped_column(String(120))
    source_url: Mapped[str | None] = mapped_column(String(2048))
    citation_policy: Mapped[str] = mapped_column(Text, nullable=False, default="")


class Hadith(Base):
    __tablename__ = "hadith"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    source_id: Mapped[int] = mapped_column(ForeignKey("hadith_sources.id", ondelete="RESTRICT"), nullable=False)
    speaker: Mapped[str] = mapped_column(String(150), nullable=False)
    text: Mapped[str] = mapped_column(Text, nullable=False)
    chain_text: Mapped[str | None] = mapped_column(Text)
    volume: Mapped[str | None] = mapped_column(String(40))
    page: Mapped[str | None] = mapped_column(String(40))
    hadith_number: Mapped[str | None] = mapped_column(String(60))
    topic: Mapped[str | None] = mapped_column(String(100))
    publication_status: Mapped[str] = mapped_column(String(20), nullable=False, default="draft")
    __table_args__ = (Index("ix_hadith_source_number", "source_id", "hadith_number"),)


class HadithGrading(Base):
    __tablename__ = "hadith_gradings"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    hadith_id: Mapped[int] = mapped_column(ForeignKey("hadith.id", ondelete="CASCADE"), nullable=False)
    grading: Mapped[str] = mapped_column(String(80), nullable=False)
    scholar: Mapped[str] = mapped_column(String(180), nullable=False)
    methodology: Mapped[str] = mapped_column(String(180), nullable=False)
    citation: Mapped[str] = mapped_column(Text, nullable=False)
    reviewed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))


class DuaCategory(Base):
    __tablename__ = "dua_categories"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    slug: Mapped[str] = mapped_column(String(80), unique=True, nullable=False)
    name_ar: Mapped[str] = mapped_column(String(120), nullable=False)
    sort_order: Mapped[int] = mapped_column(Integer, nullable=False, default=0)


class Dua(Base):
    __tablename__ = "duas"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    category_id: Mapped[int | None] = mapped_column(ForeignKey("dua_categories.id", ondelete="SET NULL"))
    title: Mapped[str] = mapped_column(String(180), nullable=False)
    text: Mapped[str] = mapped_column(Text, nullable=False)
    attribution: Mapped[str | None] = mapped_column(String(180))
    source_id: Mapped[int | None] = mapped_column(ForeignKey("hadith_sources.id", ondelete="SET NULL"))
    source_reference: Mapped[str | None] = mapped_column(String(250))
    publication_status: Mapped[str] = mapped_column(String(20), nullable=False, default="draft")


class Visit(Base):
    __tablename__ = "visits"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    title: Mapped[str] = mapped_column(String(180), nullable=False)
    text: Mapped[str] = mapped_column(Text, nullable=False)
    visited_person: Mapped[str | None] = mapped_column(String(180))
    source_reference: Mapped[str] = mapped_column(Text, nullable=False)
    publication_status: Mapped[str] = mapped_column(String(20), nullable=False, default="draft")


class Reciter(Base):
    __tablename__ = "reciters"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    name: Mapped[str] = mapped_column(String(180), nullable=False)
    biography: Mapped[str] = mapped_column(Text, nullable=False, default="")
    riwaya: Mapped[str | None] = mapped_column(String(100))
    source_url: Mapped[str | None] = mapped_column(String(2048))
    rights_status: Mapped[str] = mapped_column(String(24), nullable=False, default="unverified")
    is_published: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)


class AudioFile(Base):
    __tablename__ = "audio_files"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    title: Mapped[str] = mapped_column(String(250), nullable=False)
    reciter_id: Mapped[int | None] = mapped_column(ForeignKey("reciters.id", ondelete="SET NULL"))
    category: Mapped[str] = mapped_column(String(60), nullable=False)
    duration_seconds: Mapped[int | None] = mapped_column(Integer)
    mime_type: Mapped[str] = mapped_column(String(80), nullable=False)
    source_url: Mapped[str] = mapped_column(String(2048), nullable=False)
    download_url: Mapped[str | None] = mapped_column(String(2048))
    rights_status: Mapped[str] = mapped_column(String(24), nullable=False, default="unverified")
    rights_evidence: Mapped[str | None] = mapped_column(Text)
    streaming_allowed: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False, server_default=false())
    download_allowed: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False, server_default=false())
    offline_cache_allowed: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False, server_default=false())
    is_published: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    __table_args__ = (Index("ix_audio_category_published", "category", "is_published"),)


class QuranBookmark(Base):
    __tablename__ = "quran_bookmarks"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int | None] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"))
    device_key_hash: Mapped[str | None] = mapped_column(String(64))
    surah_number: Mapped[int] = mapped_column(ForeignKey("quran_surahs.number", ondelete="CASCADE"), nullable=False)
    ayah_number: Mapped[int] = mapped_column(Integer, nullable=False)
    label: Mapped[str | None] = mapped_column(String(100))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)


class QuranRecitation(Base):
    __tablename__ = "quran_recitations"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    reciter_id: Mapped[int] = mapped_column(ForeignKey("reciters.id", ondelete="CASCADE"), nullable=False)
    surah_number: Mapped[int] = mapped_column(ForeignKey("quran_surahs.number", ondelete="CASCADE"), nullable=False)
    audio_file_id: Mapped[int] = mapped_column(ForeignKey("audio_files.id", ondelete="RESTRICT"), nullable=False)
    riwaya: Mapped[str] = mapped_column(String(80), nullable=False, default="حفص عن عاصم")
    is_published: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)


class RadioProgram(Base):
    __tablename__ = "radio_programs"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    channel_id: Mapped[int] = mapped_column(ForeignKey("radio_channels.id", ondelete="CASCADE"), nullable=False)
    title: Mapped[str] = mapped_column(String(180), nullable=False)
    presenter: Mapped[str | None] = mapped_column(String(160))
    starts_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    ends_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    source_url: Mapped[str | None] = mapped_column(String(2048))


class IslamicEvent(Base):
    __tablename__ = "islamic_events"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    title: Mapped[str] = mapped_column(String(180), nullable=False)
    hijri_month: Mapped[int | None] = mapped_column(Integer)
    hijri_day: Mapped[int | None] = mapped_column(Integer)
    expected_gregorian_date: Mapped[date | None] = mapped_column(Date)
    date_certainty: Mapped[str] = mapped_column(String(30), nullable=False, default="disputed_or_local")
    event_type: Mapped[str] = mapped_column(String(30), nullable=False)
    description: Mapped[str] = mapped_column(Text, nullable=False, default="")
    historical_sources: Mapped[list] = mapped_column(JSON, nullable=False, default=list)
    notification_enabled: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    is_published: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)


class NotificationPreference(Base):
    __tablename__ = "notification_preferences"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int | None] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"))
    device_key_hash: Mapped[str | None] = mapped_column(String(64))
    category: Mapped[str] = mapped_column(String(40), nullable=False)
    enabled: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    lead_minutes: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    quiet_hours: Mapped[dict] = mapped_column(JSON, nullable=False, default=dict)
    __table_args__ = (UniqueConstraint("user_id", "device_key_hash", "category", name="uq_notification_pref_subject"),)


class Notification(Base):
    __tablename__ = "notifications"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    title: Mapped[str] = mapped_column(String(180), nullable=False)
    body: Mapped[str] = mapped_column(Text, nullable=False)
    category: Mapped[str] = mapped_column(String(40), nullable=False)
    scheduled_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    status: Mapped[str] = mapped_column(String(20), nullable=False, default="draft")
    payload: Mapped[dict] = mapped_column(JSON, nullable=False, default=dict)
    created_by: Mapped[int] = mapped_column(ForeignKey("admin_users.id", ondelete="RESTRICT"), nullable=False)


class EthicsLesson(Base):
    __tablename__ = "ethics_lessons"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    slug: Mapped[str] = mapped_column(String(100), unique=True, nullable=False)
    title: Mapped[str] = mapped_column(String(180), nullable=False)
    level: Mapped[str] = mapped_column(String(30), nullable=False, default="beginner")
    order_index: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    introduction: Mapped[str] = mapped_column(Text, nullable=False, default="")
    body_markdown: Mapped[str] = mapped_column(Text, nullable=False)
    quran_reference: Mapped[str | None] = mapped_column(String(120))
    hadith_id: Mapped[int | None] = mapped_column(ForeignKey("hadith.id", ondelete="SET NULL"))
    practical_action: Mapped[str] = mapped_column(Text, nullable=False, default="")
    publication_status: Mapped[str] = mapped_column(String(20), nullable=False, default="draft")


class EthicsQuiz(Base):
    __tablename__ = "ethics_quizzes"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    lesson_id: Mapped[int] = mapped_column(ForeignKey("ethics_lessons.id", ondelete="CASCADE"), nullable=False)
    title: Mapped[str] = mapped_column(String(180), nullable=False)


class EthicsQuestion(Base):
    __tablename__ = "ethics_questions"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    quiz_id: Mapped[int] = mapped_column(ForeignKey("ethics_quizzes.id", ondelete="CASCADE"), nullable=False)
    prompt: Mapped[str] = mapped_column(Text, nullable=False)
    options: Mapped[list] = mapped_column(JSON, nullable=False)
    correct_option_index: Mapped[int] = mapped_column(Integer, nullable=False)
    explanation: Mapped[str] = mapped_column(Text, nullable=False, default="")


class EthicsProgress(Base):
    __tablename__ = "ethics_progress"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int | None] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"))
    device_key_hash: Mapped[str | None] = mapped_column(String(64))
    lesson_id: Mapped[int] = mapped_column(ForeignKey("ethics_lessons.id", ondelete="CASCADE"), nullable=False)
    completion_percent: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    best_score: Mapped[int | None] = mapped_column(Integer)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, onupdate=utcnow, nullable=False)


class TasbihSession(Base):
    __tablename__ = "tasbih_sessions"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int | None] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"))
    device_key_hash: Mapped[str | None] = mapped_column(String(64))
    phrase: Mapped[str] = mapped_column(String(180), nullable=False)
    count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    target: Mapped[int] = mapped_column(Integer, nullable=False, default=100)
    session_date: Mapped[date] = mapped_column(Date, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)


class Favorite(Base):
    __tablename__ = "favorites"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int | None] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"))
    device_key_hash: Mapped[str | None] = mapped_column(String(64))
    entity_type: Mapped[str] = mapped_column(String(40), nullable=False)
    entity_id: Mapped[str] = mapped_column(String(100), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)
    __table_args__ = (
        UniqueConstraint("user_id", "device_key_hash", "entity_type", "entity_id", name="uq_favorite_subject_entity"),
        Index(
            "uq_device_favorite_entity", "device_key_hash", "entity_type", "entity_id",
            unique=True,
            sqlite_where=text("user_id IS NULL AND device_key_hash IS NOT NULL"),
            postgresql_where=text("user_id IS NULL AND device_key_hash IS NOT NULL"),
        ),
    )


class BookReadingProgress(Base):
    __tablename__ = "book_reading_progress"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    device_key_hash: Mapped[str] = mapped_column(String(64), nullable=False)
    book_id: Mapped[int] = mapped_column(ForeignKey("books.id", ondelete="CASCADE"), nullable=False)
    volume_number: Mapped[int] = mapped_column(Integer, nullable=False)
    page_number: Mapped[int] = mapped_column(Integer, nullable=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, onupdate=utcnow, nullable=False)
    __table_args__ = (
        UniqueConstraint("device_key_hash", "book_id", "volume_number", name="uq_device_book_volume_progress"),
        CheckConstraint("volume_number >= 1", name="ck_book_progress_volume_positive"),
        CheckConstraint("page_number >= 1", name="ck_book_progress_page_positive"),
        Index("ix_book_progress_device_updated", "device_key_hash", "updated_at"),
    )
