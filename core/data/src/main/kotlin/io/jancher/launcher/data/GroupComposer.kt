package io.jancher.launcher.data

import io.jancher.launcher.model.App
import io.jancher.launcher.model.ComponentKey
import io.jancher.launcher.model.Group
import io.jancher.launcher.model.GroupRole
import io.jancher.launcher.model.GroupRule
import io.jancher.launcher.model.GroupMember
import io.jancher.launcher.model.GroupSort
import io.jancher.launcher.model.MemberKind
import io.jancher.launcher.model.Visibility

/** Группа вместе с вычисленным составом. */
data class ComposedGroup(
    val group: Group,
    val apps: List<App>,
)

/**
 * Считает, что лежит в каждой группе:
 *
 *     состав(g) = ( правило(g) ∪ PINNED(g) ) \ EXCLUDED(g) \ невидимые
 *
 * Смысл именно в этом порядке: новое приложение попадает в свою алфавитную
 * группу само, но любое ручное решение пользователя правило перебивает.
 * Пользователь никогда не должен обнаружить, что установка приложения
 * переставила что-то, что он расставил руками.
 */
object GroupComposer {

    fun compose(apps: List<App>, groups: List<Group>, members: List<GroupMember>): List<ComposedGroup> {
        val byKey = apps.associateBy { it.key }
        val pinned = members.filter { it.kind == MemberKind.PINNED }.groupBy { it.groupId }
        val excluded = members.filter { it.kind == MemberKind.EXCLUDED }
            .groupBy({ it.groupId }, { it.key })
            .mapValues { it.value.toHashSet() }

        // «Прочее» считается последним: ему нужно знать, что разобрали остальные.
        val assigned = HashSet<ComponentKey>()
        val ruleGroups = groups.filter { it.role == GroupRole.NORMAL && it.rule != GroupRule.Unassigned }
        for (group in ruleGroups) {
            val rule = group.rule ?: continue
            apps.forEach { if (rule.matches(it)) assigned += it.key }
            pinned[group.id]?.forEach { assigned += it.key }
        }

        return groups.map { group ->
            val fromRule = when (val rule = group.rule) {
                null -> emptyList()
                GroupRule.Unassigned -> apps.filter { it.key !in assigned }
                else -> apps.filter { rule.matches(it) }
            }

            val pinnedApps = pinned[group.id].orEmpty()
                .sortedBy { it.position }
                .mapNotNull { byKey[it.key] }

            val excludedKeys = excluded[group.id].orEmpty()

            val visible = (pinnedApps + fromRule)
                .distinctBy { it.key }
                .filter { it.key !in excludedKeys && it.visibility == Visibility.VISIBLE }

            ComposedGroup(group, visible.sortedFor(group.sort, pinnedApps))
        }
    }

    private fun List<App>.sortedFor(sort: GroupSort, manualOrder: List<App>): List<App> =
        when (sort) {
            GroupSort.ALPHA -> sortedBy { it.displayLabel.lowercase() }
            GroupSort.RECENT -> sortedByDescending { it.firstInstallTime }
            GroupSort.MANUAL -> {
                // Порядок задают закреплённые элементы; всё, что пришло
                // из правила и порядка не имеет, уходит вниз по алфавиту.
                val order = manualOrder.withIndex().associate { (i, app) -> app.key to i }
                sortedWith(
                    compareBy(
                        { order[it.key] ?: Int.MAX_VALUE },
                        { it.displayLabel.lowercase() },
                    ),
                )
            }
            // Частота использования требует разрешения на статистику;
            // до его получения ведём себя как алфавит, а не падаем.
            GroupSort.USAGE -> sortedBy { it.displayLabel.lowercase() }
        }

    private fun GroupRule.matches(app: App): Boolean = when (this) {
        is GroupRule.AlphaRange -> {
            val first = app.displayLabel.firstOrNull()?.uppercaseChar()
            first != null && first in from..to
        }
        is GroupRule.Category -> app.categoryHint == playCategory
        is GroupRule.InstalledAfter -> app.firstInstallTime >= timestamp
        GroupRule.AllApps -> true
        GroupRule.Unassigned -> false
        is GroupRule.AnyOf -> rules.any { it.matches(app) }
        is GroupRule.AllOf -> rules.all { it.matches(app) }
        is GroupRule.Not -> !rule.matches(app)
    }
}
