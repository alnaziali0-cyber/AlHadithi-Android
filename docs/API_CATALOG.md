# Public catalog API — Khazain Al-Huda

All routes are read-only and versioned under `/api/v1/public`. They return only records that have passed the corresponding publication gate. They do not seed, generate, normalize or infer religious content.

## List sections

`GET /api/v1/public/catalog/{catalog_type}?search={term}&limit=30&offset=0`

Supported `catalog_type` values:

- `book`
- `hadith`
- `dua`
- `visit`
- `event`
- `ethics_lesson`
- `audio`

The response shape is:

```json
{
  "items": [],
  "total": 0,
  "limit": 30,
  "offset": 0
}
```

The search term is trimmed, SQL wildcard characters are escaped, and the API rejects terms longer than 100 characters. `limit` must be between 1 and 100; `offset` must be between 0 and 100000.

## Publication and provenance gates

| Section | Public visibility conditions |
|---|---|
| Books | `publication_status=published`, `rights_status` is `licensed`, `public_domain` or `permission_granted`, non-empty `rights_evidence`, and a recorded HTTPS source URL |
| Hadith | `publication_status=published`, a source record with an edition, and a hadith number or volume/page locator; grading entries are returned separately with their scholar, methodology and citation |
| Dua | `publication_status=published` and a non-empty source reference |
| Visit | `publication_status=published` and a non-empty source reference |
| Event | `is_published=true` and at least one historical source in the source array; date certainty is returned unchanged |
| Ethics lesson | `publication_status=published`; optional Quran/hadith references remain explicit metadata |
| Audio | `is_published=true`, reusable rights status, `streaming_allowed=true`, non-empty rights evidence and an HTTPS source URL. `download_url` is returned only when `download_allowed=true`; `can_cache_offline` is controlled separately by `offline_cache_allowed` |

The schema stores book `rights_evidence` separately from a reusable-rights status. Audio records use three independent booleans: `streaming_allowed`, `download_allowed` and `offline_cache_allowed`. All three default to false in the database; a direct stream being permitted does not imply permission to download or cache the work. The current public API keeps download and offline-cache controls disabled unless their respective flags are explicitly set.

The public API does not equate presence in a source book with authenticity. A hadith grading is an attributed scholarly opinion and is not merged into the narration text or converted to a universal verdict.

## Quran reader

`GET /api/v1/public/quran/surahs` returns the 114-surah index only after the complete import has 6,236 ayahs, structural metadata for every ayah, and an approved import record with reviewer identity and credential.

`GET /api/v1/public/quran/surahs/{surah_number}/ayahs?limit=100&offset=0` returns one surah in canonical ayah order. `surah_number` is 1–114, `limit` is 1–300, and `offset` is 0–286. Before returning text, the service rechecks the approved import, checks that the target surah has exactly its declared ayah count, validates a contiguous sequence starting at ayah 1, and rejects empty text. A failed check returns HTTP 503 rather than a partial or synthesized surah.

Each ayah response includes the original `text_uthmani` value plus ayah, juz, hizb, page and sajdah metadata. The response includes the imported source name, version and SHA-256 digest so the client can display provenance. The latest import record is authoritative: a newer pending or unreviewed import immediately closes the text and bookmark gates even if an older approved record exists. The API does not normalize or regenerate Quran text.

Quran ayahs can be saved in the anonymous device favorites endpoint with `entity_type=quran_ayah` and `entity_id={surah}:{ayah}`. The favorite resolver independently verifies the approved import and existence of that ayah. These marks are device-local, not account-synchronized.

## Book pages

`GET /api/v1/public/books/{book_id}/pages?volume_number=1&limit=40&offset=0`

Pages are available only when the parent book is published and carries a reusable rights status. The endpoint returns the selected book/volume metadata, a page array and pagination metadata. A published book with no digitized pages returns an empty page array; it does not trigger generated text or placeholder pages.

## Client behaviour

The Flutter catalog provider stores the last successful first-page response in SharedPreferences. When the API is unavailable, it may render that cached response with an explicit stale/offline label. The book reader caches each fetched page batch independently by book, volume and offset. This is device-local cache only; it does not provide cross-device synchronization or a guaranteed download license.

## Admin CMS write API

The owner-only CMS API is documented in `ADMIN_CMS.md`. It currently supports controlled create/update/soft-archive operations for books, hadith sources, hadith records, duas, visits, Islamic events, ethics lessons and audio, plus append-only hadith grading and transactional book-page batch import. Mutations require the authenticated admin session and CSRF token, validate relationships and source/rights evidence on the server, and append an audit row in the same transaction.

The current CMS uses one strict JSON editor with related page-import and hadith-grading forms. It is a functional first editorial surface, not yet a rich field-specific editor or a multi-reviewer approval workflow. Fine-grained editorial roles and immutable reviewer sign-off remain future work.

## Current limitations

Verified source datasets, critical scholarly review, permission files, full-text search indexing, and reviewed historical metadata are still required. The API and CMS do not seed or invent any religious material. Existing draft or unverified records remain hidden from public routes.

## المكتبة الشخصية (مجهول الهوية)

| Method | Endpoint | Notes |
|---|---|---|
| GET | `/api/v1/user/favorites` | يتطلب `X-Device-Key` صالحاً ويعيد حتى 100 عنصر |
| POST | `/api/v1/user/favorites` | يتطلب `X-Device-Key` وحقول `entity_type` و`entity_id`؛ الإضافة idempotent |
| DELETE | `/api/v1/user/favorites/{entity_type}/{entity_id}` | يحذف من مساحة الجهاز الحالي فقط |
| GET | `/api/v1/user/books/{book_id}/reading-progress` | يعيد `progress: null` عند عدم وجود موضع |
| PUT | `/api/v1/user/books/{book_id}/reading-progress` | يتطلب `volume_number` و`page_number` لصفحة منشورة موجودة |

راجع `DEVICE_LIBRARY.md` لنموذج الثقة وحدود استخدام مفتاح الجهاز.


### إذن التخزين المحلي للكتب

ترجع بيانات الكتاب الحقل `offline_access_allowed` بصورة مستقلة عن `rights_status`. القيمة الافتراضية `false`. لا يجوز للعميل حفظ محتوى الكتاب محلياً ما لم تكن القيمة `true`. لوحة الإدارة لا تقبل تفعيلها إلا عند وجود حالة حقوق معتمدة ودليل حقوق ومصدر HTTPS.


## Reference and Iraqi prayer-city catalogue (Round 6)

- `GET /api/v1/public/reference/sources` — read-only list of the 9 institutional/reference sources. Contact emails are deliberately not returned. `license_status` is informational and never grants permission.
- `GET /api/v1/public/prayer/cities` — city records imported from the supplied pack. Optional filters: `governorate_id`, `q`.
- `GET /api/v1/public/prayer/times?city_id=IQ-BB-HIL&day=YYYY-MM-DD&method=jafari_qom` — calculate a daily timetable from the selected city's approximate center. Do not combine `city_id` and `governorate_id`.
- `GET /api/v1/public/prayer/methods` — returns angle parameters and explicitly reports `officially_verified=false` until an authority-issued profile is reviewed.

All imported city coordinates remain `verified=false` and `approximate_city_center`; the response includes their recorded coordinate source. Prayer profile presets remain non-authoritative pending documentary confirmation and comparative QA.

## Foreground GPS prayer calculation (Round 7)

- `POST /api/v1/public/prayer/times/coordinates` with JSON `{latitude, longitude, day, method}` — calculate using coordinates only after the user explicitly chooses GPS and requests prayer times. Coordinates are sent in the request body, not the URL.
- Coordinate-based requests return `cache_status=not_cached_user_coordinates` and are not inserted into `PrayerCache`; exact GPS coordinates are therefore not persisted in the application prayer cache.
- Requests using `governorate_id` or `city_id` continue to use the server cache. Manual governorate selection uses `governorate_id`; choosing a city uses `city_id`.
- Production reverse proxies and APM agents must redact `latitude` and `longitude` query parameters from access logs and traces.

## External MisrQuran prayer-time source (Round 21)

- The mobile app requests today's timetable from `GET https://api.misrquran.gov.eg/api/PrayerTimes/GetTodaysTimes?lat={latitude}&lng={longitude}&dayLightSaving=false`. It uses a dedicated Dio client, without the app's `X-Device-Key` interceptor.
- A user-selected GPS location is sent only to this API after the user's explicit approval. Because this endpoint uses query parameters, the provider can receive them in its request infrastructure/logs; the app does not log the precise coordinates or include them in SharedPreferences cache keys or saved result payloads. The local cache identity uses a random GPS token plus the Baghdad service date.
- The API response's UTC clock fields are converted once to `Asia/Baghdad`. The app accepts only the Baghdad service date matching the current day. It uses this API for today's screen, home summary, and today's notification; the next 11 notification days remain on the existing local calculation provider.
- A same-location/day cached API result is marked stale after a request failure; if none exists, the current local calculation is used. The API response may have `sunset` equal to `maghrib`, and the app does not synthesize missing Islamic-midnight values. The endpoint was observed to ignore an added `date` parameter, so it is not used for future dates.
- The service's calculation method and religious authority status are not independently verified. Displayed results are not a fatwa or a claim of approval by a religious authority.
