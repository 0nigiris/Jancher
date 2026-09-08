package io.jancher.launcher.platform

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import io.jancher.launcher.model.ComponentKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Кеш иконок в памяти.
 *
 * Считать иконку у чужого приложения дорого: это чтение ресурсов другого APK
 * и растеризация. При скролле списка на 250 приложений без кеша кадры
 * гарантированно проседают.
 *
 * Дисковый уровень появится, когда станет видно время холодного старта:
 * пока список приложений всё равно читается заново, экономить нечего.
 */
class IconCache(
    context: Context,
    private val iconSizePx: Int,
) {
    private val users = UserResolver(context)

    private val launcherApps: LauncherApps =
        context.getSystemService(LauncherApps::class.java)

    private val density: Int = context.resources.displayMetrics.densityDpi

    // Ограничение по памяти, а не по числу элементов: иконки разного размера
    // на разных плотностях отличаются в разы.
    private val memory = object : LruCache<String, Bitmap>(MEMORY_BUDGET_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    fun peek(key: ComponentKey): Bitmap? = memory.get(key.asString())

    suspend fun load(key: ComponentKey): Bitmap? {
        memory.get(key.asString())?.let { return it }
        val user = users.resolve(key) ?: return null

        val bitmap = withContext(Dispatchers.IO) {
            val info = runCatching {
                launcherApps.getActivityList(key.packageName, user)
                    .firstOrNull { it.componentName.className == key.className }
            }.getOrNull() ?: return@withContext null

            // getIcon сам накладывает бейдж рабочего профиля и системную маску
            // для adaptive-иконок — руками этого делать не нужно.
            runCatching {
                info.getIcon(density).toBitmap(iconSizePx, iconSizePx)
            }.getOrNull()
        }

        if (bitmap != null) memory.put(key.asString(), bitmap)
        return bitmap
    }

    fun clear() = memory.evictAll()

    private companion object {
        const val MEMORY_BUDGET_BYTES = 24 * 1024 * 1024
    }
}
