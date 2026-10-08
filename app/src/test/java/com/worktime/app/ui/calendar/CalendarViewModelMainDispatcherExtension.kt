package com.worktime.app.ui.calendar

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/** Keeps CalendarViewModel's viewModelScope on the same test scheduler as runTest. */
class CalendarViewModelMainDispatcherExtension : BeforeEachCallback, AfterEachCallback {
    private var mainDispatcherInstalled = false

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun beforeEach(context: ExtensionContext) {
        if (context.requiredTestClass.packageName != CalendarViewModel::class.java.packageName) return
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mainDispatcherInstalled = true
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun afterEach(context: ExtensionContext) {
        if (!mainDispatcherInstalled) return
        Dispatchers.resetMain()
        mainDispatcherInstalled = false
    }
}
