package io.jancher.launcher.data

import io.jancher.launcher.model.App
import io.jancher.launcher.model.ComponentKey
import io.jancher.launcher.model.Group
import io.jancher.launcher.model.GroupMember
import io.jancher.launcher.model.GroupRole
import io.jancher.launcher.model.GroupRule
import io.jancher.launcher.model.GroupSort
import io.jancher.launcher.model.MemberKind
import io.jancher.launcher.model.Visibility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupComposerTest {

    private fun app(label: String, visibility: Visibility = Visibility.VISIBLE) = App(
        key = ComponentKey(label.lowercase(), "Main", 0),
        label = label,
        visibility = visibility,
    )

    private fun group(
        id: Long,
        title: String,
        rule: GroupRule?,
        role: GroupRole = GroupRole.NORMAL,
        sort: GroupSort = GroupSort.ALPHA,
    ) = Group(id = id, title = title, position = id.toInt(), rule = rule, role = role, sort = sort)

    private val chrome = app("Chrome")
    private val calendar = app("Calendar")
    private val telegram = app("Telegram")
    private val apps = listOf(chrome, calendar, telegram)

    @Test
    fun `правило раскладывает приложения по алфавитным группам`() {
        val groups = listOf(
            group(1, "A–D", GroupRule.AlphaRange('A', 'D')),
            group(2, "S–Z", GroupRule.AlphaRange('S', 'Z')),
        )
        val result = GroupComposer.compose(apps, groups, emptyList())

        assertEquals(listOf("Calendar", "Chrome"), result[0].apps.map { it.label })
        assertEquals(listOf("Telegram"), result[1].apps.map { it.label })
    }

    @Test
    fun `закреплённое вручную попадает в группу вопреки правилу`() {
        val groups = listOf(group(1, "A–D", GroupRule.AlphaRange('A', 'D')))
        val members = listOf(GroupMember(1, telegram.key, MemberKind.PINNED, 0))

        val result = GroupComposer.compose(apps, groups, members)

        assertTrue(result[0].apps.any { it.label == "Telegram" })
    }

    @Test
    fun `исключённое вручную пропадает вопреки правилу`() {
        val groups = listOf(group(1, "A–D", GroupRule.AlphaRange('A', 'D')))
        val members = listOf(GroupMember(1, chrome.key, MemberKind.EXCLUDED, 0))

        val result = GroupComposer.compose(apps, groups, members)

        assertEquals(listOf("Calendar"), result[0].apps.map { it.label })
    }

    @Test
    fun `Прочее собирает всё, что не разобрали другие группы`() {
        val groups = listOf(
            group(1, "A–D", GroupRule.AlphaRange('A', 'D')),
            group(2, "Прочее", GroupRule.Unassigned),
        )
        val result = GroupComposer.compose(apps, groups, emptyList())

        assertEquals(listOf("Telegram"), result[1].apps.map { it.label })
    }

    @Test
    fun `избранное не мешает приложению остаться в своей букве`() {
        val groups = listOf(
            group(1, "Избранное", null, role = GroupRole.FAVORITES, sort = GroupSort.MANUAL),
            group(2, "S–Z", GroupRule.AlphaRange('S', 'Z')),
            group(3, "Прочее", GroupRule.Unassigned),
        )
        val members = listOf(GroupMember(1, telegram.key, MemberKind.PINNED, 0))

        val result = GroupComposer.compose(apps, groups, members)

        assertEquals(listOf("Telegram"), result[0].apps.map { it.label })
        // Одно приложение живёт в нескольких местах одновременно — это
        // и есть заявленное разделение приложения и его места в лаунчере.
        assertEquals(listOf("Telegram"), result[1].apps.map { it.label })
        // И при этом «Прочее» не считает его неразобранным.
        assertTrue(result[2].apps.none { it.label == "Telegram" })
    }

    @Test
    fun `скрытые и архивные не показываются в группах`() {
        val hidden = app("Zoom", Visibility.HIDDEN)
        val archived = app("Zello", Visibility.ARCHIVED)
        val groups = listOf(group(1, "S–Z", GroupRule.AlphaRange('S', 'Z')))

        val result = GroupComposer.compose(apps + hidden + archived, groups, emptyList())

        assertEquals(listOf("Telegram"), result[0].apps.map { it.label })
    }

    @Test
    fun `ручная сортировка ставит закреплённые в заданном порядке`() {
        val groups = listOf(
            group(1, "Избранное", null, role = GroupRole.FAVORITES, sort = GroupSort.MANUAL),
        )
        val members = listOf(
            GroupMember(1, telegram.key, MemberKind.PINNED, 0),
            GroupMember(1, calendar.key, MemberKind.PINNED, 1),
            GroupMember(1, chrome.key, MemberKind.PINNED, 2),
        )

        val result = GroupComposer.compose(apps, groups, members)

        assertEquals(listOf("Telegram", "Calendar", "Chrome"), result[0].apps.map { it.label })
    }
}
