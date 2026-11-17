"""Read-only, publication-gated catalog queries for the mobile application.

The catalog never seeds religious material. It exposes only records that have
passed the publication and provenance gates represented by the current schema.
"""

from __future__ import annotations

from typing import Any
from urllib.parse import urlparse

from fastapi import HTTPException
from sqlalchemy import func, or_
from sqlalchemy.orm import Session

from app.models import (
    AudioFile,
    Book,
    BookPage,
    EthicsLesson,
    Hadith,
    HadithGrading,
    HadithSource,
    IslamicEvent,
    Dua,
    Reciter,
    Visit,
)


REUSABLE_RIGHTS_STATUSES = frozenset({"licensed", "public_domain", "permission_granted"})
CATALOG_TYPES = frozenset({"book", "hadith", "dua", "visit", "event", "ethics_lesson", "audio"})


def _safe_https_url(value: str | None) -> str | None:
    if not value:
        return None
    parsed = urlparse(value)
    if parsed.scheme != "https" or not parsed.netloc or parsed.username or parsed.password:
        return None
    return value


def _search_pattern(value: str | None) -> str | None:
    if value is None:
        return None
    term = value.strip()
    if not term:
        return None
    escaped = term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
    return f"%{escaped}%"


def _page_result(items: list[dict[str, Any]], total: int, limit: int, offset: int) -> dict[str, Any]:
    return {"items": items, "total": total, "limit": limit, "offset": offset}


def list_catalog(
    db: Session,
    catalog_type: str,
    *,
    search: str | None = None,
    limit: int = 30,
    offset: int = 0,
) -> dict[str, Any]:
    """Return public catalog rows after explicit publication/provenance checks."""
    if catalog_type not in CATALOG_TYPES:
        raise HTTPException(status_code=404, detail="Catalog section not found")
    pattern = _search_pattern(search)

    if catalog_type == "book":
        query = db.query(Book).filter(
            Book.publication_status == "published",
            Book.rights_status.in_(REUSABLE_RIGHTS_STATUSES),
            Book.rights_evidence.is_not(None),
            Book.rights_evidence != "",
            Book.source_url.is_not(None),
            Book.source_url.like("https://%"),
        )
        if pattern:
            query = query.filter(or_(Book.title.ilike(pattern, escape="\\"), Book.author.ilike(pattern, escape="\\")))
        total = query.count()
        rows = query.order_by(Book.title.asc(), Book.id.asc()).offset(offset).limit(limit).all()
        items = [{
            "id": row.id,
            "title": row.title,
            "body_markdown": row.description,
            "author": row.author,
            "editor": row.editor,
            "publisher": row.publisher,
            "volume_count": row.volume_count,
            "rights_status": row.rights_status,
            "offline_access_allowed": bool(row.offline_access_allowed),
            "source_url": _safe_https_url(row.source_url),
            "metadata": _join_metadata((row.author, row.editor, row.publisher, f"عدد المجلدات: {row.volume_count}" if row.volume_count else None)),
        } for row in rows]
        return _page_result(items, total, limit, offset)

    if catalog_type == "hadith":
        query = db.query(Hadith, HadithSource).join(HadithSource, Hadith.source_id == HadithSource.id).filter(
            Hadith.publication_status == "published",
            HadithSource.edition.is_not(None),
            HadithSource.edition != "",
            or_(
                (Hadith.hadith_number.is_not(None) & (Hadith.hadith_number != "")),
                (Hadith.volume.is_not(None) & (Hadith.volume != "") & Hadith.page.is_not(None) & (Hadith.page != "")),
            ),
        )
        if pattern:
            query = query.filter(or_(
                Hadith.text.ilike(pattern, escape="\\"),
                Hadith.speaker.ilike(pattern, escape="\\"),
                Hadith.topic.ilike(pattern, escape="\\"),
                Hadith.hadith_number.ilike(pattern, escape="\\"),
                HadithSource.title.ilike(pattern, escape="\\"),
            ))
        total = query.count()
        rows = query.order_by(Hadith.id.desc()).offset(offset).limit(limit).all()
        ids = [hadith.id for hadith, _source in rows]
        grading_rows = db.query(HadithGrading).filter(HadithGrading.hadith_id.in_(ids)).order_by(HadithGrading.id.asc()).all() if ids else []
        gradings_by_hadith: dict[int, list[dict[str, Any]]] = {}
        for grading in grading_rows:
            gradings_by_hadith.setdefault(grading.hadith_id, []).append({
                "grading": grading.grading,
                "scholar": grading.scholar,
                "methodology": grading.methodology,
                "citation": grading.citation,
                "reviewed_at": grading.reviewed_at.isoformat() if grading.reviewed_at else None,
            })
        items = []
        for hadith, source in rows:
            citation = _join_metadata((source.title, source.author, source.edition, f"المجلد: {hadith.volume}" if hadith.volume else None, f"الصفحة: {hadith.page}" if hadith.page else None, f"رقم الرواية: {hadith.hadith_number}" if hadith.hadith_number else None))
            items.append({
                "id": hadith.id,
                "title": hadith.topic or hadith.speaker,
                "body_markdown": hadith.text,
                "speaker": hadith.speaker,
                "chain_text": hadith.chain_text,
                "source": {"title": source.title, "author": source.author, "edition": source.edition, "source_url": _safe_https_url(source.source_url), "citation_policy": source.citation_policy},
                "source_reference": citation,
                "gradings": gradings_by_hadith.get(hadith.id, []),
                "metadata": citation,
            })
        return _page_result(items, total, limit, offset)

    if catalog_type == "dua":
        query = db.query(Dua).filter(
            Dua.publication_status == "published",
            Dua.source_reference.is_not(None),
            Dua.source_reference != "",
        )
        if pattern:
            query = query.filter(or_(Dua.title.ilike(pattern, escape="\\"), Dua.text.ilike(pattern, escape="\\"), Dua.attribution.ilike(pattern, escape="\\")))
        total = query.count()
        rows = query.order_by(Dua.title.asc(), Dua.id.asc()).offset(offset).limit(limit).all()
        source_ids = {row.source_id for row in rows if row.source_id is not None}
        sources = {source.id: source for source in db.query(HadithSource).filter(HadithSource.id.in_(source_ids)).all()} if source_ids else {}
        items = []
        for row in rows:
            source = sources.get(row.source_id)
            items.append({
                "id": row.id, "title": row.title, "body_markdown": row.text,
                "attribution": row.attribution, "source_reference": row.source_reference,
                "source": ({"title": source.title, "author": source.author, "edition": source.edition, "source_url": _safe_https_url(source.source_url)} if source else None),
                "metadata": _join_metadata((row.attribution, row.source_reference, source.title if source else None)),
            })
        return _page_result(items, total, limit, offset)

    if catalog_type == "visit":
        query = db.query(Visit).filter(
            Visit.publication_status == "published",
            Visit.source_reference != "",
        )
        if pattern:
            query = query.filter(or_(Visit.title.ilike(pattern, escape="\\"), Visit.text.ilike(pattern, escape="\\"), Visit.visited_person.ilike(pattern, escape="\\")))
        total = query.count()
        rows = query.order_by(Visit.title.asc(), Visit.id.asc()).offset(offset).limit(limit).all()
        items = [{
            "id": row.id, "title": row.title, "body_markdown": row.text,
            "attribution": row.visited_person, "source_reference": row.source_reference,
            "metadata": _join_metadata((row.visited_person, row.source_reference)),
        } for row in rows]
        return _page_result(items, total, limit, offset)

    if catalog_type == "event":
        query = db.query(IslamicEvent).filter(
            IslamicEvent.is_published.is_(True),
            IslamicEvent.historical_sources.is_not(None),
            func.json_array_length(IslamicEvent.historical_sources) > 0,
        )
        if pattern:
            query = query.filter(or_(IslamicEvent.title.ilike(pattern, escape="\\"), IslamicEvent.description.ilike(pattern, escape="\\")))
        rows = query.order_by(IslamicEvent.expected_gregorian_date.asc(), IslamicEvent.id.asc()).offset(offset).limit(limit).all()
        # Only list events that retain at least one recorded historical source.
        rows = [row for row in rows if isinstance(row.historical_sources, list) and len(row.historical_sources) > 0]
        total = len(rows) if offset == 0 and len(rows) < limit else query.count()
        items = [{
            "id": row.id, "title": row.title, "body_markdown": row.description,
            "event_type": row.event_type,
            "hijri_month": row.hijri_month, "hijri_day": row.hijri_day,
            "expected_gregorian_date": row.expected_gregorian_date.isoformat() if row.expected_gregorian_date else None,
            "date_certainty": row.date_certainty,
            "historical_sources": row.historical_sources,
            "metadata": _join_metadata((row.expected_gregorian_date.isoformat() if row.expected_gregorian_date else None, f"هجري: {row.hijri_day}/{row.hijri_month}" if row.hijri_day and row.hijri_month else None, f"درجة الثبوت: {row.date_certainty}")),
        } for row in rows]
        return _page_result(items, total, limit, offset)

    if catalog_type == "ethics_lesson":
        query = db.query(EthicsLesson).filter(EthicsLesson.publication_status == "published")
        if pattern:
            query = query.filter(or_(EthicsLesson.title.ilike(pattern, escape="\\"), EthicsLesson.introduction.ilike(pattern, escape="\\"), EthicsLesson.body_markdown.ilike(pattern, escape="\\")))
        total = query.count()
        rows = query.order_by(EthicsLesson.order_index.asc(), EthicsLesson.id.asc()).offset(offset).limit(limit).all()
        items = [{
            "id": row.id, "title": row.title,
            "body_markdown": _join_metadata((row.introduction, row.body_markdown, f"التطبيق العملي: {row.practical_action}" if row.practical_action else None)),
            "level": row.level, "quran_reference": row.quran_reference,
            "hadith_id": row.hadith_id,
            "metadata": _join_metadata((row.level, row.quran_reference)),
        } for row in rows]
        return _page_result(items, total, limit, offset)

    query = db.query(AudioFile).filter(
        AudioFile.is_published.is_(True),
        AudioFile.rights_status.in_(REUSABLE_RIGHTS_STATUSES),
        AudioFile.streaming_allowed.is_(True),
        AudioFile.rights_evidence.is_not(None),
        AudioFile.rights_evidence != "",
        AudioFile.source_url.like("https://%"),
    )
    if pattern:
        query = query.filter(or_(AudioFile.title.ilike(pattern, escape="\\"), AudioFile.category.ilike(pattern, escape="\\")))
    rows = query.order_by(AudioFile.title.asc(), AudioFile.id.asc()).offset(offset).limit(limit).all()
    total = query.count()
    reciter_ids = {row.reciter_id for row in rows if row.reciter_id is not None}
    reciters = {reciter.id: reciter for reciter in db.query(Reciter).filter(Reciter.id.in_(reciter_ids), Reciter.is_published.is_(True)).all()} if reciter_ids else {}
    items = [{
        "id": row.id, "title": row.title, "body_markdown": _join_metadata((row.category, reciters[row.reciter_id].name if row.reciter_id in reciters else None)),
        "category": row.category, "duration_seconds": row.duration_seconds,
        "mime_type": row.mime_type, "source_url": _safe_https_url(row.source_url),
        "download_url": _safe_https_url(row.download_url) if row.download_allowed else None,
        "can_download": bool(row.download_allowed and _safe_https_url(row.download_url)),
        "can_cache_offline": bool(
            row.offline_cache_allowed and row.download_allowed and _safe_https_url(row.download_url)
        ),
        "reciter": reciters[row.reciter_id].name if row.reciter_id in reciters else None,
        "rights_status": row.rights_status,
    } for row in rows]
    return _page_result(items, total, limit, offset)


def get_book_pages(
    db: Session,
    book_id: int,
    *,
    volume_number: int = 1,
    limit: int = 40,
    offset: int = 0,
    start_page_number: int | None = None,
) -> dict[str, Any]:
    book = db.get(Book, book_id)
    if (
        not book
        or book.publication_status != "published"
        or book.rights_status not in REUSABLE_RIGHTS_STATUSES
        or not book.rights_evidence
        or not _safe_https_url(book.source_url)
    ):
        raise HTTPException(status_code=404, detail="Book not found")
    query = db.query(BookPage).filter(BookPage.book_id == book_id, BookPage.volume_number == volume_number)
    if start_page_number is not None:
        query = query.filter(BookPage.page_number >= start_page_number)
    total = query.count()
    rows = query.order_by(BookPage.page_number.asc()).offset(offset).limit(limit).all()
    return {
        "book": {
            "id": book.id, "title": book.title, "volume_count": book.volume_count,
            "offline_access_allowed": bool(book.offline_access_allowed),
        },
        "items": [{"page_number": row.page_number, "text_content": row.text_content} for row in rows],
        "total": total, "limit": limit, "offset": offset, "volume_number": volume_number,
    }


def _join_metadata(parts: tuple[str | None, ...]) -> str:
    return " · ".join(part.strip() for part in parts if part and part.strip())
