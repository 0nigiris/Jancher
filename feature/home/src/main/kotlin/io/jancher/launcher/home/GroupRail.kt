package io.jancher.launcher.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Вертикальный рельс справа: ведёшь пальцем — выбираешь группу.
 *
 * Рельс намеренно **не** прокручивает список во время ведения. С аккордеоном
 * это единственный работающий вариант: раскрытие группы меняет высоты, и если
 * список едет под пальцем, попасть в нужную группу невозможно. Поэтому во
 * время ведения меняется только всплывающая подпись, а список перестраивается
 * один раз — когда палец отпущен.
 *
 * Рельс отступает от края экрана: правый край принадлежит системному жесту
 * «назад», и отбирать его у системы нельзя — [setSystemGestureExclusionRects]
 * ограничен по высоте и всё равно не решил бы проблему.
 */
@Composable
fun GroupRail(
    labels: List<String>,
    onPreview: (Int?) -> Unit,
    onCommit: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (labels.isEmpty()) return

    val haptics = LocalHapticFeedback.current
    var height by remember { mutableStateOf(0) }
    var active by remember { mutableStateOf<Int?>(null) }

    fun indexAt(y: Float): Int {
        if (height <= 0) return 0
        val step = height.toFloat() / labels.size
        return (y / step).toInt().coerceIn(0, labels.size - 1)
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(RAIL_WIDTH)
            .pointerInput(labels.size) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitPointerEvent().changes.firstOrNull { it.pressed }
                            ?: continue
                        var current = indexAt(down.position.y)
                        active = current
                        onPreview(current)
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                onCommit(current)
                                active = null
                                onPreview(null)
                                change.consume()
                                break
                            }
                            val next = indexAt(change.position.y)
                            if (next != current) {
                                current = next
                                active = next
                                onPreview(next)
                                // Отсечка на каждой группе: пользователь
                                // понимает, где находится, не глядя на экран.
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            change.consume()
                        }
                    }
                }
            }
            .onSizeChanged { height = it.height },
        verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        labels.forEachIndexed { index, label ->
            Text(
                text = label.take(RAIL_LABEL_CHARS),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = if (index == active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(vertical = 3.dp),
            )
        }
    }
}

/** Всплывающая подпись выбранной группы — то, что заменяет прокрутку списка. */
@Composable
fun RailPreviewPill(
    label: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 20.dp, vertical = 10.dp),
    )
}

private val RAIL_WIDTH = 28.dp
private const val RAIL_LABEL_CHARS = 3
