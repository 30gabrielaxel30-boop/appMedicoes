package com.keyhistory.utils

import java.text.Normalizer

/**
 * Normalização e geração de termos para o índice de pesquisa cego.
 * Os termos são convertidos em HMAC antes de chegar ao banco de dados.
 */
object SearchTokenizer {
    const val MIN_PREFIX = 2
    const val MAX_PREFIX = 24
    private val diacritics = Regex("\\p{Mn}+")
    private val separators = Regex("[^\\p{L}\\p{N}]+")

    fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(diacritics, "")

    fun words(text: String): List<String> =
        normalize(text).split(separators).filter { it.isNotEmpty() }

    /** Todos os prefixos (2..24 caracteres) de cada palavra, para pesquisa por início de palavra. */
    fun indexTerms(text: String): Set<String> {
        val out = LinkedHashSet<String>()
        for (word in words(text)) {
            val max = minOf(word.length, MAX_PREFIX)
            for (len in MIN_PREFIX..max) out += word.substring(0, len)
        }
        return out
    }

    /** Termos de consulta que podem ser resolvidos pelo índice. */
    fun queryTerms(query: String): Set<String> =
        words(query).filter { it.length >= MIN_PREFIX }.map { it.take(MAX_PREFIX) }.toSet()

    /** Verificação final, em memória, após a descriptografia. */
    fun matches(content: String, query: String, exactPhrase: Boolean): Boolean {
        val normalizedContent = normalize(content)
        if (exactPhrase) {
            val phrase = words(query).joinToString(" ")
            return phrase.isEmpty() || words(content).joinToString(" ").contains(phrase)
        }
        val contentWords = words(content)
        return words(query).all { term -> contentWords.any { it.startsWith(term) } || normalizedContent.contains(term) }
    }
}
