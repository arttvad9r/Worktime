package com.worktime.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.worktime.app.R
import com.worktime.app.domain.model.MoneyLimits
import com.worktime.app.ui.components.AppDimens
import com.worktime.app.ui.components.AppModalBottomSheet
import com.worktime.app.ui.components.AppNavigationRow
import com.worktime.app.ui.components.AppPrimaryButton
import com.worktime.app.ui.components.AppRowDivider
import com.worktime.app.ui.components.AppSectionSurface
import com.worktime.app.ui.components.AppSegmentedControl
import com.worktime.app.ui.components.CompactMoneyField
import com.worktime.app.ui.components.LabelValueRow
import com.worktime.app.ui.format.parseDecimalMicros
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private enum class RatePeriod { CURRENT_MONTH, CUSTOM }

private enum class DateField { Start, End }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeRateSheet(
    visibleMonth: YearMonth,
    initialRange: ClosedRange<LocalDate>?,
    operationErrorMessage: String?,
    onDismiss: () -> Unit,
    onChangeRate: (LocalDate, LocalDate, Long) -> Unit,
) {
    var period by rememberSaveable(initialRange) {
        mutableStateOf(if (initialRange != null) RatePeriod.CUSTOM else RatePeriod.CURRENT_MONTH)
    }
    var customStart by rememberSaveable(initialRange) {
        mutableStateOf(initialRange?.start)
    }
    var customEnd by rememberSaveable(initialRange) {
        mutableStateOf(initialRange?.endInclusive)
    }
    var rate by rememberSaveable { mutableStateOf("") }
    var pickingDate by rememberSaveable { mutableStateOf<DateField?>(null) }
    var confirmChange by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(operationErrorMessage) {
        if (!operationErrorMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(operationErrorMessage)
        }
    }

    val startDate: LocalDate?
    val endDate: LocalDate?
    if (period == RatePeriod.CURRENT_MONTH) {
        startDate = visibleMonth.atDay(1)
        endDate = visibleMonth.atEndOfMonth()
    } else {
        startDate = customStart
        endDate = customEnd
    }

    val parsedRate = runCatching { parseDecimalMicros(rate) }.getOrNull()
    val rateValid = parsedRate != null && parsedRate > 0L && parsedRate <= MoneyLimits.MAX_COMPONENT_MICROS
    val rangeValid = startDate != null && endDate != null && startDate <= endDate
    val canChange = rateValid && rangeValid

    val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", LocalLocale.current.platformLocale)
    val rateLabel = stringResource(R.string.hourly_rate)

    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.rate_for_period),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppSegmentedControl(
                    options = listOf(
                        stringResource(R.string.current_month),
                        stringResource(R.string.custom_period),
                    ),
                    selectedIndex = if (period == RatePeriod.CURRENT_MONTH) 0 else 1,
                    onSelect = { index ->
                        period = if (index == 0) RatePeriod.CURRENT_MONTH else RatePeriod.CUSTOM
                    },
                )

                AppSectionSurface {
                    if (period == RatePeriod.CURRENT_MONTH) {
                        LabelValueRow(
                            label = stringResource(R.string.start_date),
                            value = visibleMonth.atDay(1).format(dateFormatter),
                            modifier = Modifier.heightIn(min = AppDimens.rowMinHeight),
                        )
                        AppRowDivider()
                        LabelValueRow(
                            label = stringResource(R.string.end_date),
                            value = visibleMonth.atEndOfMonth().format(dateFormatter),
                            modifier = Modifier.heightIn(min = AppDimens.rowMinHeight),
                        )
                    } else {
                        AppNavigationRow(
                            label = stringResource(R.string.start_date),
                            value = customStart?.format(dateFormatter) ?: "",
                            onClick = { pickingDate = DateField.Start },
                        )
                        AppRowDivider()
                        AppNavigationRow(
                            label = stringResource(R.string.end_date),
                            value = customEnd?.format(dateFormatter) ?: "",
                            onClick = { pickingDate = DateField.End },
                        )
                    }

                    AppRowDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = AppDimens.rowMinHeight),
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.rowGap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = rateLabel,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                        CompactMoneyField(
                            text = rate,
                            onTextChange = { rate = it },
                            isError = rate.isNotEmpty() && !rateValid,
                            contentDescription = rateLabel,
                        )
                    }
                }

                AppPrimaryButton(
                    text = stringResource(R.string.change_rate),
                    onClick = { confirmChange = true },
                    enabled = canChange,
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(AppDimens.screenHorizontalPadding),
            )
        }
    }

    when (val target = pickingDate) {
        DateField.Start -> DatePickerSheetDialog(
            initialDate = customStart ?: visibleMonth.atDay(1),
            onSelect = { selectedStart ->
                customStart = selectedStart
                customEnd?.let { selectedEnd ->
                    if (selectedEnd < selectedStart) {
                        customEnd = null
                    }
                }
                pickingDate = null
            },
            onDismiss = { pickingDate = null },
        )

        DateField.End -> DatePickerSheetDialog(
            initialDate = customEnd?.takeIf { selectedEnd ->
                customStart == null || selectedEnd >= customStart
            } ?: customStart ?: visibleMonth.atEndOfMonth(),
            minimumDate = customStart,
            onSelect = {
                customEnd = it
                pickingDate = null
            },
            onDismiss = { pickingDate = null },
        )

        null -> Unit
    }

    if (confirmChange) {
        AlertDialog(
            onDismissRequest = { confirmChange = false },
            title = { Text(stringResource(R.string.change_rate_confirmation_title)) },
            text = { Text(stringResource(R.string.change_rate_confirmation_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmChange = false
                        val start = startDate
                        val end = endDate
                        val safeRate = parsedRate
                        if (start != null && end != null && safeRate != null && safeRate > 0L) {
                            onChangeRate(start, end, safeRate)
                        }
                    },
                ) { Text(stringResource(R.string.change_rate)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmChange = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 0.dp,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerSheetDialog(
    initialDate: LocalDate,
    minimumDate: LocalDate? = null,
    onSelect: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val selectableDates = remember(minimumDate) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                minimumDate == null || utcTimeMillis.toLocalDate() >= minimumDate

            override fun isSelectableYear(year: Int): Boolean =
                minimumDate == null || year >= minimumDate.year
        }
    }
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toUtcMillis(),
        selectableDates = selectableDates,
    )
    val pickerColors = DatePickerDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        navigationContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        dayContentColor = MaterialTheme.colorScheme.onSurface,
        selectedDayContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedDayContainerColor = MaterialTheme.colorScheme.primaryContainer,
        todayContentColor = MaterialTheme.colorScheme.primary,
        todayDateBorderColor = MaterialTheme.colorScheme.primary,
        dividerColor = MaterialTheme.colorScheme.outlineVariant,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = pickerState.selectedDateMillis != null,
                onClick = {
                    pickerState.selectedDateMillis?.let { onSelect(it.toLocalDate()) }
                },
            ) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 0.dp,
        colors = pickerColors,
    ) {
        DatePicker(
            state = pickerState,
            title = null,
            headline = null,
            showModeToggle = false,
            colors = pickerColors,
        )
    }
}

private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
