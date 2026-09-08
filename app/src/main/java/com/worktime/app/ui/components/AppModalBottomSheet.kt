package com.worktime.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val AppSheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

/**
 * The only modal bottom sheet wrapper: fixed shape, drag handle, optional sentence-case
 * title, navigation-bar padding and horizontal padding. Sheets must not restyle this.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppModalBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = AppSheetShape,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = AppDimens.screenHorizontalPadding)
                .padding(bottom = 16.dp),
        ) {
            PlainDragHandle(
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            if (title != null) {
                AppSheetTitle(text = title)
            }
            content()
        }
    }
}
