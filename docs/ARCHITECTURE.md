# Architecture — 2026 rewrite

## Runtime shape

WorkTime is a single Android application module with a deliberately small dependency graph.

```text
MainActivity
  → ModernAppGraph (process-scoped, applicationContext)
       ├─ Room ModernDatabase (worktime-modern.db)
       └─ ModernRepository
  → ModernViewModel
       ↓ StateFlow / explicit actions
     ModernWorkTimeApp
       ↓ Navigation 3
     Calendar / Month report / Year report / Settings
```

`ModernAppGraph` owns the process-lifetime Room database and repository so Activity recreation/resizing cannot create a new persistence graph underneath a surviving ViewModel. `MainActivity` remains the UI composition root and obtains the graph with `applicationContext`. There is no custom `Application`, Hilt/Koin container, generic repository framework or event bus.

## State

`ModernViewModel` owns the small shared application state required across the four destinations:

- selected month and year;
- observed month/year work-day collections;
- day-editor state;
- staged rate-change preview;
- staged import preview;
- recoverable UI error state.

Repository-backed streams are exposed as `StateFlow` and collected lifecycle-aware in Compose. Transient field input and panel expanded/collapsed state stay local to the relevant composable.

This rewrite currently uses one app-level ViewModel rather than reproducing the previous implementation's larger set of feature ViewModels. Split it only when state ownership or testing pressure provides a concrete reason.

## Navigation and layout

Navigation uses Navigation 3 with a saveable back stack and ViewModel/saveable-state entry decorators.

Destinations:

1. Calendar — root.
2. Month report.
3. Year report.
4. Settings.

System Back and predictive pop use the Navigation 3 stack. Transitions are short directional slide/fade animations.

The root applies safe-drawing insets and limits content width to 720 dp. Phones request portrait orientation. The design must remain usable when Android ignores that request on large/resizable configurations; the six-week calendar means six logical rows, not a hard-coded pixel height.

## Calendar UX

The calendar always renders 42 dates: seven columns by six rows, Monday-first. Adjacent-month dates remain visible but inactive. Each in-month cell can represent today, selected state, an empty date or a saved work day. Saved cells show duration and calculated total.

The bottom monthly summary is compact by default and expands in place. From it the user opens the monthly report, then the yearly report. Selecting a month in the yearly report returns to that month's report.

## Persistence

Room is the single persisted source of truth.

### `modern_work_days`

One aggregate record per date:

- `epochDay` primary key;
- `workedMinutes`;
- hourly-rate snapshot in minor units;
- bonus;
- fine/penalty;
- signed other adjustment;
- optional note.

An empty record (zero duration, zero adjustments, blank note) is deleted instead of persisted.

### `modern_rate_periods`

Stores explicit rate overrides with start date, optional inclusive end date and hourly rate. Newer overlapping periods win for dates without an existing work-day snapshot.

Applying a bulk rate change is transactional: the period is stored and existing work-day snapshots inside that range are explicitly rewritten. Changing only the default rate does not rewrite saved history.

### `modern_settings`

Stores default hourly rate, ISO currency code and theme mode. Settings live in Room in this rewrite; there is no DataStore dependency.

## Money and time

- money: `Long` minor currency units;
- duration: integer minutes;
- worked duration: `0..1440` minutes;
- base earnings: half-up rounding of `rate × minutes / 60`;
- totals: checked integer arithmetic;
- business/data layers reject `Float` and `Double` through static audit.

Floating point is permitted only for presentation-only geometry such as the relative bar width in the yearly report. Monetary text parsing accepts plain decimal notation with comma or dot and deliberately rejects exponent notation.

## Backup and restore

Backup is versioned JSON and includes all data required to restore the rewrite:

- settings;
- work days;
- rate periods.

Decode validates schema version, sizes, duplicate keys/IDs, dates, money bounds, durations, currencies and theme values before the database is changed. Restore runs in a single Room transaction. File access uses Android's system document picker, so broad storage permission is not requested.

## Database evolution

`ModernDatabase` is currently schema version 2. The explicit `1 → 2` migration adds the signed `otherMinor` column with default zero and is covered by an instrumented migration test. Room's generated v2 schema is committed and CI checks it for drift. Destructive migration fallback is forbidden by static audit.

## Legacy boundary

The old runtime packages, old Room schema, old screenshot baselines, widget resources, benchmark modules and previous Baseline Profile are not in active source sets. They remain available in Git history for behavioral comparison only.
