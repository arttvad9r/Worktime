package com.worktime.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.worktime.app.MainActivity
import com.worktime.app.R
import com.worktime.app.WorkTimeApplication
import com.worktime.app.domain.calculation.SalaryCalculator
import com.worktime.app.domain.model.MonthSummary
import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.domain.repository.UserPreferencesRepository
import com.worktime.app.domain.repository.WorkEntryRepository
import com.worktime.app.ui.format.formatDurationCompact
import com.worktime.app.ui.format.formatAmountMicros
import com.worktime.app.ui.format.formatMonthTitle
import java.time.Duration
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

class WorkTimeWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        val container = (context.applicationContext as WorkTimeApplication).container
        ensureWidgetObservation(
            context = context,
            workEntryRepository = container.workEntryRepository,
            userPreferencesRepository = container.userPreferencesRepository,
        )
        WorkTimeWidgetScope.launch {
            try {
                refresh(
                    context = context,
                    workEntryRepository = container.workEntryRepository,
                    userPreferencesRepository = container.userPreferencesRepository,
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val container = (context.applicationContext as WorkTimeApplication).container
        ensureWidgetObservation(
            context = context,
            workEntryRepository = container.workEntryRepository,
            userPreferencesRepository = container.userPreferencesRepository,
        )
    }

    override fun onDisabled(context: Context) {
        stopWidgetObservation()
        super.onDisabled(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in TIME_INVALIDATION_ACTIONS && hasInstalledWidgets(context)) {
            val pendingResult = goAsync()
            val container = (context.applicationContext as WorkTimeApplication).container
            ensureWidgetObservation(
                context = context,
                workEntryRepository = container.workEntryRepository,
                userPreferencesRepository = container.userPreferencesRepository,
            )
            widgetTimeInvalidations.tryEmit(Unit)
            WorkTimeWidgetScope.launch {
                try {
                    // The process may have been started solely for this broadcast. Refresh
                    // directly as well as invalidating the observer so no event is lost before
                    // its SharedFlow collector becomes active.
                    refresh(
                        context = context,
                        workEntryRepository = container.workEntryRepository,
                        userPreferencesRepository = container.userPreferencesRepository,
                    )
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

// Widget data flows:
// - while at least one widget exists and the process is alive, observe entries and theme;
// - when the process is dead, the system still refreshes at least every updatePeriodMillis (30 min).
private val WorkTimeWidgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
private val widgetTimeInvalidations = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
private var widgetObserverJob: Job? = null
private val TIME_INVALIDATION_ACTIONS = setOf(
    Intent.ACTION_DATE_CHANGED,
    Intent.ACTION_TIME_CHANGED,
    Intent.ACTION_TIMEZONE_CHANGED,
)

private fun hasInstalledWidgets(context: Context): Boolean {
    val appContext = context.applicationContext
    return AppWidgetManager.getInstance(appContext)
        .getAppWidgetIds(ComponentName(appContext, WorkTimeWidgetProvider::class.java))
        .isNotEmpty()
}

fun ensureWidgetObservation(
    context: Context,
    workEntryRepository: WorkEntryRepository,
    userPreferencesRepository: UserPreferencesRepository,
) {
    if (!hasInstalledWidgets(context)) {
        stopWidgetObservation()
        return
    }
    if (widgetObserverJob?.isActive == true) return

    widgetObserverJob = observeForWidget(
        context = context.applicationContext,
        workEntryRepository = workEntryRepository,
        userPreferencesRepository = userPreferencesRepository,
    )
}

private fun stopWidgetObservation() {
    widgetObserverJob?.cancel()
    widgetObserverJob = null
}

suspend fun refresh(
    context: Context,
    workEntryRepository: WorkEntryRepository,
    userPreferencesRepository: UserPreferencesRepository,
) {
    val month = YearMonth.now()
    val entries = workEntryRepository.observeMonth(month).first()
    val themeMode = userPreferencesRepository.preferences.first().themeMode
    push(context, month, SalaryCalculator.monthSummary(entries), themeMode)
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
private fun observeForWidget(
    context: Context,
    workEntryRepository: WorkEntryRepository,
    userPreferencesRepository: UserPreferencesRepository,
): Job = WorkTimeWidgetScope.launch {
    currentMonthFlow()
        .flatMapLatest { month ->
            combine(
                workEntryRepository.observeMonth(month),
                userPreferencesRepository.preferences,
            ) { entries, preferences ->
                WidgetSnapshot(
                    month = month,
                    summary = SalaryCalculator.monthSummary(entries),
                    themeMode = preferences.themeMode,
                )
            }
        }
        .collect { snapshot ->
            push(context, snapshot.month, snapshot.summary, snapshot.themeMode)
        }
}

internal fun currentMonthFlow(
    now: () -> YearMonth = { YearMonth.now() },
    waitUntilNextMonth: suspend (YearMonth) -> Unit = { month ->
        val nextMonth = month.plusMonths(1).atDay(1)
            .atStartOfDay(ZoneId.systemDefault())
        val waitMillis = Duration.between(java.time.Instant.now(), nextMonth.toInstant())
            .toMillis()
            .coerceAtLeast(1_000L)
        delay(waitMillis)
    },
    invalidations: kotlinx.coroutines.flow.Flow<Unit> = widgetTimeInvalidations,
) = channelFlow {
    val reschedules = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    var timer: Job? = null
    launch {
        merge(flowOf(Unit), invalidations, reschedules).collect {
            timer?.cancelAndJoin()
            val current = now()
            send(current)
            timer = launch {
                waitUntilNextMonth(current)
                reschedules.emit(Unit)
            }
        }
    }
    awaitCancellation()
}.distinctUntilChanged()

private fun push(
    context: Context,
    month: YearMonth,
    summary: MonthSummary,
    themeMode: ThemeMode,
) {
    val appContext = context.applicationContext
    val manager = AppWidgetManager.getInstance(appContext)
    val views = remoteViews(appContext, month, summary, themeMode)
    manager.getAppWidgetIds(ComponentName(appContext, WorkTimeWidgetProvider::class.java))
        .forEach { id -> manager.updateAppWidget(id, views) }
}

private fun remoteViews(
    context: Context,
    month: YearMonth,
    summary: MonthSummary,
    themeMode: ThemeMode,
): RemoteViews = RemoteViews(context.packageName, R.layout.work_time_widget).apply {
    explicitWidgetPalette(context, themeMode)?.let { palette ->
        setInt(R.id.widget_root, "setBackgroundResource", palette.backgroundDrawable)
        setInt(R.id.widget_add, "setBackgroundResource", palette.actionBackgroundDrawable)
        setTextColor(R.id.widget_month, palette.secondaryText)
        setTextColor(R.id.widget_summary, palette.primaryText)
        setTextColor(R.id.widget_add, palette.accent)
    }

    setTextViewText(R.id.widget_month, widgetMonthLabel(month))
    setTextViewText(R.id.widget_summary, widgetSummaryLine(context, summary))

    setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent(context, openToday = false))
    setOnClickPendingIntent(R.id.widget_add, openAppPendingIntent(context, openToday = true))
}

private fun widgetSummaryLine(context: Context, summary: MonthSummary): String {
    val shifts = context.resources.getQuantityString(
        R.plurals.shifts_short,
        summary.shiftCount,
        summary.shiftCount,
    )
    val hours = context.getString(
        R.string.hours_short,
        formatDurationCompact(summary.workedMinutes),
    )
    val income = context.getString(
        R.string.amount_with_currency,
        formatAmountMicros(summary.totalPayMicros),
    )
    return "$shifts · $hours · $income"
}

/** System mode deliberately stays resource-driven so values/values-night tracks configuration changes. */
private fun explicitWidgetPalette(context: Context, themeMode: ThemeMode): WidgetPalette? = when (themeMode) {
    ThemeMode.SYSTEM -> null
    ThemeMode.DARK -> WidgetPalette(
        backgroundDrawable = R.drawable.widget_background_dark,
        actionBackgroundDrawable = R.drawable.widget_add_background_dark,
        primaryText = context.getColor(R.color.widget_text_dark),
        secondaryText = context.getColor(R.color.widget_text_secondary_dark),
        accent = context.getColor(R.color.widget_accent_dark),
    )
    ThemeMode.LIGHT -> WidgetPalette(
        backgroundDrawable = R.drawable.widget_background_light,
        actionBackgroundDrawable = R.drawable.widget_add_background_light,
        primaryText = context.getColor(R.color.widget_text_light),
        secondaryText = context.getColor(R.color.widget_text_secondary_light),
        accent = context.getColor(R.color.widget_accent_light),
    )
}

private fun widgetMonthLabel(month: YearMonth): String = formatMonthTitle(month)

private fun openAppPendingIntent(context: Context, openToday: Boolean): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (openToday) {
            putExtra(MainActivity.EXTRA_OPEN_TODAY, true)
        }
    }
    return PendingIntent.getActivity(
        context,
        if (openToday) 1 else 0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

private data class WidgetSnapshot(
    val month: YearMonth,
    val summary: MonthSummary,
    val themeMode: ThemeMode,
)

private data class WidgetPalette(
    val backgroundDrawable: Int,
    val actionBackgroundDrawable: Int,
    val primaryText: Int,
    val secondaryText: Int,
    val accent: Int,
)
