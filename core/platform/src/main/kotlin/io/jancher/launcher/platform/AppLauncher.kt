package io.jancher.launcher.platform

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.view.View
import io.jancher.launcher.model.ComponentKey

/**
 * Запуск приложений и системных экранов, относящихся к приложению.
 */
class AppLauncher(private val context: Context) {

    private val launcherApps: LauncherApps =
        context.getSystemService(LauncherApps::class.java)

    private val users = UserResolver(context)

    /**
     * @param source вью иконки: система разворачивает окно из её границ.
     *   `makeClipRevealAnimation` требует настоящую вью, поэтому без неё
     *   запуск выполняется без анимации, а не падает.
     * @param bounds границы иконки в координатах экрана.
     */
    fun launch(key: ComponentKey, source: View?, bounds: Rect?): Boolean {
        val user = users.resolve(key) ?: return false
        val component = ComponentName(key.packageName, key.className)
        val options = if (source != null && bounds != null) {
            ActivityOptions.makeClipRevealAnimation(
                source, 0, 0, bounds.width(), bounds.height(),
            ).toBundle()
        } else {
            null
        }
        return runCatching {
            launcherApps.startMainActivity(component, user, bounds, options)
        }.isSuccess
    }

    fun openAppInfo(key: ComponentKey, bounds: Rect?) {
        val user = users.resolve(key) ?: return
        runCatching {
            launcherApps.startAppDetailsActivity(
                ComponentName(key.packageName, key.className), user, bounds, null,
            )
        }
    }
}
