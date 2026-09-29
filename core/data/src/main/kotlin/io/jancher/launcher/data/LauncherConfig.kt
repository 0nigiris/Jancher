package io.jancher.launcher.data

import io.jancher.launcher.model.GroupLayout
import io.jancher.launcher.model.GroupRole
import io.jancher.launcher.model.GroupRule
import io.jancher.launcher.model.GroupSort
import io.jancher.launcher.model.MemberKind
import io.jancher.launcher.model.Visibility
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Вся пользовательская конфигурация лаунчера одним документом.
 *
 * Здесь нет списка установленных приложений: он читается из системы при
 * каждом запуске за единицы миллисекунд, а его копия в хранилище — только
 * источник рассинхронизации. Хранится ровно то, чего система не знает:
 * решения пользователя.
 *
 * [version] существует ради миграций: формат будет меняться, а конфигурация
 * пользователя переживать обновления обязана.
 */
@Serializable
data class LauncherConfig(
    val version: Int = CURRENT_VERSION,
    val groups: List<GroupConfig> = emptyList(),
    val members: List<MemberConfig> = emptyList(),
    val appPrefs: Map<String, AppPrefs> = emptyMap(),
    val nextGroupId: Long = 1L,
    val settings: Settings = Settings(),
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

@Serializable
data class Settings(
    /**
     * Чистый чёрный фон вместо тёмно-серого. На AMOLED-экране такие пиксели
     * буквально не светятся, но на LCD это выглядит грязно — поэтому
     * настройка, а не поведение по умолчанию.
     */
    val trueBlack: Boolean = false,
)

@Serializable
data class GroupConfig(
    val id: Long,
    val title: String,
    val position: Int,
    val parentId: Long? = null,
    val role: GroupRole = GroupRole.NORMAL,
    val layout: GroupLayout = GroupLayout.LIST_COMPACT,
    val sort: GroupSort = GroupSort.ALPHA,
    val rule: RuleConfig? = null,
    val showOnRail: Boolean = true,
    val hidden: Boolean = false,
)

/** Ручная правка состава поверх правила. Ключ приложения — плоская строка. */
@Serializable
data class MemberConfig(
    val groupId: Long,
    val key: String,
    val kind: MemberKind,
    val position: Int,
)

@Serializable
data class AppPrefs(
    val visibility: Visibility = Visibility.VISIBLE,
    val customLabel: String? = null,
    val archivedAt: Long? = null,
)

/**
 * Сериализуемое зеркало [GroupRule]. Доменный тип намеренно не помечен
 * `@Serializable`: формат хранения — это отдельное решение, и модель не должна
 * меняться из-за того, что поменялся способ записи на диск.
 */
@Serializable
sealed interface RuleConfig {

    @Serializable
    @SerialName("alpha")
    data class Alpha(val from: Char, val to: Char) : RuleConfig

    @Serializable
    @SerialName("category")
    data class Category(val value: Int) : RuleConfig

    @Serializable
    @SerialName("installed_after")
    data class InstalledAfter(val timestamp: Long) : RuleConfig

    @Serializable
    @SerialName("unassigned")
    data object Unassigned : RuleConfig

    @Serializable
    @SerialName("all")
    data object AllApps : RuleConfig
}

fun RuleConfig.toDomain(): GroupRule = when (this) {
    is RuleConfig.Alpha -> GroupRule.AlphaRange(from, to)
    is RuleConfig.Category -> GroupRule.Category(value)
    is RuleConfig.InstalledAfter -> GroupRule.InstalledAfter(timestamp)
    RuleConfig.Unassigned -> GroupRule.Unassigned
    RuleConfig.AllApps -> GroupRule.AllApps
}
