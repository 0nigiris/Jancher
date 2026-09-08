package io.jancher.launcher

import android.content.Context
import android.util.TypedValue
import io.jancher.launcher.data.ConfigStore
import io.jancher.launcher.data.LauncherRepository
import io.jancher.launcher.platform.AppLauncher
import io.jancher.launcher.platform.IconCache
import io.jancher.launcher.platform.InstalledAppsSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Ручная сборка зависимостей.
 *
 * Hilt здесь не окупается: граф маленький, реализация у каждого сервиса одна,
 * а кодогенерация стоит времени сборки и заметного куска времени старта —
 * ровно того ресурса, который у лаунчера в дефиците.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val iconCache = IconCache(appContext, iconSizePx = dp(ICON_SIZE_DP))

    val appLauncher = AppLauncher(appContext)

    private val configStore = ConfigStore(appContext, scope)

    val repository = LauncherRepository(
        source = InstalledAppsSource(appContext),
        store = configStore,
    )

    private fun dp(value: Float): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            appContext.resources.displayMetrics,
        ).toInt()

    private companion object {
        // Иконки растеризуются с запасом: 40dp в списке, 48 — чтобы тот же
        // кеш годился для сетки и крупных кнопок без повторной загрузки.
        const val ICON_SIZE_DP = 48f
    }
}
