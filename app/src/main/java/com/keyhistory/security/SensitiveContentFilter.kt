package com.keyhistory.security

/**
 * Segunda barreira de proteção: mesmo em campos comuns, o conteúdo é descartado quando
 * aparenta ser um dado sensível (cartão, código de verificação, CPF, chave/token, etc.).
 */
object SensitiveContentFilter {

    enum class Verdict { ALLOWED, EMPTY, CARD_NUMBER, VERIFICATION_CODE, DOCUMENT_NUMBER, SECRET_TOKEN }

    private val digitsOnly = Regex("^[\\d\\s.-]+$")
    private val cpf = Regex("\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b")
    private val cnpj = Regex("\\b\\d{2}\\.?\\d{3}\\.?\\d{3}/?\\d{4}-?\\d{2}\\b")
    private val cardCandidate = Regex("(?:\\d[ -]?){13,19}")
    private val tokenLike = Regex("^[A-Za-z0-9_\\-+/=]{24,}$")

    fun evaluate(text: String): Verdict {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Verdict.EMPTY

        val digits = trimmed.filter { it.isDigit() }
        if (trimmed.matches(digitsOnly) && digits.length in 4..8) return Verdict.VERIFICATION_CODE

        cardCandidate.findAll(trimmed).forEach { match ->
            val candidate = match.value.filter { it.isDigit() }
            if (candidate.length in 13..19 && luhn(candidate)) return Verdict.CARD_NUMBER
        }

        if (cpf.containsMatchIn(trimmed) || cnpj.containsMatchIn(trimmed)) return Verdict.DOCUMENT_NUMBER

        if (!trimmed.contains(' ') && trimmed.matches(tokenLike) &&
            trimmed.any { it.isDigit() } && trimmed.any { it.isLetter() }
        ) return Verdict.SECRET_TOKEN

        return Verdict.ALLOWED
    }

    fun luhn(number: String): Boolean {
        var sum = 0
        var double = false
        for (i in number.length - 1 downTo 0) {
            var d = number[i] - '0'
            if (d !in 0..9) return false
            if (double) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
            double = !double
        }
        return sum % 10 == 0
    }
}
