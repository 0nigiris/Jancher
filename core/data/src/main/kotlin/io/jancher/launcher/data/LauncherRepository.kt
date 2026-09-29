package io.jancher.launcher.data

import io.jancher.launcher.model.App
import io.jancher.launcher.model.ComponentKey
import io.jancher.launcher.model.Group
import io.jancher.launcher.model.GroupMember
import io.jancher.launcher.model.GroupRole
import io.jancher.launcher.model.MemberKind
import io.jancher.launcher.model.Visibility
import io.jancher.launcher.platform.InstalledAppsSource
import io.jancher.launcher.platform.LauncherApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import java.util.Locale

/** Состояние главного экрана: приложения, уже разложенные по группам. */
data class LauncherState(
    val groups: List<ComposedGroup>,
    val allApps: List<App>,
    val settings: Settings = Settings(),
)

/**
 * Соединяет то, что установлено (система), с тем, что решил пользователь
 * (конфигурация). Ниже этого слоя UI не спускается.
 */
class LauncherRepository(
    private val source: InstalledAppsSource,
    private val store: ConfigStore,
) {

    val state: Flow<LauncherState> =
        combine(source.observe(), store.config) { installed, config ->
            val effective = config.ensureInitialised()
            val apps = installed.toApps(effective.appPrefs)
            LauncherState(
                groups = GroupComposer.compose(apps, effective.toGroups(), effective.toMembers()),
                allApps = apps.sortedBy { it.displayLabel.lowercase() },
                settings = effective.settings,
            )
        }.flowOn(Dispatchers.Default)

    /**
     * Первый запуск: пустая конфигурация превращается в набор групп
     * по алфавиту локали. Делается на лету, а не при установке, чтобы
     * смена языка системы до первого запуска не давала неверный алфавит.
     */
    suspend fun initialiseIfEmpty() {
        store.update { current ->
            if (current.groups.isEmpty()) DefaultConfig.create(Locale.getDefault()) else current
        }
    }

    suspend fun setTrueBlack(enabled: Boolean) {
        store.update { it.copy(settings = it.settings.copy(trueBlack = enabled)) }
    }

    suspend fun toggleFavorite(key: ComponentKey) {
        store.update { config ->
            val favorites = config.groups.firstOrNull { it.role == GroupRole.FAVORITES }
                ?: return@update config
            val existing = config.members.firstOrNull {
                it.groupId == favorites.id && it.key == key.asString() && it.kind == MemberKind.PINNED
            }
            if (existing != null) {
                config.copy(members = config.members - existing)
            } else {
                val position = config.members.count { it.groupId == favorites.id }
                config.copy(
                    members = config.members + MemberConfig(
                        groupId = favorites.id,
                        key = key.asString(),
                        kind = MemberKind.PINNED,
                        position = position,
                    ),
                )
            }
        }
    }

    private fun LauncherConfig.ensureInitialised(): LauncherConfig =
        if (groups.isEmpty()) DefaultConfig.create(Locale.getDefault()) else this

    private fun List<LauncherApp>.toApps(prefs: Map<String, AppPrefs>): List<App> =
        map { raw ->
            val pref = prefs[raw.key.asString()]
            App(
                key = raw.key,
                label = raw.label,
                visibility = pref?.visibility ?: Visibility.VISIBLE,
                customLabel = pref?.customLabel,
                categoryHint = raw.categoryHint,
                firstInstallTime = raw.firstInstallTime,
                isSystem = raw.isSystem,
            )
        }

    private fun LauncherConfig.toGroups(): List<Group> =
        groups.sortedBy { it.position }.map { g ->
            Group(
                id = g.id,
                title = g.title,
                position = g.position,
                parentId = g.parentId,
                role = g.role,
                layout = g.layout,
                sort = g.sort,
                rule = g.rule?.toDomain(),
                showOnRail = g.showOnRail,
                hidden = g.hidden,
            )
        }

    private fun LauncherConfig.toMembers(): List<GroupMember> =
        members.mapNotNull { m ->
            val key = ComponentKey.parse(m.key) ?: return@mapNotNull null
            GroupMember(m.groupId, key, m.kind, m.position)
        }
}
