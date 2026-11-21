package com.keyhistory.keyboard

/**
 * Mantém somente o texto digitado pelo usuário por meio deste teclado no campo atual.
 * O conteúdo pré-existente do campo nunca é lido nem registrado.
 */
class TypedTextBuffer(private val maxLength: Int = 20_000) {
    private val builder = StringBuilder()

    val text: String get() = builder.toString()
    val length: Int get() = builder.length
    fun isEmpty() = builder.isEmpty()
    fun isNotBlank() = builder.isNotBlank()

    fun append(value: CharSequence) {
        if (builder.length + value.length <= maxLength) builder.append(value)
    }

    fun deleteLast() {
        if (builder.isEmpty()) return
        val end = builder.length
        val cp = Character.codePointBefore(builder, end)
        builder.setLength(end - Character.charCount(cp))
    }

    fun clear() = builder.setLength(0)

    fun drain(): String = text.also { clear() }
}
