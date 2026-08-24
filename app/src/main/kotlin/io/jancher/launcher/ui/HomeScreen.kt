package io.jancher.launcher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.jancher.launcher.R

@Composable
fun HomeScreen(
    isDefaultHome: Boolean,
    onRequestDefaultHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(R.string.home_placeholder_title),
            style = MaterialTheme.typography.displaySmall,
        )
        Text(
            text = stringResource(R.string.home_placeholder_subtitle),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (!isDefaultHome) {
            TextButton(onClick = onRequestDefaultHome) {
                Text(text = "Сделать лаунчером по умолчанию")
            }
        }
    }
}
