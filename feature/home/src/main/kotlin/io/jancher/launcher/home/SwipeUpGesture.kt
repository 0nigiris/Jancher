package io.jancher.launcher.home

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Свайп вверх от нижнего края экрана.
 *
 * События наблюдаются в фазе Initial и **не** перехватываются, пока жест
 * не опознан: иначе нижние строки списка перестали бы нажиматься. Как только
 * движение вверх превысило порог — жест забирается себе, и список не начинает
 * скроллиться вдогонку.
 */
fun Modifier.swipeUpFromBottom(enabled: Boolean = true, onTriggered: () -> Unit): Modifier = composed {
    val density = LocalDensity.current
    val zoneHeightPx = with(density) { TRIGGER_ZONE.toPx() }
    val thresholdPx = with(density) { TRIGGER_DISTANCE.toPx() }

    if (!enabled) return@composed Modifier

    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val down = awaitPointerEvent(PointerEventPass.Initial)
                    .changes.firstOrNull { it.pressed } ?: continue

                val startedAtBottom = down.position.y > size.height - zoneHeightPx
                if (!startedAtBottom) {
                    // Ждём, пока палец отпустят, чтобы не разбирать этот жест.
                    while (awaitPointerEvent(PointerEventPass.Initial)
                            .changes.any { it.pressed }
                    ) Unit
                    continue
                }

                val startY = down.position.y
                var triggered = false

                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull() ?: break
                    if (!change.pressed) break

                    if (!triggered && startY - change.position.y > thresholdPx) {
                        triggered = true
                        change.consume()
                        onTriggered()
                    }
                }
            }
        }
    }
}

/** Полоса у нижнего края, с которой начинается жест. */
private val TRIGGER_ZONE = 120.dp

/** Насколько нужно потянуть вверх, чтобы это считалось намерением, а не дрожью. */
private val TRIGGER_DISTANCE = 48.dp
