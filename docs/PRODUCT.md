# Product specification

## Product statement

WorkTime is a personal, phone-first Android timesheet that records actual worked time by date and immediately shows expected income for the selected month.

It is a salary calendar, not a project tracker, shift planner, timer, HR system or payroll suite. The compact phone layout is the primary product surface, but the application remains resizable and adapts to the available app window instead of locking orientation or assuming one device resolution.

## Primary flow

1. Open the required month (arrows, swipe or month picker).
2. Tap a date; today's empty cell shows a `+`.
3. Enter duration and hourly rate.
4. Optionally add a bonus and/or penalty.
5. Review the calculation and save.
6. Read the monthly report card under the calendar.
7. Tap the card to open the yearly summary.

## Calendar

- Monday-first fixed 6 × 7 grid that fills the available height; no grid lines.
- Previous/next arrows, a horizontal pager and a month-picker dialog provide navigation. The ViewModel's `visibleMonth` is the single source of truth.
- Adjacent-month dates remain visible but faint and inactive.
- Filled cells show date, worked duration and daily income (hidden at large font scales) on a soft neutral surface; today has an outline; bonus/penalty days carry a small green/red dot.
- Each month page is built once as an immutable `CalendarMonthUi`; cells do no calculation.
- Compact-height windows scroll instead of squeezing the grid. Rotation, split-screen and resizing must not require an orientation lock.

## Day editor

- Modal bottom sheet with four ordinary text fields: duration, rate, bonus and penalty (bonus and penalty share a row).
- The keyboard stays closed on open (no focus until a field is tapped); IME Next moves through the fields, Done on penalty saves.
- Duration accepts `H`, `H:MM` or `HH:MM` up to 24:00; empty means zero.
- Under the duration field up to four one-tap chips suggest durations: the most frequent one for this weekday in the last 180 days, then the most frequent overall, then defaults (8, 12, 10, 6 h). With a 13 h weekday / 15 h Fri–Sat habit the chips read `8 | 12 | 13 | 15` on any day.
- Calculation shows pay by rate, optional adjustments and total; Save is primary and existing entries can be deleted.
- Invalid values use outline-only error treatment; helper text is intentionally absent.

## Monthly information

A card under the calendar shows the month's income, shift count and worked hours, plus base/bonus/penalty when adjustments exist. Every record's pay is rounded to cents and totals are sums of rounded records, so the cell, card and yearly numbers always agree. Tapping the card opens the yearly summary.

## Year summary

The yearly summary is a full-screen, view-only surface opened from the monthly report card.

It shows total yearly income, shifts and hours, the average per month with data, and bonus/penalty totals when applicable. Each populated month is one row with a proportional bar (red for a negative month); months without records are collapsed into a single muted line. Arrows switch the year with a short cross-fade.

## Settings

Settings are grouped into `Calculation`, `Appearance` and `Data`:

- default hourly rate, edited inline;
- `Change rate for period`;
- system/light/dark theme segmented control;
- JSON backup export, CSV spreadsheet export and JSON import.

The shared segmented control uses one animated selected pill and emits one light tactile tick only when selection actually changes. Theme-mode changes interpolate visible Material color roles rather than flashing between complete palettes. The label is explicitly `Default rate`: it is not the rate of the currently selected day.

### Default-rate behavior

- If the default rate has never been initialized, the hourly rate of the first saved worked entry can initialize it.
- Once initialized, entering a different rate for an individual day affects only that entry and does not overwrite the default.
- Manually changing the default in Settings marks it initialized.

### Change rate for period

- Applies a new hourly rate to every entry inside an inclusive current-month or custom date range.
- Custom end dates earlier than the selected start are disabled by the Material date picker; moving the start beyond an existing end clears the invalid end.
- Confirmation states that entries in the period change while the default rate stays unchanged.
- The operation does not alter durations, bonuses or penalties.
- Successful bulk changes can be undone from the root Snackbar.

## Data export and import

- JSON export is the complete backup format and can be imported back.
- Backup version 2 preserves both visible preferences and the hidden default-rate initialization state, so export/import is a behavioral round trip even when the stored default rate is zero.
- Version 1 JSON backups remain import-compatible; legacy initialization state is inferred from the stored default and worked entries because the old format did not contain the flag explicitly.
- CSV is spreadsheet-oriented and export-only.
- Import validates the complete file before replacing current entries/settings and uses compensation snapshots if the multi-store replacement fails.
- Core operation remains fully local; export/import uses the system document picker.

## Home-screen widget

An optional 4 × 1 widget shows the current month, shift count, worked hours and income. Tapping the body opens WorkTime; the compact `+` action opens today's day editor directly. Explicit Light/Dark choices follow the app preference, while System mode remains resource-driven and follows the device configuration. Live Room/DataStore observation is kept only while at least one widget is installed; system update/date invalidation paths remain as fallback refresh mechanisms. The widget supports horizontal resizing.

## Launch behavior

The app uses the AndroidX SplashScreen compatibility API. The splash surface follows the current light/dark base surface and exits quickly into real content; there is no artificial branding delay.

## Haptic behavior

Haptics are deliberately sparse: a user-driven month pager snap and actual segmented selection get a light tick; the summary upward gesture gets one threshold cue; Save/Delete/non-empty bulk rate changes get confirmation only after persistence succeeds. Ordinary navigation taps, arrows, day selection and text-field focus do not add vibration.

## Business rules

- One aggregate record per date.
- Duration range is `0..1440` minutes; `24:00` is valid, `24:01` is not.
- Worked time requires a positive hourly rate.
- Bonus/penalty-only records are valid and do not increase work-day count.
- Historical records retain their saved hourly rate unless explicitly changed by the bulk-rate operation.
- Bulk rate change never modifies the default rate.
- Entry deletion and bulk rate changes can be undone through the most recent in-memory undo snapshot; undo does not survive process death.
- The UI uses fixed `₽` labels; there is no currency selector or exchange-rate behavior.
- User-entered amounts accept at most two fractional digits.
- Domain/data calculations store integer micros and do not persist binary floating-point values.

## Non-goals

- registration or cloud sync;
- time clock/background timer;
- projects, clients or invoices;
- scheduled shifts/overtime rules;
- taxes, exchange rates or multi-currency accounting;
- notes or quick-duration templates;
- validation helper text;
- a separate device-model-specific or landscape-only product mode;
- looping/decorative animation or motion that delays user actions.

## Release criteria

- `./scripts/verify.sh` completes on the release candidate when runner/toolchain access is available.
- Core create/edit/delete/relaunch, bulk-rate, import/export and widget paths pass on physical hardware.
- Editor IME transitions and modal-sheet gestures remain stable.
- Calendar, reports, settings and year summary remain usable without clipping across the supported compact/expanded window states, supported font scales and locales.
- Rotation/window resize does not reset persisted feature state or expose an unsupported fixed-resolution layout.
- Motion and haptics remain smooth/restrained on supported devices and do not change interaction timing or data semantics.
- No known data-loss or calculation defect remains.
