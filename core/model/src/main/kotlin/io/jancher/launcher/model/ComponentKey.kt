package io.jancher.launcher.model

/**
 * Идентичность запускаемой сущности.
 *
 * Имя пакета для этого не годится: у части приложений несколько launcher-активити
 * (настройки, Xiaomi-подобные оболочки), а рабочий профиль даёт то же приложение
 * под другим пользователем. Ключ включает всё три составляющие, поэтому настройки
 * переживают переустановку приложения и не путают клона из рабочего профиля
 * с личной копией.
 */
data class ComponentKey(
    val packageName: String,
    val className: String,
    val userSerial: Int,
) {
    /** Плоское представление для первичного ключа в Room и для экспорта. */
    fun asString(): String = "$packageName/$className#$userSerial"

    override fun toString(): String = asString()

    companion object {
        fun parse(raw: String): ComponentKey? {
            val hash = raw.lastIndexOf('#')
            if (hash <= 0) return null
            val slash = raw.lastIndexOf('/', startIndex = hash)
            if (slash <= 0) return null
            val user = raw.substring(hash + 1).toIntOrNull() ?: return null
            return ComponentKey(
                packageName = raw.substring(0, slash),
                className = raw.substring(slash + 1, hash),
                userSerial = user,
            )
        }
    }
}
