package io.jancher.launcher.home

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Часы в верхней зоне. Позже сюда же встанут виджеты.
 *
 * Обновление привязано к границе минуты, а не к таймеру на 60 секунд:
 * иначе показания «плывут» и минута меняется на пару секунд позже системных,
 * что сразу бросается в глаза.
 */
@Composable
fun Clock(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val locale = Locale.getDefault()

    val timeFormat = remember(locale, DateFormat.is24HourFormat(context)) {
        SimpleDateFormat(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm", locale)
    }
    val dateFormat = remember(locale) { SimpleDateFormat("EEEE, d MMMM", locale) }

    val now by produceState(initialValue = Date()) {
        while (true) {
            value = Date()
            delay(60_000L - System.currentTimeMillis() % 60_000L)
        }
    }

    Column(modifier) {
        Text(
            text = timeFormat.format(now),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = dateFormat.format(now).replaceFirstChar { it.titlecase(locale) },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
