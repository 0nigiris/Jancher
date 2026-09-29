package io.jancher.launcher.home

import androidx.compose.runtime.staticCompositionLocalOf
import io.jancher.launcher.platform.AppLauncher
import io.jancher.launcher.platform.IconCache
import io.jancher.launcher.platform.ShortcutSource

/**
 * Кеш иконок и запуск приложений нужны почти в каждом элементе списка.
 * Прокидывать их параметрами через десяток composable-функций — шум,
 * который ничего не даёт: реализация в приложении ровно одна.
 */
val LocalIconCache = staticCompositionLocalOf<IconCache> {
    error("IconCache не предоставлен")
}

val LocalAppLauncher = staticCompositionLocalOf<AppLauncher> {
    error("AppLauncher не предоставлен")
}

val LocalShortcutSource = staticCompositionLocalOf<ShortcutSource> {
    error("ShortcutSource не предоставлен")
}
