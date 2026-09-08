package io.jancher.launcher.platform

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import io.jancher.launcher.model.ComponentKey
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/**
 * Единственный источник правды о том, что установлено на устройстве.
 *
 * `LauncherApps` работает без `QUERY_ALL_PACKAGES`: система отдаёт лаунчеру
 * список launcher-активити всех профилей, к которым у него есть доступ.
 * Это снимает главную причину отклонения лаунчеров в Play.
 */
class InstalledAppsSource(private val context: Context) {

    private val launcherApps: LauncherApps =
        context.getSystemService(LauncherApps::class.java)

    private val userManager: UserManager =
        context.getSystemService(UserManager::class.java)

    /** Полное чтение списка. Вызывающий обязан делать это вне главного потока. */
    fun loadAll(): List<LauncherApp> =
        userManager.userProfiles.flatMap { user ->
            runCatching { launcherApps.getActivityList(null, user) }
                .getOrDefault(emptyList())
                .map { it.toLauncherApp(user) }
        }

    /**
     * Список приложений, обновляющийся при установке, удалении и обновлении.
     *
     * Инкрементально пересчитывать дешевле, но пересборка всего списка на
     * 250 приложениях занимает единицы миллисекунд и не имеет ни одного
     * из классов ошибок рассинхронизации. Оптимизировать это стоит только
     * если замеры покажут проблему.
     */
    fun observe(): Flow<List<LauncherApp>> = callbackFlow {
        val callback = object : LauncherApps.Callback() {
            private fun reload() {
                trySend(loadAll())
            }

            override fun onPackageAdded(packageName: String, user: UserHandle) = reload()
            override fun onPackageRemoved(packageName: String, user: UserHandle) = reload()
            override fun onPackageChanged(packageName: String, user: UserHandle) = reload()

            override fun onPackagesAvailable(
                packageNames: Array<out String>,
                user: UserHandle,
                replacing: Boolean,
            ) = reload()

            override fun onPackagesUnavailable(
                packageNames: Array<out String>,
                user: UserHandle,
                replacing: Boolean,
            ) = reload()
        }

        send(loadAll())
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        awaitClose { launcherApps.unregisterCallback(callback) }
    }.conflate()

    private fun LauncherActivityInfo.toLauncherApp(user: UserHandle): LauncherApp {
        val serial = userManager.getSerialNumberForUser(user)
        return LauncherApp(
            key = ComponentKey(
                packageName = componentName.packageName,
                className = componentName.className,
                userSerial = serial.toInt(),
            ),
            label = label.toString(),
            user = user,
            firstInstallTime = firstInstallTime,
            isSystem = applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
            // -1 означает «разработчик не указал категорию», а не «категория -1»
            categoryHint = applicationInfo.category.takeIf { it >= 0 },
        )
    }
}
