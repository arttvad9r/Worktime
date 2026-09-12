package com.worktime.app.modern.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface ModernDestination : NavKey {
    @Serializable
    data object Calendar : ModernDestination

    @Serializable
    data object MonthReport : ModernDestination

    @Serializable
    data object YearReport : ModernDestination

    @Serializable
    data object Settings : ModernDestination
}
