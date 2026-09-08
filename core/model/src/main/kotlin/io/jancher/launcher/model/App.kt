package io.jancher.launcher.model

/**
 * Установленное приложение вместе с тем, что о нём решил пользователь.
 *
 * Место в лаунчере здесь не хранится: одно приложение может лежать
 * в нескольких группах, поэтому связь «приложение — место» принадлежит
 * группе, а не приложению.
 */
data class App(
    val key: ComponentKey,
    val label: String,
    val visibility: Visibility = Visibility.VISIBLE,
    val customLabel: String? = null,
    val categoryHint: Int? = null,
    val firstInstallTime: Long = 0L,
    val isSystem: Boolean = false,
) {
    /** Имя, которое видит пользователь: своё, если задано. */
    val displayLabel: String get() = customLabel ?: label
}
