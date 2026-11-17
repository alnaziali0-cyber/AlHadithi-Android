"""Add auditable rights evidence and per-use audio permissions.

Revision ID: 0002_content_rights_permissions
Revises: 0001_initial
"""
from alembic import op
import sqlalchemy as sa

revision = "0002_content_rights_permissions"
down_revision = "0001_initial"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.add_column("books", sa.Column("rights_evidence", sa.Text(), nullable=True))
    op.add_column(
        "audio_files",
        sa.Column("streaming_allowed", sa.Boolean(), server_default=sa.false(), nullable=False),
    )
    op.add_column(
        "audio_files",
        sa.Column("download_allowed", sa.Boolean(), server_default=sa.false(), nullable=False),
    )
    op.add_column(
        "audio_files",
        sa.Column("offline_cache_allowed", sa.Boolean(), server_default=sa.false(), nullable=False),
    )


def downgrade() -> None:
    op.drop_column("audio_files", "offline_cache_allowed")
    op.drop_column("audio_files", "download_allowed")
    op.drop_column("audio_files", "streaming_allowed")
    op.drop_column("books", "rights_evidence")
