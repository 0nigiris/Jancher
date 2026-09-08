package io.jancher.launcher.data

/**
 * Перевод текста между раскладками ЙЦУКЕН и QWERTY по позиции клавиши.
 *
 * Нужен потому, что переключать раскладку ради поиска никто не будет:
 * набрано «ntktuhfv» — значит искали «telegram». Обратное тоже верно:
 * «ЫЗЩЕШАН» на английской раскладке — это «spotify».
 */
internal object KeyboardLayout {

    private const val RU = "йцукенгшщзхъфывапролджэячсмитьбю."
    private const val EN = "qwertyuiop[]asdfghjkl;'zxcvbnm,./"

    private val ruToEn: Map<Char, Char> = RU.toList().zip(EN.toList()).toMap()
    private val enToRu: Map<Char, Char> = EN.toList().zip(RU.toList()).toMap()

    /** Возвращает варианты прочтения запроса, включая исходный. */
    fun variants(query: String): List<String> {
        val lower = query.lowercase()
        return buildList {
            add(lower)
            convert(lower, ruToEn)?.let(::add)
            convert(lower, enToRu)?.let(::add)
        }
    }

    private fun convert(text: String, table: Map<Char, Char>): String? {
        var changed = false
        val result = buildString(text.length) {
            for (ch in text) {
                val mapped = table[ch]
                if (mapped != null) {
                    changed = true
                    append(mapped)
                } else {
                    append(ch)
                }
            }
        }
        return result.takeIf { changed }
    }
}
