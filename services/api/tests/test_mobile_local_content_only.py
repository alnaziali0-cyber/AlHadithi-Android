import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
MOBILE = ROOT / "apps" / "mobile"


USER_FACING_CONTENT_FILES = (
    MOBILE / "lib/features/content/library_reference_panel.dart",
    MOBILE / "lib/features/content/ahl_al_bayt_content_screen.dart",
    MOBILE / "lib/features/content/content_screen.dart",
    MOBILE / "lib/features/content/quran_library_screen.dart",
    MOBILE / "lib/features/content/quran_text_reader_screen.dart",
    MOBILE / "lib/features/radio/audio_library_screen.dart",
    MOBILE / "lib/features/radio/radio_screen.dart",
    MOBILE / "lib/features/settings/source_catalog_screen.dart",
)


def test_content_and_audio_screens_have_no_external_launchers_or_urls():
    for path in USER_FACING_CONTENT_FILES:
        source = path.read_text(encoding="utf-8")
        assert "launchUrl" not in source, path
        assert "https://" not in source, path
        assert "http://" not in source, path


def test_audio_playback_is_local_only_and_no_download_code_remains():
    service = (MOBILE / "lib/core/quran_audio_service.dart").read_text(
        encoding="utf-8"
    )
    reader = (
        MOBILE / "lib/features/content/quran_text_reader_screen.dart"
    ).read_text(encoding="utf-8")
    radio = (MOBILE / "lib/features/radio/radio_screen.dart").read_text(
        encoding="utf-8"
    )

    assert "AudioSource.file" in reader
    assert "AudioSource.uri" not in reader
    assert "downloadSurah" not in reader
    assert "Dio" not in service
    assert "http" not in service.lower()
    assert "download" not in service.lower()
    assert "AudioPlayer" not in radio
    assert "stream_url" not in radio


def test_external_audio_catalog_is_not_packaged_as_a_flutter_asset():
    pubspec = (MOBILE / "pubspec.yaml").read_text(encoding="utf-8")
    assert "assets/data/catalogs/" not in pubspec


def test_api_content_and_source_text_is_sanitized_before_display():
    content_screen = (
        MOBILE / "lib/features/content/content_screen.dart"
    ).read_text(encoding="utf-8")
    source_screen = (
        MOBILE / "lib/features/settings/source_catalog_screen.dart"
    ).read_text(encoding="utf-8")

    assert "safeDisplayText(" in content_screen
    assert "safeDisplayText(" in source_screen


def test_support_links_and_prayer_api_connection_remain_intact():
    support = (MOBILE / "lib/features/settings/support_sheet.dart").read_text(
        encoding="utf-8"
    )
    prayer = (MOBILE / "lib/core/misr_quran_prayer_times.dart").read_text(
        encoding="utf-8"
    )

    assert "launchUrl" in support
    assert "api.misrquran.gov.eg" in prayer


def test_visits_section_does_not_open_or_package_pdf_files():
    visits = (
        MOBILE / "lib/features/content/ziyara_library_screen.dart"
    ).read_text(encoding="utf-8")
    pubspec = (MOBILE / "pubspec.yaml").read_text(encoding="utf-8")
    router = (MOBILE / "lib/app/router.dart").read_text(encoding="utf-8")

    assert "PdfViewer" not in visits
    assert "launchUrl" not in visits
    assert "https://" not in visits
    assert "assets/data/visits/files/" not in pubspec
    assert "assets/data/visits/transcribed/" in pubspec
    assert "ZiyaraVolumeTextScreen" in router

    catalog = json.loads(
        (MOBILE / "assets/data/visits/catalog.json").read_text(encoding="utf-8")
    )
    documents = catalog["documents"]
    assert catalog["body_transcription_verified"] is False
    assert len(documents) == 6
    for document in documents:
        assert document["transcription_status"] == "ocr_draft_unverified"
        transcription = MOBILE / document["text_asset_path"]
        assert transcription.is_file(), transcription
        assert transcription.stat().st_size > 0, transcription
