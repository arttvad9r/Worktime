# Privacy and data handling — 2026 rewrite

WorkTime is local-first and does not require an account or network connection for its current functionality.

## Local data

The rewrite stores its data in the app sandbox with Room:

- per-date worked minutes;
- hourly-rate snapshot;
- bonus, fine and signed other adjustment;
- optional note;
- rate override periods;
- default rate;
- selected currency code;
- theme mode.

These values are processed on-device. There is no DataStore or cloud-sync layer in the rewrite.

## Network and permissions

The application manifest does not request `INTERNET`, location, contacts, microphone, camera or broad storage permissions. The current dependency graph does not intentionally include analytics or advertising SDKs.

Automatic Android app-data backup/device transfer is disabled by the application configuration. Release QA must still inspect the merged manifest and exact final dependency graph rather than relying on this document alone.

## Export and import

Data leaves the sandbox only through an explicit user action:

- export creates a versioned JSON backup at a destination chosen through Android's system document picker;
- import reads a user-selected document, validates it, shows a preview and requires confirmation before transactional restore.

If the user selects a cloud-backed document provider, transfer to that provider is caused by that explicit destination choice; WorkTime itself does not upload the backup.

The rewrite does not currently export CSV.

## Deletion and retention

Individual dates can be deleted in the app. A saved date containing no duration, adjustments or note is removed rather than retained as an empty database row. Uninstalling removes the app sandbox subject to Android platform behavior. Exported JSON files are outside the sandbox and must be removed separately from wherever the user saved them.

WorkTime has no developer-side account database and no server-side copy of work/earnings data.

## Release verification

Before a public release, verify the exact signed candidate:

1. merged manifest and requested permissions;
2. dependency graph for unexpected networking/telemetry SDKs;
3. backup/data-extraction rules;
4. JSON import/export behavior through the system picker;
5. logs and error messages for accidental personal/work-value disclosure.
