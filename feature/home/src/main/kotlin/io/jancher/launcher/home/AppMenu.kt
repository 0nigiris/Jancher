package io.jancher.launcher.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.jancher.launcher.model.App
import io.jancher.launcher.platform.Shortcut

/**
 * Меню приложения по долгому нажатию.
 *
 * Раньше долгое нажатие сразу бросало приложение в избранное — быстро,
 * но необратимо на ощупь: промахнулся пальцем и уже что-то изменил.
 * Меню оставляет то же действие одним касанием, но делает его осознанным
 * и заодно даёт то, ради чего в лаунчере вообще нужно долгое нажатие —
 * шорткаты приложения.
 */
@Composable
fun AppMenu(
    app: App,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shortcutSource = LocalShortcutSource.current
    var shortcuts by remember(app.key) { mutableStateOf<List<Shortcut>>(emptyList()) }

    LaunchedEffect(app.key) {
        shortcuts = shortcutSource.load(app.key)
    }

    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA))
            .clickable(onClick = onDismiss),
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .systemBarsPadding()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(vertical = 8.dp),
        ) {
            Text(
                text = app.displayLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )

            shortcuts.forEach { shortcut ->
                MenuItem(shortcut.label) {
                    shortcutSource.start(shortcut, app.key, null)
                    onDismiss()
                }
            }

            if (shortcuts.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }

            MenuItem(if (isFavorite) "Убрать из избранного" else "В избранное") {
                onToggleFavorite()
                onDismiss()
            }
            MenuItem("О приложении") {
                onOpenAppInfo()
                onDismiss()
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MenuItem(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
    )
}

private const val SCRIM_ALPHA = 0.6f
