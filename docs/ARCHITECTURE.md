# Architecture

## Layers

```text
Compose UI -> screen/destination state holders -> domain contracts -> data implementations
                                      |                |-> Room / DataStore
                                      |                `-> JSON / CSV document codecs
                                      `-> domain calculations / mutation coordination
```

- `domain` owns work-entry invariants, exact calculations, repository contracts, backup-document contracts and cross-store mutation coordination.
- `data` implements Room/DataStore repositories and concrete JSON/CSV document serialization.
- `ui` renders immutable state and sends explicit user actions back to the owning state holder.
- `WorkTimeApp` is the composition root: it wires domain-facing contracts from `AppContainer` to ViewModel factories and owns app navigation/overlay composition.
- Reusable composables receive state and callbacks instead of resolving repositories or ViewModels themselves.

`BackupViewModel` depends on the domain-facing `BackupDocumentSerializer` port. `DefaultBackupDocumentSerializer` and the concrete `BackupCodec`/`WorkEntryCsv` formats remain in `data`; `AppContainer` is the only composition boundary that knows the implementation. Repository/static audit checks prevent production `ui` from importing `data`, `domain` from importing `ui`/`data`, and `data` from importing `ui`.

The project intentionally stays a small single app module. A separate domain module or DI framework would add structure without reducing current complexity.

## State ownership

State is split by feature instead of being accumulated in one root ViewModel:

- `CalendarViewModel` owns visible month, selected day, month-entry snapshots, bulk-rate UI state, recoverable calendar errors and the session-scoped Undo snapshot.
- `PreferencesViewModel` owns theme/default-rate preference state and preference mutations.
- `BackupViewModel` owns import/export state, confirmation, backup errors and rollback behavior.
- `YearSummaryViewModel` is scoped to the Navigation 3 Year Summary destination and owns selected-year summary state.

All repository-backed state exposed to Compose is collected lifecycle-aware. Calendar month and entries are emitted together, and `isReady` blocks editing until persisted state has emitted.

Transient interaction state that does not belong in a ViewModel remains local to its screen. `CalendarPagerState` maps a bounded `HorizontalPager` (1970–2100) to months and keeps it in sync with `CalendarViewModel.visibleMonth`, the single source of truth. The year screen has no pager: arrows change the year in `YearSummaryViewModel`.

## Navigation

The app uses Navigation 3 with a saveable back stack and destination-scoped ViewModel stores.

- Calendar is the root destination.
- Settings is a peer full-screen destination.
- Year Summary is a destination with its own state holder and vertical enter/exit motion.
- Predictive pop uses the same structural exit motion as normal Back.

Navigation objects stay at the app root and are not injected into feature ViewModels.

## Mutation serialization

`DataMutationCoordinator` serializes writes that can affect shared Room/DataStore state across Calendar, Preferences and Backup flows. This prevents concurrent feature mutations from interleaving during operations such as backup replacement.

Calendar Undo is intentionally process-local convenience state. Persisted repository changes survive process recreation; an in-memory Undo snapshot does not.

## Persistence

### Room

One row per date:

```text
dateEpochDay, workedMinutes, hourlyRateMicros, bonusMicros, penaltyMicros, note
```

The legacy `note` column is retained to avoid a destructive schema change. The compact UI does not expose notes and preserves an existing stored value when editing.

### DataStore

Current preferences:

- default hourly rate micros;
- theme mode;
- whether the first-entry default-rate adoption has already been decided.

An old stored currency key may remain on upgraded installations but is not read or written.

### Backup documents

The JSON backup and CSV export formats are infrastructure details in `data`. The UI layer sees only `BackupDocumentSerializer` plus domain models. Import bytes are bounded before decode, decoded data is validated before confirmation, and replacement remains coordinated with repository rollback behavior.

## Amount model

Amounts are stored as `Long` micros. The UI formats them with the fixed `₽`/`₽/h` presentation strings; there is no selectable currency or exchange-rate model.

```text
ratePay = roundHalfUp(workedMinutes x hourlyRateMicros / 60)
entryTotal = ratePay + bonus - penalty
monthTotal = sum(entryTotal)
```

Parsing rejects malformed/exponent input. `MoneyLimits` bounds user-entered components; checked integer arithmetic protects overflow.

## UI surfaces

- `CalendarScreen`: calendar pager plus monthly report card; pager sync is delegated to `CalendarPagerState`.
- `CalendarMonthUi`: immutable per-month cell models built once per page; `CalendarGrid`, `CalendarChrome`, `CalendarSummary`: rendering only.
- `DayEditorSheet`: public sheet entry point; `DayEditorState` validates input; the form is four plain text fields.
- `SettingsScreen`: default rate, theme and data operations driven by dedicated Preferences/Backup state holders.
- `YearSummaryScreen`: view-only yearly totals with per-month bars; empty months are collapsed into one line; state lives in `YearSummaryViewModel`.
- `AppOverlays`, `AppOperationFeedback`, `AppNavigationMotion`: root-only overlay, feedback and motion concerns extracted from `WorkTimeApp`.
- `MoneyFormatting` and `DurationFormatting`: presentation-boundary formatting only.

Write failures keep the relevant editor/settings surface open where applicable and expose a generic localized error without logging personal values. Import is validated before confirmation and compensated across Room/DataStore: a failure after replacement attempts to restore both snapshots; rollback failure is reported separately.
