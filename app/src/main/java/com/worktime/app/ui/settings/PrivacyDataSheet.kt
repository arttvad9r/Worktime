package com.worktime.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.worktime.app.R
import com.worktime.app.ui.components.AppDimens
import com.worktime.app.ui.components.AppTopBar

@Composable
internal fun PrivacyScreen(onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.screenHorizontalPadding),
        ) {
            AppTopBar(
                title = stringResource(R.string.privacy_and_data),
                onBack = onDismiss,
            )

            Text(
                text = stringResource(R.string.privacy_intro_text),
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            PrivacySection(
                title = stringResource(R.string.privacy_stored_data_title),
                body = stringResource(R.string.privacy_stored_data_text),
            )
            PrivacySection(
                title = stringResource(R.string.privacy_network_title),
                body = stringResource(R.string.privacy_network_text),
            )
            PrivacySection(
                title = stringResource(R.string.privacy_backup_title),
                body = stringResource(R.string.privacy_backup_text),
            )
            PrivacySection(
                title = stringResource(R.string.privacy_deletion_title),
                body = stringResource(R.string.privacy_deletion_text),
            )
            PrivacySection(
                title = stringResource(R.string.privacy_policy_title),
                body = stringResource(R.string.privacy_policy_text),
            )

            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun PrivacySection(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
