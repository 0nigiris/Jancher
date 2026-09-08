package io.jancher.launcher.data

import io.jancher.launcher.model.App

/**
 * Поиск по приложениям.
 *
 * Ранжирование важнее полноты: пользователь набирает одну-две буквы и ждёт,
 * что нужное окажется первым. Поэтому совпадение в начале имени всегда
 * весомее совпадения в середине, а инициалы («вк», «gp») — весомее подстроки.
 */
object AppMatcher {

    fun search(apps: List<App>, query: String, limit: Int = 24): List<App> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        val variants = KeyboardLayout.variants(trimmed)

        return apps.asSequence()
            .mapNotNull { app ->
                val score = variants.maxOf { score(app.displayLabel, it) }
                if (score > 0) app to score else null
            }
            .sortedWith(
                compareByDescending<Pair<App, Int>> { it.second }
                    .thenBy { it.first.displayLabel.length }
                    .thenBy { it.first.displayLabel.lowercase() },
            )
            .take(limit)
            .map { it.first }
            .toList()
    }

    private fun score(label: String, query: String): Int {
        val lower = label.lowercase()

        if (lower.startsWith(query)) return SCORE_PREFIX
        if (initials(lower).startsWith(query)) return SCORE_INITIALS

        // Совпадение с началом любого слова: «карт» находит «Google Карты».
        var index = lower.indexOf(query)
        while (index > 0) {
            if (!lower[index - 1].isLetterOrDigit()) return SCORE_WORD_PREFIX
            index = lower.indexOf(query, index + 1)
        }

        return if (lower.contains(query)) SCORE_SUBSTRING else 0
    }

    /** «Google Play Маркет» → «gpм»: то, как приложение сокращают в голове. */
    private fun initials(lower: String): String =
        lower.split(' ', '-', '_', '.')
            .mapNotNull { it.firstOrNull() }
            .joinToString("")

    private const val SCORE_PREFIX = 1000
    private const val SCORE_INITIALS = 800
    private const val SCORE_WORD_PREFIX = 600
    private const val SCORE_SUBSTRING = 300
}
