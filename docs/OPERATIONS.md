# Operations and deployment

## Services

- `api`: FastAPI, Alembic migrations and admin console. The container applies the migration head before starting Uvicorn.
- `db`: PostgreSQL, private network only.
- `publisher`: dedicated 30-second worker for scheduled CMS items.
- `caddy`: HTTPS ingress / TLS certificate management.

## Secrets

The root `.env.example` is a template, not a usable configuration. Create a deployment-local `.env` outside source control. Generate values from a cryptographic password manager or secret manager. The database password should be URL-safe because it is interpolated into the SQLAlchemy URL in Compose. Never reuse the bootstrap admin password elsewhere.

## Backups

At minimum:

- Nightly PostgreSQL logical backup (`pg_dump -Fc`) to encrypted off-host storage.
- Daily media-volume backup or object-storage versioning.
- Retention policy (e.g. 7 daily, 4 weekly, 12 monthly) agreed by the operator.
- Monthly restore drill into an isolated environment, including media checksum verification.
- Monitor backup age and alert if the latest successful backup exceeds its SLA.

The Compose file does not create backups automatically; install a scheduled job in the hosting environment. The publisher waits for the API readiness probe, which in turn runs only after its Alembic migration command succeeds.

## Monitoring

Monitor `/health` for process liveness and `/ready` for database readiness, API latency/error rate, database connection pool, storage capacity, publication worker logs, media upload failures and Caddy certificate expiry. Admin service status is available at `/api/v1/admin/services`. Do not expose admin health details publicly.

## Media storage migration

The initial implementation stores uploads on a persistent local volume. Before horizontal scaling, move media to a private S3-compatible bucket, use a key derived from UUID only, scan/re-encode assets, issue signed or controlled public delivery URLs and preserve SHA-256 and rights evidence.

## Publication worker

Run the separate `publisher` service continuously. It promotes due scheduled records to `published` and archives expired records. It is idempotent; database transaction failures are logged and retried on the next poll. This worker currently handles CMS publication, not push notifications.
