package io.jancher.launcher.model

/**
 * Правило автоматического наполнения группы.
 *
 * Состав группы считается так:
 *
 *     состав(g) = ( правило(g) ∪ PINNED(g) ) \ EXCLUDED(g) \ невидимые
 *
 * Это даёт главное свойство: новое приложение попадает в свою алфавитную
 * группу само, но любое ручное решение пользователя правило перебивает
 * и переживает переустановку приложения.
 */
sealed interface GroupRule {

    /** Алфавитный диапазон по первой букве имени, включительно. */
    data class AlphaRange(val from: Char, val to: Char) : GroupRule

    /**
     * Категория из `ApplicationInfo.category`. Данные заполняет разработчик
     * приложения, поэтому у заметной части приложений там `-1` или мусор.
     * Годится как подсказка при создании группы, но не как единственный
     * источник истины.
     */
    data class Category(val playCategory: Int) : GroupRule

    /** Установлено после указанного момента — для группы «Новое». */
    data class InstalledAfter(val timestamp: Long) : GroupRule

    /** Всё, что не попало ни в одну другую группу. Страховка от потери приложений. */
    data object Unassigned : GroupRule

    data object AllApps : GroupRule

    data class AnyOf(val rules: List<GroupRule>) : GroupRule
    data class AllOf(val rules: List<GroupRule>) : GroupRule
    data class Not(val rule: GroupRule) : GroupRule
}
