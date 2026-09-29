package io.jancher.launcher.updater

import kotlinx.serialization.Serializable

/**
 * Манифест обновления — небольшой JSON, лежащий в репозитории.
 *
 * Отдельный файл вместо разбора GitHub Releases API: формат целиком наш,
 * не зависит от изменений чужого API и позволяет объявить контрольную сумму,
 * которой в релизах нет.
 */
@Serializable
data class UpdateManifest(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    /** SHA-256 файла в hex. Без него установка не начинается. */
    val sha256: String,
    val sizeBytes: Long = 0,
    val notes: String = "",
    /**
     * Минимальная версия, с которой можно обновиться напрямую.
     * Пригодится, если когда-нибудь сломается формат конфигурации.
     */
    val minVersionCode: Int = 1,
)
