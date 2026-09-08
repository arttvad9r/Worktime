# Privacy and data handling

WorkTime is local-first. It does not require an account or internet connection for its core functions.

This document is the repository privacy audit and the source of truth for the in-app `Privacy` disclosure. WorkTime may be distributed through GitHub Releases and Android application stores such as RuStore. Store-specific privacy, data-safety, permissions and developer-disclosure forms must be completed from the exact release candidate rather than from assumptions about an earlier build.

## Stored data

Room stores per-date worked minutes, hourly-rate snapshot, bonus and penalty. The schema also contains a legacy note column; the current UI does not expose notes.

DataStore stores the default hourly rate and selected theme. Currency is not collected or stored by the current model; an unused legacy preference key may remain on an upgraded installation.

These values are processed on-device and are not automatically sent to the developer or a third party. WorkTime does not create an account or keep a developer-side copy of work/earnings data.

## Network and permissions

- The application manifest does not request `INTERNET`, location, contacts, microphone or camera permission.
- No analytics, advertising or crash-reporting SDK is part of the current dependency graph.
- The app does not automatically transmit work, earnings or settings data.
- Financial/work values must not be written to logs or error messages.

Recheck these statements against the exact final release dependency graph and merged manifest before every public release. Adding networking, analytics, crash reporting, advertising or another SDK can change the privacy model even if the visible application flow is unchanged.

## Backup, export and import

Android cloud backup and device-to-device transfer are disabled/excluded for app data by the manifest and extraction/backup rules. This behavior must still be checked on release devices because manufacturer behavior can differ.

JSON/CSV export is explicit and user-directed. The app writes to the destination selected by the user through the Android system document picker. If the user chooses another app or a cloud-backed provider, that transfer occurs because of the user's explicit destination choice; WorkTime does not automatically upload the file itself. Further handling by the selected external service is governed by that service's own terms and privacy policy.

Import reads only the file explicitly selected by the user through the system picker.

## Support contact

The app itself does not transmit support data. If a user voluntarily contacts the published support address, the developer may receive the information included in that message, such as the sender address, message text and attachments. The public privacy policy must describe this separately from the local-only app data path and must not imply that local WorkTime records are uploaded automatically.

## Deletion and retention

Users can delete individual entries in the app. Uninstalling the app removes its local app data subject to Android platform behavior. Exported files are outside the app sandbox and must be removed separately from the destination selected by the user.

Any information voluntarily sent to the support contact is outside the app-local database and follows the retention/deletion terms stated in the public privacy policy.

## Release privacy matrix

Treat these statements as release-time assertions, not permanent guarantees:

| Area | Current WorkTime behavior |
| --- | --- |
| Account/login | None |
| Automatic network transmission | None |
| Analytics | None |
| Advertising | None |
| Crash-reporting SDK | None |
| Location/contacts/camera/microphone | Not requested |
| Work/earnings/settings data | Stored and processed locally |
| Cloud backup/device transfer | Disabled by app configuration |
| Export | Explicit user-directed JSON/CSV through system picker |
| Import | Explicit user-selected file through system picker |
| Local deletion | Individual entries can be deleted; uninstall removes app sandbox subject to Android behavior |
| Support contact | Only if the user independently sends a message to the published support address |

If any future feature or dependency automatically sends data off-device, update this document, the in-app disclosure, store declarations and public privacy policy before distributing that build.

## RuStore declaration notes

RuStore analyzes manifest permissions automatically but also instructs developers to add user-entered data types that are not implied by permissions. The RuStore taxonomy includes salary as an example of `Другие финансовые данные`.

For the current WorkTime build, the release operator must therefore review the exact wording of the RuStore `Безопасность данных пользователя` form after uploading the APK. Where the form distinguishes local processing from collection/transmission, describe work/rate/income values as used locally for app functionality and not transmitted to the developer or third parties. Do not hide a user-entered data type merely because no Android permission is required for it.

The public RuStore legal templates are maintained under `docs/rustore/`. They contain developer-identity placeholders that must be replaced by the account owner before submission.

## Release verification

Before every public release:

1. Inspect the exact signed APK dependency graph and merged manifest.
2. Confirm there is no unexpected network, analytics, advertising or crash-reporting data path.
3. Recheck backup and device-transfer rules.
4. Re-run managed-device and physical-device release QA.
5. Compare the in-app Privacy disclosure with this document.
6. Compare every active store privacy/data-safety declaration with this document.
7. Confirm public privacy-policy and user-agreement links are current and accessible without authentication.
8. Confirm release assets and workflow logs contain no user data, signing secrets or other sensitive information.
