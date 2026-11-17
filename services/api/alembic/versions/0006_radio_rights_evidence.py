"""Require durable reuse-permission evidence for verified radio streams.

Revision ID: 0006_radio_rights_evidence
Revises: 0005_prayer_cities
"""
from alembic import op
import sqlalchemy as sa

revision = "0006_radio_rights_evidence"
down_revision = "0005_prayer_cities"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.add_column("radio_channels", sa.Column("rights_evidence", sa.Text(), nullable=True))
    op.add_column("radio_channels", sa.Column("rights_reviewed_at", sa.DateTime(timezone=True), nullable=True))
    # Existing rows must be re-reviewed before an operator can re-enable a stream.
    op.execute(sa.text("UPDATE radio_channels SET is_verified = FALSE, is_enabled = FALSE"))


def downgrade() -> None:
    op.drop_column("radio_channels", "rights_reviewed_at")
    op.drop_column("radio_channels", "rights_evidence")
