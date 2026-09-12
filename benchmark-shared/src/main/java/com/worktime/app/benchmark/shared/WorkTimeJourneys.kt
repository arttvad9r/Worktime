package com.worktime.app.benchmark.shared

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiAutomatorTestScope
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.textAsString
import androidx.test.uiautomator.uiAutomator
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

const val WorkTimePackage = "com.worktime.app"
const val WorkTimeBenchmarkPackage = "com.worktime.app.benchmark"

private const val NextMonthResourceName = "next_month"

class WorkTimeJourneys(
    private val targetPackage: String = WorkTimePackage,
) {
    private val targetResources by lazy {
        InstrumentationRegistry.getInstrumentation()
            .context
            .packageManager
            .getResourcesForApplication(targetPackage)
    }

    private val locale: Locale by lazy {
        targetResources.configuration.locales[0]
    }

    private val nextMonthDescription: String by lazy {
        val resourceId = targetResources.getIdentifier(
            NextMonthResourceName,
            "string",
            targetPackage,
        )
        check(resourceId != 0) {
            "Target app does not expose string resource $NextMonthResourceName"
        }
        targetResources.getString(resourceId)
    }

    private val expectedNextMonthTitle: String by lazy {
        YearMonth.now()
            .plusMonths(1)
            .format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }

    fun launchCalendar(scope: MacrobenchmarkScope) = with(scope) {
        startActivityAndWait()

        // Resolve localized journey data before a measured block starts.
        expectedNextMonthTitle
        uiAutomator {
            nextMonthIcon().nearestClickableAncestor()
        }
    }

    fun navigateToNextMonth(scope: MacrobenchmarkScope) = with(scope) {
        uiAutomator {
            nextMonthIcon().nearestClickableAncestor().click()

            onElement {
                textAsString() == expectedNextMonthTitle
            }
        }
    }

    private fun UiAutomatorTestScope.nextMonthIcon(): UiObject2 =
        onElement {
            contentDescription?.toString() == nextMonthDescription
        }

    private fun UiObject2.nearestClickableAncestor(): UiObject2 {
        var current: UiObject2? = this
        while (current != null) {
            if (current.isClickable) return current
            current = current.parent
        }
        error("Next-month accessibility node has no clickable ancestor")
    }
}
