package io.jancher.launcher.model

/**
 * Группа — контейнер и одновременно единственный способ что-либо показать
 * на главном экране. Избранное — это тоже группа, просто с ролью FAVORITES:
 * поэтому у избранного нет лимита и их может быть несколько.
 *
 * Подгруппы — та же сущность с [parentId], а не отдельный тип. Глубину
 * ограничивает UI, а не схема: иначе каждая операция писалась бы дважды.
 */
data class Group(
    val id: Long,
    val title: String,
    val position: Int,
    val parentId: Long? = null,
    val role: GroupRole = GroupRole.NORMAL,
    val layout: GroupLayout = GroupLayout.LIST_COMPACT,
    val sort: GroupSort = GroupSort.ALPHA,
    val rule: GroupRule? = null,
    val showOnRail: Boolean = true,
    val hidden: Boolean = false,
)

enum class GroupRole {
    NORMAL,
    FAVORITES,
    ALL_APPS,
    ARCHIVE,
}

enum class GroupLayout {
    /** Плотный список: имя приложения, маленькая иконка. */
    LIST_COMPACT,

    /** Просторный список с крупной иконкой. */
    LIST_COMFORT,

    /** Сетка иконок. */
    GRID,

    /** Крупные кнопки в духе One UI. */
    LARGE_BUTTONS,
}

enum class GroupSort {
    /** Порядок задан вручную перетаскиванием. */
    MANUAL,
    ALPHA,

    /** По частоте запусков — требует разрешения на статистику использования. */
    USAGE,

    /** По времени установки, новые сверху. */
    RECENT,
}
