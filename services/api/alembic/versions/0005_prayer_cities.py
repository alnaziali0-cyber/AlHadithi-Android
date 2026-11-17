"""Add Iraqi prayer-city catalogue with provenance and verification state.

Revision ID: 0005_prayer_cities
Revises: 0004_book_offline_access
"""
from alembic import op
import sqlalchemy as sa

revision = "0005_prayer_cities"
down_revision = "0004_book_offline_access"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "prayer_cities",
        sa.Column("city_id", sa.String(length=32), primary_key=True),
        sa.Column("governorate_id", sa.String(length=32), nullable=False),
        sa.Column("governorate_name_ar", sa.String(length=100), nullable=False),
        sa.Column("city_name_ar", sa.String(length=100), nullable=False),
        sa.Column("city_name_en", sa.String(length=100), nullable=False),
        sa.Column("latitude", sa.Float(), nullable=False),
        sa.Column("longitude", sa.Float(), nullable=False),
        sa.Column("timezone", sa.String(length=64), nullable=False, server_default="Asia/Baghdad"),
        sa.Column("coordinate_source", sa.String(length=250), nullable=False),
        sa.Column("coordinate_accuracy", sa.String(length=32), nullable=False, server_default="approximate_city_center"),
        sa.Column("verified", sa.Boolean(), nullable=False, server_default=sa.false()),
        sa.ForeignKeyConstraint(["governorate_id"], ["governorates.id"], ondelete="RESTRICT"),
        sa.CheckConstraint("latitude >= -90 AND latitude <= 90", name="ck_prayer_city_latitude"),
        sa.CheckConstraint("longitude >= -180 AND longitude <= 180", name="ck_prayer_city_longitude"),
    )
    op.create_index("ix_prayer_cities_governorate", "prayer_cities", ["governorate_id"])


def downgrade() -> None:
    op.drop_index("ix_prayer_cities_governorate", table_name="prayer_cities")
    op.drop_table("prayer_cities")
