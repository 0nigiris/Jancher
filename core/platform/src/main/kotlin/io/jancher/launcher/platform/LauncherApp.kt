package io.jancher.launcher.platform

import android.os.UserHandle
import io.jancher.launcher.model.ComponentKey

/**
 * Сырые данные о launcher-активити, как их отдаёт система.
 *
 * Намеренно не хранит `LauncherActivityInfo`: тот держит ссылку на ресурсы
 * чужого приложения, и коллекция таких объектов на 250 приложений заметна
 * в памяти. Иконка догружается по ключу, когда действительно нужна.
 */
data class LauncherApp(
    val key: ComponentKey,
    val label: String,
    val user: UserHandle,
    val firstInstallTime: Long,
    val isSystem: Boolean,
    val categoryHint: Int?,
)
