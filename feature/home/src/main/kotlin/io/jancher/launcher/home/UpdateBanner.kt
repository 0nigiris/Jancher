package io.jancher.launcher.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.jancher.launcher.model.UpdateError
import io.jancher.launcher.model.UpdateState

/**
 * Плашка обновления внизу экрана.
 *
 * Не диалог: модальное окно поверх домашнего экрана при каждом запуске
 * лаунчера — это ровно тот тип навязчивости, от которого мы уходим.
 * Плашка не мешает пользоваться лаунчером и уходит по «Позже».
 */
@Composable
fun UpdateBanner(
    state: UpdateState,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = state !is UpdateState.Idle && state !is UpdateState.Checking

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = modifier,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            when (state) {
                is UpdateState.Available -> {
                    Text(
                        text = "Есть версия ${state.info.versionName}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (state.info.notes.isNotBlank()) {
                        Text(
                            text = state.info.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Actions(
                        primary = "Обновить" + state.info.sizeBytes.asSizeSuffix(),
                        onPrimary = onDownload,
                        onSecondary = onDismiss,
                    )
                }

                is UpdateState.Downloading -> {
                    Text(
                        text = "Загрузка ${state.info.versionName}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    )
                }

                is UpdateState.ReadyToInstall -> {
                    Text(
                        text = "Версия ${state.info.versionName} готова",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Система спросит подтверждение",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Actions(primary = "Установить", onPrimary = onInstall, onSecondary = onDismiss)
                }

                is UpdateState.Failed -> {
                    Text(
                        text = when (state.reason) {
                            UpdateError.CHECKSUM -> "Файл скачался повреждённым"
                            UpdateError.INSTALL_FAILED -> "Установка не удалась"
                            UpdateError.MALFORMED -> "Непонятный ответ сервера"
                            UpdateError.NETWORK -> "Не удалось скачать"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Actions(primary = "Повторить", onPrimary = onDownload, onSecondary = onDismiss)
                }

                else -> Unit
            }
        }
    }
}

@Composable
private fun Actions(
    primary: String,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = primary,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onPrimary),
        )
        Text(
            text = "Позже",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable(onClick = onSecondary),
        )
    }
}

private fun Long.asSizeSuffix(): String =
    if (this <= 0) "" else " · ${this / 1024 / 1024} МБ"
