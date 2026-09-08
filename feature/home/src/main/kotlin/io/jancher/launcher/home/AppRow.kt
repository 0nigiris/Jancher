package io.jancher.launcher.home

import android.graphics.Rect
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.jancher.launcher.model.App

/**
 * Строка приложения в списке.
 *
 * Границы строки запоминаются, потому что системе нужен прямоугольник
 * источника: без него окно приложения разворачивается из центра экрана,
 * и запуск ощущается оторванным от нажатия.
 */
@Composable
fun AppRow(
    app: App,
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 40.dp,
    onLongClick: (App) -> Unit = {},
) {
    val launcher = LocalAppLauncher.current
    val view = LocalView.current
    var coordinates: LayoutCoordinates? = null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates = it }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    val bounds = coordinates?.boundsInWindow()?.let {
                        Rect(it.left.toInt(), it.top.toInt(), it.right.toInt(), it.bottom.toInt())
                    }
                    launcher.launch(app.key, view, bounds)
                },
                onLongClick = { onLongClick(app) },
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AppIcon(key = app.key, size = iconSize)
        Text(
            text = app.displayLabel,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(220.dp),
        )
    }
}
