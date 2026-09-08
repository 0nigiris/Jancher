package io.jancher.launcher.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import io.jancher.launcher.model.ComponentKey

/**
 * Иконка приложения.
 *
 * Из кеша иконка берётся синхронно прямо при первой композиции: попадание
 * в кеш не должно стоить лишнего кадра, иначе при быстром скролле список
 * заметно мерцает. Промах уходит в фон.
 */
@Composable
fun AppIcon(
    key: ComponentKey,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val cache = LocalIconCache.current
    var bitmap by remember(key) { mutableStateOf<ImageBitmap?>(cache.peek(key)?.asImageBitmap()) }

    LaunchedEffect(key) {
        if (bitmap == null) {
            bitmap = cache.load(key)?.asImageBitmap()
        }
    }

    val image = bitmap
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = null,
            modifier = modifier.size(size),
        )
    } else {
        // Пустое место того же размера: без него строка дёргается,
        // когда иконка догружается.
        Box(modifier.size(size))
    }
}
