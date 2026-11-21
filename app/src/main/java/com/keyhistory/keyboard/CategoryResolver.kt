package com.keyhistory.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo

object CategoryResolver {
    const val MESSAGE = "Mensagem"
    const val SEARCH = "Pesquisa"
    const val EMAIL = "E-mail"
    const val WEB = "Endereço web"
    const val NOTE = "Nota"
    const val TEXT = "Texto"

    val all = listOf(MESSAGE, SEARCH, EMAIL, WEB, NOTE, TEXT)

    private val messagingPackages = listOf(
        "whatsapp", "telegram", "messenger", "messaging", "signal", "mms", "sms", "discord", "slack",
        "instagram", "teams", "chat",
    )
    private val notePackages = listOf("keep", "notes", "notas", "evernote", "onenote", "docs", "notion")

    fun resolve(inputType: Int, imeOptions: Int, packageName: String?): String {
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        val action = imeOptions and EditorInfo.IME_MASK_ACTION
        val pkg = packageName.orEmpty().lowercase()
        return when {
            variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS ||
                variation == InputType.TYPE_TEXT_VARIATION_EMAIL_SUBJECT -> EMAIL
            variation == InputType.TYPE_TEXT_VARIATION_URI -> WEB
            action == EditorInfo.IME_ACTION_SEARCH -> SEARCH
            variation == InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE ||
                variation == InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE ||
                action == EditorInfo.IME_ACTION_SEND ||
                messagingPackages.any { pkg.contains(it) } -> MESSAGE
            notePackages.any { pkg.contains(it) } -> NOTE
            else -> TEXT
        }
    }
}
