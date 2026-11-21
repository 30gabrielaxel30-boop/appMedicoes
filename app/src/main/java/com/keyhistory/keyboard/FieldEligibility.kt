package com.keyhistory.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo

/** Dados mínimos do campo de texto, desacoplados de [EditorInfo] para permitir testes. */
data class FieldInfo(
    val inputType: Int,
    val imeOptions: Int,
    val packageName: String?,
    val hintText: String? = null,
    val label: String? = null,
)

enum class BlockReason(val message: String) {
    PASSWORD("Campo de senha protegido"),
    NUMERIC("Campo numérico/PIN protegido"),
    NO_PERSONALIZED_LEARNING("Campo marcado como não pessoal"),
    SENSITIVE_HINT("Campo sensível protegido"),
    UNKNOWN_ORIGIN("Origem não verificada"),
    OWN_APP("Campo interno do KeyHistory"),
    EXCLUDED_APP("Aplicativo excluído do registro"),
    NOT_TEXT("Campo não textual"),
}

sealed interface Eligibility {
    data object Eligible : Eligibility
    data class Blocked(val reason: BlockReason) : Eligibility
}

/**
 * Decide se um campo pode ter o texto digitado registrado. Política restritiva:
 * qualquer dúvida resulta em bloqueio.
 */
object FieldEligibility {

    private val sensitiveKeywords = listOf(
        "senha", "password", "passcode", "pin", "código", "codigo", "code", "otp", "token",
        "cvv", "cvc", "cartão", "cartao", "card", "conta", "agência", "agencia", "iban",
        "cpf", "cnpj", "rg", "2fa", "autenticação", "autenticacao", "verificação", "verificacao",
        "segredo", "secret", "chave", "pix", "security", "segurança", "seguranca",
    )

    fun evaluate(
        field: FieldInfo,
        ownPackage: String,
        originVerified: Boolean,
        excludedPackages: Set<String> = emptySet(),
    ): Eligibility {
        val pkg = field.packageName
        if (pkg.isNullOrBlank() || !originVerified) return Eligibility.Blocked(BlockReason.UNKNOWN_ORIGIN)
        if (pkg == ownPackage) return Eligibility.Blocked(BlockReason.OWN_APP)
        if (pkg in excludedPackages) return Eligibility.Blocked(BlockReason.EXCLUDED_APP)

        val inputClass = field.inputType and InputType.TYPE_MASK_CLASS
        val variation = field.inputType and InputType.TYPE_MASK_VARIATION

        when (inputClass) {
            InputType.TYPE_CLASS_TEXT -> {
                if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                    variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                    variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
                ) return Eligibility.Blocked(BlockReason.PASSWORD)
            }
            InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_PHONE ->
                return Eligibility.Blocked(BlockReason.NUMERIC)
            else -> return Eligibility.Blocked(BlockReason.NOT_TEXT)
        }

        if (field.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0) {
            return Eligibility.Blocked(BlockReason.NO_PERSONALIZED_LEARNING)
        }

        val descriptor = listOfNotNull(field.hintText, field.label).joinToString(" ").lowercase()
        if (descriptor.isNotBlank()) {
            val words = descriptor.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }.toSet()
            if (sensitiveKeywords.any { it in words }) return Eligibility.Blocked(BlockReason.SENSITIVE_HINT)
        }
        return Eligibility.Eligible
    }
}
