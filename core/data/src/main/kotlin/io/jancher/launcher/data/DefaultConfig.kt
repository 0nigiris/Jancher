package io.jancher.launcher.data

import io.jancher.launcher.model.GroupLayout
import io.jancher.launcher.model.GroupRole
import io.jancher.launcher.model.GroupSort
import java.util.Locale

/**
 * Конфигурация первого запуска.
 *
 * Алфавитные диапазоны строятся по алфавиту локали устройства: жёстко зашитое
 * A–Z на русском телефоне отправляет весь список в «Прочее». Порядок групп —
 * латиница, затем алфавит локали, затем «Прочее».
 */
object DefaultConfig {

    private const val LATIN = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val CYRILLIC = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"

    fun create(locale: Locale = Locale.getDefault()): LauncherConfig {
        var id = 1L
        var position = 0
        val groups = mutableListOf<GroupConfig>()

        groups += GroupConfig(
            id = id++,
            title = "Избранное",
            position = position++,
            role = GroupRole.FAVORITES,
            layout = GroupLayout.LIST_COMFORT,
            sort = GroupSort.MANUAL,
            rule = null,
        )

        val alphabets = buildList {
            add(LATIN)
            if (locale.language == "ru") add(CYRILLIC)
        }

        for (alphabet in alphabets) {
            for (range in alphabet.chunkedRanges(RANGE_SIZE)) {
                groups += GroupConfig(
                    id = id++,
                    title = if (range.first == range.last) {
                        range.first.toString()
                    } else {
                        "${range.first}–${range.last}"
                    },
                    position = position++,
                    rule = RuleConfig.Alpha(range.first, range.last),
                )
            }
        }

        groups += GroupConfig(
            id = id++,
            title = "Прочее",
            position = position++,
            rule = RuleConfig.Unassigned,
        )

        return LauncherConfig(groups = groups, nextGroupId = id)
    }

    private const val RANGE_SIZE = 4

    private fun String.chunkedRanges(size: Int): List<CharRange> =
        chunked(size).map { it.first()..it.last() }
}
