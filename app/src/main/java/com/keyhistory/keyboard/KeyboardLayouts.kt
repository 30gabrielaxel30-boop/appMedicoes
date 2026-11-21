package com.keyhistory.keyboard

enum class KeyKind { CHAR, DEAD, SHIFT, BACKSPACE, ENTER, SPACE, TO_SYMBOLS, TO_SYMBOLS2, TO_LETTERS }

data class Key(
    val label: String,
    val kind: KeyKind = KeyKind.CHAR,
    val weight: Float = 1f,
    val variants: List<String> = emptyList(),
)

enum class Layer { LETTERS, SYMBOLS, SYMBOLS2 }

/** Layout compatível com ABNT2 (português do Brasil), com tecla Ç dedicada. */
object KeyboardLayouts {

    private val variantsMap: Map<String, List<String>> = mapOf(
        "q" to listOf("1"), "w" to listOf("2"),
        "e" to listOf("3", "é", "ê", "è", "ë", "€"),
        "r" to listOf("4"), "t" to listOf("5"), "y" to listOf("6", "ý"),
        "u" to listOf("7", "ú", "ü", "ù", "û"),
        "i" to listOf("8", "í", "î", "ì", "ï"),
        "o" to listOf("9", "ó", "ô", "õ", "ò", "ö", "º"),
        "p" to listOf("0", "§"),
        "a" to listOf("á", "ã", "â", "à", "ä", "ª"),
        "s" to listOf("ß", "$"),
        "c" to listOf("ç", "©"),
        "n" to listOf("ñ"),
        "." to listOf(",", "?", "!", ";", ":", "…", "'", "\""),
        "," to listOf(";", ":", "-", "_"),
    )

    private fun chars(s: String) = s.map { c -> Key(c.toString(), variants = variantsMap[c.toString()].orEmpty()) }

    private fun bottomRow(modeKey: Key) = listOf(
        modeKey,
        Key(",", variants = variantsMap[","].orEmpty()),
        Key("espaço", KeyKind.SPACE, 5f),
        Key(".", variants = variantsMap["."].orEmpty()),
        Key("enter", KeyKind.ENTER, 1.5f),
    )

    val letters: List<List<Key>> = listOf(
        chars("qwertyuiop"),
        chars("asdfghjklç"),
        listOf(Key("shift", KeyKind.SHIFT, 1.5f)) + chars("zxcvbnm") + Key("apagar", KeyKind.BACKSPACE, 1.5f),
        bottomRow(Key("?123", KeyKind.TO_SYMBOLS, 1.5f)),
    )

    val symbols: List<List<Key>> = listOf(
        chars("1234567890"),
        chars("@#$%&-+()/"),
        listOf(Key("=\\<", KeyKind.TO_SYMBOLS2, 1.5f)) + chars("*\"':;!?") + Key("apagar", KeyKind.BACKSPACE, 1.5f),
        bottomRow(Key("ABC", KeyKind.TO_LETTERS, 1.5f)),
    )

    val symbols2: List<List<Key>> = listOf(
        listOf("´", "`", "^", "~", "¨").map { Key(it, KeyKind.DEAD) } + chars("|•√π÷"),
        chars("×§¶€£¥_={}"),
        listOf(Key("?123", KeyKind.TO_SYMBOLS, 1.5f)) + chars("[]<>\\°¢") + Key("apagar", KeyKind.BACKSPACE, 1.5f),
        bottomRow(Key("ABC", KeyKind.TO_LETTERS, 1.5f)),
    )

    fun rows(layer: Layer) = when (layer) {
        Layer.LETTERS -> letters
        Layer.SYMBOLS -> symbols
        Layer.SYMBOLS2 -> symbols2
    }

    private val deadMap: Map<String, Map<Char, Char>> = mapOf(
        "´" to mapOf('a' to 'á', 'e' to 'é', 'i' to 'í', 'o' to 'ó', 'u' to 'ú', 'c' to 'ç', 'y' to 'ý'),
        "`" to mapOf('a' to 'à', 'e' to 'è', 'i' to 'ì', 'o' to 'ò', 'u' to 'ù'),
        "^" to mapOf('a' to 'â', 'e' to 'ê', 'i' to 'î', 'o' to 'ô', 'u' to 'û'),
        "~" to mapOf('a' to 'ã', 'o' to 'õ', 'n' to 'ñ'),
        "¨" to mapOf('a' to 'ä', 'e' to 'ë', 'i' to 'ï', 'o' to 'ö', 'u' to 'ü'),
    )

    /** Combina um acento (tecla morta) com o próximo caractere. */
    fun compose(dead: String, next: String): String {
        if (next == " ") return dead
        if (next.length != 1) return dead + next
        val c = next[0]
        val lower = c.lowercaseChar()
        val composed = deadMap[dead]?.get(lower) ?: return dead + next
        return if (c.isUpperCase()) composed.uppercaseChar().toString() else composed.toString()
    }
}
