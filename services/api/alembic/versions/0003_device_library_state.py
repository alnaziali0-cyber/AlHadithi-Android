"""Add anonymous-device favorites uniqueness and book reading progress.

Revision ID: 0003_device_library_state
Revises: 0002_content_rights_permissions
"""
from alembic import op
import sqlalchemy as sa

revision = "0003_device_library_state"
down_revision = "0002_content_rights_permissions"
branch_labels = None
depends_on = None


def upgrade() -> None:
    # PostgreSQL treats NULL values as distinct in ordinary unique constraints.
    # This partial index makes device-only favorite writes idempotent under races.
    op.execute(sa.text("""
        DELETE FROM favorites
        WHERE user_id IS NULL
          AND device_key_hash IS NOT NULL
          AND id NOT IN (
              SELECT MIN(id)
              FROM favorites
              WHERE user_id IS NULL AND device_key_hash IS NOT NULL
              GROUP BY device_key_hash, entity_type, entity_id
          )
    """))
    op.create_index(
        "uq_device_favorite_entity",
        "favorites",
        ["device_key_hash", "entity_type", "entity_id"],
        unique=True,
        sqlite_where=sa.text("user_id IS NULL AND device_key_hash IS NOT NULL"),
        postgresql_where=sa.text("user_id IS NULL AND device_key_hash IS NOT NULL"),
    )
    op.create_table(
        "book_reading_progress",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("device_key_hash", sa.String(length=64), nullable=False),
        sa.Column("book_id", sa.Integer(), sa.ForeignKey("books.id", ondelete="CASCADE"), nullable=False),
        sa.Column("volume_number", sa.Integer(), nullable=False),
        sa.Column("page_number", sa.Integer(), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
        sa.UniqueConstraint("device_key_hash", "book_id", "volume_number", name="uq_device_book_volume_progress"),
        sa.CheckConstraint("volume_number >= 1", name="ck_book_progress_volume_positive"),
        sa.CheckConstraint("page_number >= 1", name="ck_book_progress_page_positive"),
    )
    op.create_index("ix_book_progress_device_updated", "book_reading_progress", ["device_key_hash", "updated_at"])


def downgrade() -> None:
    op.drop_index("ix_book_progress_device_updated", table_name="book_reading_progress")
    op.drop_table("book_reading_progress")
    op.drop_index("uq_device_favorite_entity", table_name="favorites")
