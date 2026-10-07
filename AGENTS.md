# WorkTime

Local-first Android salary calendar: log hours/rate/bonus/penalty per day, see monthly and yearly earnings. Kotlin/Compose, Room/DataStore, no accounts, cloud, ads or analytics.

## Where things are documented
- Index of all docs: `docs/README.md`
- Product scope and rules: `docs/PRODUCT.md`, `docs/UX.md`, `docs/UI_SYSTEM.md`
- Architecture: `docs/ARCHITECTURE.md`
- Decisions: `docs/DECISIONS.md` — this project keeps decisions there (one `###` section each), not in `docs/adr/`
- Testing and device QA: `docs/TESTING.md`, `docs/ANDROID_QA.md`, `docs/ANDROID_DEVICE_TESTING.md`
- Release: `docs/RELEASE_CHECKLIST.md`, `docs/RELEASE_SIGNING.md`
- Contribution rules and scope limits: `CONTRIBUTING.md`

## Commands
- Full local gate (same as CI): `./scripts/verify.sh`
- Build details and toolchain: `docs/BUILD.md`

## Rules not obvious from the code
- Release signing key and passwords live in git-ignored `keystore/` (see `docs/RELEASE_SIGNING.md`); never commit it or print its contents.
- Instrumentation runs only against `com.worktime.app.debug`; never against the production package (cleanup would wipe real user data).
- Several worktrees exist (`Worktime`, `Worktime-main`, `Worktime-*-polish`, `Worktime-release`); `main` is the source of truth for docs and behavior.

## Status
- 0.1.1 on `main` (2026-10-01): production signing key rotated.
- Next: physical release verification per `docs/ROADMAP.md` ("Next") and `docs/RELEASE_CHECKLIST.md`.
