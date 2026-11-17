"""Small independent scheduler worker for scheduled CMS content.

Run as a separate process/container (not inside a mobile or web request loop).
For high-volume use, replace the polling loop with a managed queue scheduler.
"""
import logging
import time
from datetime import datetime, timezone
from sqlalchemy import and_, or_
from app.db.session import SessionLocal
from app.models import ContentItem

logging.basicConfig(level=logging.INFO, format='%(asctime)s %(levelname)s %(message)s')


def publish_due_items() -> tuple[int, int]:
    now = datetime.now(timezone.utc)
    db = SessionLocal()
    try:
        due = db.query(ContentItem).filter(
            ContentItem.status == "scheduled",
            or_(ContentItem.starts_at.is_(None), ContentItem.starts_at <= now),
            or_(ContentItem.ends_at.is_(None), ContentItem.ends_at > now),
        ).with_for_update(skip_locked=True).all()
        expired = db.query(ContentItem).filter(
            ContentItem.status == "published",
            ContentItem.ends_at.is_not(None),
            ContentItem.ends_at <= now,
        ).with_for_update(skip_locked=True).all()
        for row in due:
            row.status = "published"
        for row in expired:
            row.status = "archived"
        db.commit()
        return len(due), len(expired)
    except Exception:
        db.rollback()
        logging.exception("Publication scheduler iteration failed")
        raise
    finally:
        db.close()


if __name__ == "__main__":
    while True:
        try:
            published, archived = publish_due_items()
            if published or archived:
                logging.info("Scheduled content published=%s archived=%s", published, archived)
        except Exception:
            logging.exception("Publication scheduler loop failed; retrying after the polling interval")
        time.sleep(30)
