# Religious content provenance and editorial policy

## Qur'an text

### Active mobile offline bundle

The active mobile Quran reader now loads `apps/mobile/assets/data/quran/` as its local source. Its `ayahs.json` is a byte-for-byte copy of `data/tanzil/simple.json` from the pinned `risan/quran-json` commit `6523a9ad3b73bb7e8cb3a9cdf0ea774b8bab3123`; SHA-256: `703246a73a0a3d57f721b02e3edad9a2ee6831b79f49ffb9513ddfe99a65b801`. The upstream manifest labels this text **Hafs 'an Asim**, Hafs verse numbering, **Simple / Imla'ei**, full vocalization. The raw ayah strings are not normalized, recomposed, edited, or supplemented in the reader.

Tanzil permits verbatim copying and distribution under Creative Commons Attribution 3.0, requires attribution and a link to Tanzil, and prohibits changing the text. The app source notice is `apps/mobile/assets/data/quran/QURAN_SOURCE_NOTICE_AR.md`; the complete text notice must stay with every copy or derivative containing a substantial portion:

> Tanzil Quran Text — Copyright (C) 2007-2021 Tanzil Project — CC BY 3.0 — https://tanzil.net/ — the text may not be modified. See https://tanzil.net/docs/text_license.

`quran_sources.json` records source files and licenses; `quran_attribution.json` separates text-source credit, the reading-chain reference, audio terms, and font license. The chain `Hafs ibn Sulayman ← Asim ibn Abi al-Najud ← Abu Abd al-Rahman al-Sulami ← Ali ibn Abi Talib ← Prophet Muhammad` is recorded as reported by Islamweb Fatwa 100241; this is not an ijazah or performance sanad held by the app or publisher and is not an endorsement by any marja, hawza, or institution. The source edition and chain still require qualified editorial review before a final religious-content approval.

The automated bundle loader checks SHA-256 for the component data files and validates 114 surahs, 6,236 sequential ayahs, 30 juz, 60 full hizbs, 240 quarter boundaries, and 15 source-marked sajdah positions. Tanzil's 240 `hizbs` index points are modeled as 60 hizbs × 4 quarters. Sajdah classifications are source metadata, not a fiqh ruling. Mechanical integrity checks are not a human visual comparison against a printed mushaf. Keep `docs/reports/QURAN_SHA256SUMS.txt` with the release. The checked-in generator is `content/resources/quran/build_mobile_quran_bundle.py`; it only accepts the pinned source snapshot and copies the ayah file byte-for-byte.

The active offline reader uses the local `AmiriQuran.ttf` font. A Unicode codepoint coverage scan found no missing characters for the bundled text; visual shaping and display still need to be exercised on Android devices. Audio is **not bundled**. The mobile app no longer streams or downloads external recitations. It can discover and play only a previously saved local surah file in the existing `quran_audio/islamic_network/ar.alafasy` app-documents directory; those files are not moved or deleted. The repository contains no MP3/OGG or book PDF/EPUB files. Provider and attribution details remain in internal source records only. `EveryAyah` and `MP3Quran` remain excluded because their redistribution-license status is unknown.

### Retained legacy asset and backend staging

The older `apps/mobile/assets/data/quran-hafs-rasm_uthmani.json` and its Quran.ws notice are retained as historical project files but are no longer registered as Flutter assets or read by the active `/quran` route. A separate raw Tanzil Uthmani snapshot still exists under `content/resources/quran/01_QURAN/`; it has not been imported into the backend database. The importer in `services/api/scripts/import_tanzil.py` accepts a UTF-8 TSV snapshot in the exact seven-column format `surah|ayah|text|juz|hizb|page|sajdah`, stores the third field verbatim, computes SHA-256, and never overwrites a populated Quran table. A reviewer identity and qualification must be recorded against the exact hash before the public API can mark that database text ready. The current mobile JSON bundle is a different format and is not an importer input. Before any database import, independently verify the structural mapping, run the importer in dry-run mode, and have a qualified reviewer inspect the exact snapshot against a printed mushaf. Do not normalize or recompose Arabic strings.

## Hadith and narrations

A book's presence in a source collection does not establish the authenticity of every narration. Store the primary book/source separately from grading records. A grading record must identify the scholar, methodology, exact reference/edition and citation. Do not collapse conflicting rijal methods into one generic `sahih/daif` label.

## Duas and ziyarat

Each text needs a source title, exact section/page/number if available, edition, attribution and an editorial reviewer. If textual variants exist, keep separate editions or a documented variant record. Do not merge, modernize or paraphrase a transmitted text while presenting it as verbatim.

Round 15 review: internal provenance records retain references for Ziyarat Amin Allah, the Ziyara of Fatima al-Zahra, Dua al-Sabah and Dua Kumayl, but the mobile UI no longer opens those external pages and embeds none of their full texts. The reviewed source pages did not establish explicit redistribution permission for their digital text versions and did not consistently identify the exact printed edition/page. Keep these entries unavailable in the app until the exact version, reuse basis and review of that version are recorded. For Dua Kumayl, document the selected reading where the source page shows variants.

## Books

Capture title, author, editor, publisher, edition, number of volumes, page boundaries and rights status. Do not distribute a modern edition/PDF merely because the underlying historical work is old; edition, typesetting, annotations, images and digital files may have separate rights.

Internal records note that Alfeker's page describes one `ahlualbayt_lib_pdf.rar` archive of about 2,000 MB and says it contains 4,095 books, with the topics Hadith and Islamic Encyclopedias; the reviewed page did not show an explicit license or permission to redistribute the archive or each book. The archive was not downloaded or bundled. The mobile library now presents the two topic names as unavailable locally, without external buttons or links. Add an individual title only after recording rights for its exact digital file and edition, including redistribution and offline inclusion permission.

## Audio and radio

Record the speaker/reciter, source page, direct stream or file URL, content duration, MIME type, license/permission evidence and permitted uses (streaming, download, caching, offline playback). These URLs are internal provenance fields, not mobile UI links. The app's audio screens now use local files only; without a local file and explicit redistribution permission, show that audio is unavailable. A public website is not automatically permission to mirror its stream or download its files.

## Calendar and historical events

Store date certainty and multiple historical sources. Hijri dates depending on moon-sighting or disputed history must be labelled expected/variant, not absolute. Changes from the admin panel require an audit trail and source attribution.

## QuranEnc translations

QuranEnc's published API documentation permits downloading and re-publishing translation contents subject to its conditions: no content edits/additions/deletions; clear publisher and QuranEnc attribution; version number; preservation of embedded transcript/version information; notifying QuranEnc of translation notes; keeping the newest source version; and no inappropriate advertisements. Keep each original database archive unchanged with its catalog metadata, download URL, version, update timestamp, and SHA-256 in `content/resources/quran/`. This permission does not approve any publisher's interpretation for display: each translation remains staged until its publisher, text, edition, and editorial suitability are reviewed. Do not include the separate QuranEnc font package unless its font redistribution permission is verified.

## HadeethEnc translations

HadeethEnc's official site permits downloading and re-publishing translated content subject to no modification, source/publisher attribution, a release version, preservation of embedded version information, reporting notes, updating to newer releases, and appropriate advertising. The public database download link used in this review did not provide an unambiguous release number for the selected language bundles. No HadeethEnc database was added. Keep it as a conditional reference until the exact publisher version is documented and a qualified reviewer confirms suitability for this Shia-focused application; do not treat the site's general license as scholarly approval.

## Editorial gate

Use statuses `draft -> reviewed -> published` in the next content workflow. The initial CMS currently supports draft/scheduled/published/paused/archived for general announcements, events and ethics lessons. It does not yet implement specialist source review for every scholarly content table; those records must remain unpublished until that workflow exists.
