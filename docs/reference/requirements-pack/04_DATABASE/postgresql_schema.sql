-- KHAZAIN AL HUDA: initial PostgreSQL schema
-- Review migrations, indexes, naming and security with the project team before production.

CREATE TABLE IF NOT EXISTS organizations (
  id UUID PRIMARY KEY,
  name TEXT NOT NULL,
  official_url TEXT,
  contact_email TEXT,
  notes TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS creators (
  id UUID PRIMARY KEY,
  display_name TEXT NOT NULL,
  role TEXT NOT NULL CHECK (role IN ('muezzin','qari','khatib','radoud','reciter','other')),
  biography TEXT,
  organization_id UUID REFERENCES organizations(id),
  portrait_storage_key TEXT,
  portrait_license_id UUID,
  official_profile_url TEXT,
  verification_status TEXT NOT NULL DEFAULT 'pending' CHECK (verification_status IN ('pending','verified','rejected')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS media_licenses (
  id UUID PRIMARY KEY,
  rights_holder TEXT NOT NULL,
  licensor_contact TEXT,
  license_type TEXT NOT NULL CHECK (license_type IN ('written_permission','cc_license','public_domain','contract','other')),
  document_storage_key TEXT,
  evidence_url TEXT,
  valid_from DATE,
  valid_until DATE,
  broadcast_allowed BOOLEAN NOT NULL DEFAULT FALSE,
  download_allowed BOOLEAN NOT NULL DEFAULT FALSE,
  offline_allowed BOOLEAN NOT NULL DEFAULT FALSE,
  redistribution_allowed BOOLEAN NOT NULL DEFAULT FALSE,
  recording_allowed BOOLEAN NOT NULL DEFAULT FALSE,
  commercial_allowed BOOLEAN NOT NULL DEFAULT FALSE,
  modification_allowed BOOLEAN NOT NULL DEFAULT FALSE,
  attribution_required BOOLEAN NOT NULL DEFAULT TRUE,
  attribution_text TEXT,
  territories JSONB NOT NULL DEFAULT '["IQ"]'::jsonb,
  platforms JSONB NOT NULL DEFAULT '["Android","iOS"]'::jsonb,
  status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','approved','restricted','expired')),
  reviewed_by TEXT,
  reviewed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE creators DROP CONSTRAINT IF EXISTS creators_portrait_license_id_fkey;
ALTER TABLE creators ADD CONSTRAINT creators_portrait_license_id_fkey
  FOREIGN KEY (portrait_license_id) REFERENCES media_licenses(id);

CREATE TABLE IF NOT EXISTS content_assets (
  id UUID PRIMARY KEY,
  title TEXT NOT NULL,
  content_type TEXT NOT NULL CHECK (content_type IN ('adhan','fajr_adhan','quran','dua','ziyara','hussaini_majlis','latmiya','lecture','logo','station_image','other')),
  description TEXT,
  language_code TEXT NOT NULL DEFAULT 'ar',
  creator_id UUID REFERENCES creators(id),
  organization_id UUID REFERENCES organizations(id),
  source_url TEXT,
  original_filename TEXT,
  storage_key TEXT,
  mime_type TEXT,
  duration_seconds INTEGER CHECK (duration_seconds IS NULL OR duration_seconds >= 0),
  file_size_bytes BIGINT CHECK (file_size_bytes IS NULL OR file_size_bytes >= 0),
  checksum_sha256 CHAR(64),
  license_id UUID REFERENCES media_licenses(id),
  rights_status TEXT NOT NULL DEFAULT 'pending' CHECK (rights_status IN ('pending','approved','restricted','expired')),
  review_status TEXT NOT NULL DEFAULT 'draft' CHECK (review_status IN ('draft','pending_review','approved','published','rejected','archived')),
  metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_by TEXT,
  published_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT content_published_requires_license CHECK (
    review_status <> 'published' OR (rights_status = 'approved' AND license_id IS NOT NULL)
  )
);
CREATE INDEX IF NOT EXISTS idx_content_assets_type_status ON content_assets(content_type, review_status);
CREATE INDEX IF NOT EXISTS idx_content_assets_creator ON content_assets(creator_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_content_assets_checksum ON content_assets(checksum_sha256) WHERE checksum_sha256 IS NOT NULL;

CREATE TABLE IF NOT EXISTS radio_stations (
  id UUID PRIMARY KEY,
  name TEXT NOT NULL,
  description TEXT,
  owner_organization_id UUID REFERENCES organizations(id),
  official_website TEXT,
  logo_asset_id UUID REFERENCES content_assets(id),
  image_asset_id UUID REFERENCES content_assets(id),
  official_stream_url TEXT,
  stream_type TEXT CHECK (stream_type IN ('HLS','AAC','MP3','OTHER')),
  stream_region TEXT,
  broadcast_license_id UUID REFERENCES media_licenses(id),
  background_playback_allowed BOOLEAN NOT NULL DEFAULT FALSE,
  recording_allowed BOOLEAN NOT NULL DEFAULT FALSE,
  redistribution_allowed BOOLEAN NOT NULL DEFAULT FALSE,
  program_schedule_url TEXT,
  stream_status TEXT NOT NULL DEFAULT 'unverified' CHECK (stream_status IN ('unverified','online','offline','restricted')),
  source_verified_at TIMESTAMPTZ,
  review_status TEXT NOT NULL DEFAULT 'draft' CHECK (review_status IN ('draft','pending_review','approved','published','archived')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS radio_frequencies (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  station_id UUID NOT NULL REFERENCES radio_stations(id) ON DELETE CASCADE,
  city_name TEXT NOT NULL,
  frequency_mhz NUMERIC(6,2) NOT NULL,
  source_url TEXT,
  verified_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS iraq_cities (
  city_id TEXT PRIMARY KEY,
  country_code CHAR(2) NOT NULL DEFAULT 'IQ',
  governorate TEXT NOT NULL,
  city_name_ar TEXT NOT NULL,
  city_name_en TEXT NOT NULL,
  latitude NUMERIC(9,6) NOT NULL CHECK (latitude BETWEEN -90 AND 90),
  longitude NUMERIC(9,6) NOT NULL CHECK (longitude BETWEEN -180 AND 180),
  timezone TEXT NOT NULL DEFAULT 'Asia/Baghdad',
  coordinate_source TEXT,
  coordinate_accuracy TEXT NOT NULL DEFAULT 'approximate_city_center',
  verified BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS prayer_profiles (
  id UUID PRIMARY KEY,
  profile_name TEXT NOT NULL,
  authority_or_calendar TEXT NOT NULL,
  jurisprudential_basis TEXT NOT NULL DEFAULT 'jaafari',
  source_url TEXT NOT NULL,
  source_document_storage_key TEXT,
  fajr_angle_degrees NUMERIC(5,2),
  isha_angle_degrees NUMERIC(5,2),
  maghrib_rule TEXT NOT NULL,
  maghrib_angle_degrees NUMERIC(5,2),
  asr_rule TEXT,
  midnight_rule TEXT,
  calculation_engine TEXT NOT NULL,
  engine_version TEXT NOT NULL,
  minute_adjustments JSONB NOT NULL DEFAULT '{"fajr":0,"sunrise":0,"dhuhr":0,"sunset":0,"maghrib":0,"isha":0}'::jsonb,
  is_officially_verified BOOLEAN NOT NULL DEFAULT FALSE,
  verification_document TEXT,
  active BOOLEAN NOT NULL DEFAULT FALSE,
  notes TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS prayer_time_cache (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  city_id TEXT NOT NULL REFERENCES iraq_cities(city_id),
  profile_id UUID NOT NULL REFERENCES prayer_profiles(id),
  prayer_date DATE NOT NULL,
  fajr TIMESTAMPTZ,
  sunrise TIMESTAMPTZ,
  dhuhr TIMESTAMPTZ,
  sunset TIMESTAMPTZ,
  maghrib TIMESTAMPTZ,
  isha TIMESTAMPTZ,
  islamic_midnight TIMESTAMPTZ,
  astronomical_midnight TIMESTAMPTZ,
  engine_version TEXT NOT NULL,
  computed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(city_id, profile_id, prayer_date)
);

CREATE TABLE IF NOT EXISTS audit_logs (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  actor_id TEXT NOT NULL,
  action TEXT NOT NULL,
  entity_type TEXT NOT NULL,
  entity_id TEXT NOT NULL,
  before_data JSONB,
  after_data JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
