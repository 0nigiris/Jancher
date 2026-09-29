package io.jancher.launcher.platform

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import io.jancher.launcher.model.ComponentKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Shortcut(
    val id: String,
    val packageName: String,
    val label: String,
)

/**
 * Статические и динамические шорткаты приложения — то, что показывается
 * по долгому нажатию: «Новое сообщение», «Сканировать», закреплённые чаты.
 *
 * Доступны только лаунчеру по умолчанию: в остальных случаях система бросает
 * `SecurityException`. Это нормальный сценарий, а не ошибка — пока Jancher
 * не выбран лаунчером, меню просто показывается без шорткатов.
 */
class ShortcutSource(context: Context) {

    private val launcherApps: LauncherApps = context.getSystemService(LauncherApps::class.java)
    private val users = UserResolver(context)

    suspend fun load(key: ComponentKey): List<Shortcut> = withContext(Dispatchers.IO) {
        val user = users.resolve(key) ?: return@withContext emptyList()
        val query = LauncherApps.ShortcutQuery()
            .setPackage(key.packageName)
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
            )

        runCatching { launcherApps.getShortcuts(query, user) }
            .getOrNull()
            .orEmpty()
            .filterNotNull()
            .sortedBy { it.rank }
            .map { it.toShortcut() }
            .distinctBy { it.id }
            .take(MAX_SHORTCUTS)
    }

    fun start(shortcut: Shortcut, key: ComponentKey, bounds: Rect?) {
        val user = users.resolve(key) ?: return
        runCatching {
            launcherApps.startShortcut(shortcut.packageName, shortcut.id, bounds, null, user)
        }
    }

    private fun ShortcutInfo.toShortcut() = Shortcut(
        id = id,
        packageName = `package`,
        label = (longLabel ?: shortLabel ?: id).toString(),
    )

    private companion object {
        /** Больше четырёх в меню превращаются в список, который надо читать. */
        const val MAX_SHORTCUTS = 4
    }
}
