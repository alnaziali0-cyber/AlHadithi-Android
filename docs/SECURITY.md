# Security model and production controls

## Admin identity and sessions

- No admin registration endpoint exists.
- One owner is bootstrapped from environment secrets only when the admin table is empty.
- The bootstrap password is never embedded in Dart, JavaScript, SQL migrations or source control.
- Password hashes use Argon2id with explicit time/memory/parallelism parameters.
- First login requires password rotation; changing the password revokes all active sessions.
- Session identifiers are cryptographically random opaque values. Only keyed hashes are stored in PostgreSQL.
- The browser session cookie is HttpOnly, SameSite=Strict, scoped to `/api/v1/admin` and Secure in production.
- Unsafe admin requests require a CSRF token bound to the stored session. Origin allow-listing and CORS are explicit.
- Failed login attempts are counted per username/IP fingerprint in PostgreSQL and temporarily blocked.
- The shipped dashboard supports only the owner role. The `role` column is not a self-service role-management interface.

## Application/API hardening

- HTTPS is terminated by Caddy in the supplied Compose configuration; HSTS is set when `APP_ENV=production`.
- PostgreSQL is on a private network and is not mapped to a public host port.
- Public endpoints are read-only; admin mutations require a valid owner session and CSRF.
- External links and radio stream URLs accept absolute HTTPS only. The API does not server-fetch user-submitted URLs.
- Uploaded assets have a 15 MiB default cap, are written under random UUID names and have their magic headers checked against an allow-list. The original filename is metadata only.
- Dashboard rendering uses DOM `textContent`, not HTML injection. Markdown is currently rendered as plain text in the mobile client.
- Admin operations are recorded in `admin_audit_logs`; raw IP and user-agent are not stored, only fingerprints.
- `Permissions-Policy` disables camera, microphone and geolocation for the API/admin web origin. Mobile permissions are not requested by the current app.

## Production requirements before exposure

1. Put secrets in a managed secret store. Generate an independent random `APP_SECRET`, PostgreSQL password and bootstrap password.
2. Configure exact `DOMAIN` and `ALLOWED_ORIGINS`; do not use wildcard CORS.
3. Run PostgreSQL on a supported maintained release, encrypt volumes/backups and restrict database access.
4. Place a WAF or ingress rate limiter in front of the API; the current login throttle is database-backed but is not a substitute for a full edge abuse-control system.
5. Replace local media storage with private object storage plus a controlled delivery path when scaling beyond one API node.
6. Add secret rotation, dependency scanning, SAST, DAST, penetration testing and incident response procedures.
7. Configure log retention, monitoring, alerting and tested restore drills.
8. Review file decoding/re-encoding and malware scanning before making uploaded media publicly downloadable.

## Explicit limitations

This repository has not undergone an external penetration test or formal security audit. End-user authentication, email-based recovery, user deletion endpoint, push-device registration and account synchronization are not implemented. Do not advertise those features as available.
