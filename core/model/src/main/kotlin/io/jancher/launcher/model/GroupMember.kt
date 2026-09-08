package io.jancher.launcher.model

/**
 * Ручная правка состава группы поверх правила.
 * Одно приложение может быть закреплено в нескольких группах одновременно —
 * ограничения «одно место на приложение» в модели нет намеренно.
 */
data class GroupMember(
    val groupId: Long,
    val key: ComponentKey,
    val kind: MemberKind,
    val position: Int,
)

enum class MemberKind {
    /** Добавлено вручную, даже если правило его не выбирает. */
    PINNED,

    /** Исключено вручную, даже если правило его выбирает. */
    EXCLUDED,
}
