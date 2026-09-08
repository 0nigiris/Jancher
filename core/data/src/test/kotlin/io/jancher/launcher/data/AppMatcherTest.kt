package io.jancher.launcher.data

import io.jancher.launcher.model.App
import io.jancher.launcher.model.ComponentKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppMatcherTest {

    private fun app(label: String) = App(
        key = ComponentKey(label.lowercase(), "Main", 0),
        label = label,
    )

    private val apps = listOf(
        app("Telegram"),
        app("Termux"),
        app("Google Карты"),
        app("Настройки"),
        app("Spotify"),
        app("VK Видео"),
    )

    @Test
    fun `совпадение в начале имени идёт первым`() {
        val result = AppMatcher.search(apps, "te")
        assertEquals(listOf("Termux", "Telegram"), result.map { it.label })
    }

    @Test
    fun `находит по началу слова внутри имени`() {
        assertEquals(listOf("Google Карты"), AppMatcher.search(apps, "карт").map { it.label })
    }

    @Test
    fun `находит по инициалам`() {
        assertEquals(listOf("VK Видео"), AppMatcher.search(apps, "vв").map { it.label })
    }

    @Test
    fun `латинское имя находится набором в русской раскладке`() {
        // Клавиши t,e,l,e,g,r,a,m в русской раскладке дают «еудупкфь»
        assertEquals(listOf("Telegram"), AppMatcher.search(apps, "еудупкфь").map { it.label })
        // «ызщешан» тем же способом — это «spotify»
        assertEquals(listOf("Spotify"), AppMatcher.search(apps, "ызщешан").map { it.label })
    }

    @Test
    fun `русское имя находится набором в латинской раскладке`() {
        // «yfcnhjqrb» на русской раскладке — «настройки»
        assertEquals(listOf("Настройки"), AppMatcher.search(apps, "yfcnhjqrb").map { it.label })
    }

    @Test
    fun `пустой запрос не даёт результатов`() {
        assertTrue(AppMatcher.search(apps, "   ").isEmpty())
    }
}
