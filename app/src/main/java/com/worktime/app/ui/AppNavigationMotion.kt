package com.worktime.app.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.unit.LayoutDirection
import com.worktime.app.ui.components.AppMotion

internal fun fullScreenEnterTransition(direction: Int): EnterTransition =
    slideInHorizontally(
        animationSpec = spring(
            dampingRatio = AppMotion.NoBounceDampingRatio,
            stiffness = AppMotion.NavigationStiffness,
        ),
        initialOffsetX = { width -> direction * width / 5 },
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = AppMotion.FastMillis,
            easing = AppMotion.StandardEasing,
        ),
    )

internal fun fullScreenExitTransition(direction: Int): ExitTransition =
    slideOutHorizontally(
        animationSpec = spring(
            dampingRatio = AppMotion.NoBounceDampingRatio,
            stiffness = AppMotion.NavigationStiffness,
        ),
        targetOffsetX = { width -> direction * width },
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = AppMotion.StandardMillis,
            easing = AppMotion.StandardEasing,
        ),
    )

internal fun yearSummaryEnterTransition(): EnterTransition =
    slideInVertically(
        animationSpec = spring(
            dampingRatio = AppMotion.NoBounceDampingRatio,
            stiffness = AppMotion.NavigationStiffness,
        ),
        initialOffsetY = { height -> height / 8 },
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = AppMotion.FastMillis,
            easing = AppMotion.StandardEasing,
        ),
    )

internal fun yearSummaryExitTransition(): ExitTransition =
    slideOutVertically(
        animationSpec = spring(
            dampingRatio = AppMotion.NoBounceDampingRatio,
            stiffness = AppMotion.NavigationStiffness,
        ),
        targetOffsetY = { height -> height / 8 },
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = AppMotion.FastMillis,
            easing = AppMotion.StandardEasing,
        ),
    )

internal fun fullScreenNavigationDirection(layoutDirection: LayoutDirection): Int =
    if (layoutDirection == LayoutDirection.Ltr) 1 else -1
