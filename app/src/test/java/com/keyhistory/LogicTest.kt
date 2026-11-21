package com.keyhistory

import android.text.InputType
import android.view.inputmethod.EditorInfo
import com.keyhistory.keyboard.BlockReason
import com.keyhistory.keyboard.CategoryResolver
import com.keyhistory.keyboard.Eligibility
import com.keyhistory.keyboard.FieldEligibility
import com.keyhistory.keyboard.FieldInfo
import com.keyhistory.keyboard.KeyboardLayouts
import com.keyhistory.keyboard.TypedTextBuffer
import com.keyhistory.repository.HistoryRecord
import com.keyhistory.security.SensitiveContentFilter
import com.keyhistory.security.SensitiveContentFilter.Verdict
import com.keyhistory.utils.CsvWriter
import com.keyhistory.utils.DateUtils
import com.keyhistory.utils.Period
import com.keyhistory.utils.SearchTokenizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class FieldEligibilityTest {
    private val own = "com.keyhistory"
    private val text = InputType.TYPE_CLASS_TEXT
    private fun eval(type: Int, ime: Int = 0, pkg: String? = "com.example", hint: String? = null, verified: Boolean = true,
                     excluded: Set<String> = emptySet()) =
        FieldEligibility.evaluate(FieldInfo(type, ime, pkg, hint), own, verified, excluded)

    @Test fun normalTextIsEligible() = assertEquals(Eligibility.Eligible, eval(text))
    @Test fun passwordBlocked() = assertEquals(Eligibility.Blocked(BlockReason.PASSWORD), eval(text or InputType.TYPE_TEXT_VARIATION_PASSWORD))
    @Test fun visiblePasswordBlocked() = assertEquals(Eligibility.Blocked(BlockReason.PASSWORD), eval(text or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD))
    @Test fun webPasswordBlocked() = assertEquals(Eligibility.Blocked(BlockReason.PASSWORD), eval(text or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD))
    @Test fun numberPinBlocked() = assertEquals(Eligibility.Blocked(BlockReason.NUMERIC),
        eval(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD))
    @Test fun phoneBlocked() = assertEquals(Eligibility.Blocked(BlockReason.NUMERIC), eval(InputType.TYPE_CLASS_PHONE))
    @Test fun nullInputTypeBlocked() = assertEquals(Eligibility.Blocked(BlockReason.NOT_TEXT), eval(InputType.TYPE_NULL))
    @Test fun incognitoBlocked() = assertEquals(Eligibility.Blocked(BlockReason.NO_PERSONALIZED_LEARNING),
        eval(text, EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING))
    @Test fun unknownOriginBlocked() {
        assertEquals(Eligibility.Blocked(BlockReason.UNKNOWN_ORIGIN), eval(text, pkg = null))
        assertEquals(Eligibility.Blocked(BlockReason.UNKNOWN_ORIGIN), eval(text, verified = false))
    }
    @Test fun ownAppBlocked() = assertEquals(Eligibility.Blocked(BlockReason.OWN_APP), eval(text, pkg = own))
    @Test fun excludedAppBlocked() = assertEquals(Eligibility.Blocked(BlockReason.EXCLUDED_APP), eval(text, excluded = setOf("com.example")))
    @Test fun sensitiveHintsBlocked() {
        listOf("Senha", "Código de verificação", "Número do cartão", "CPF", "Token 2FA", "Chave Pix", "CVV").forEach {
            assertEquals(it, Eligibility.Blocked(BlockReason.SENSITIVE_HINT), eval(text, hint = it))
        }
    }
    @Test fun regularHintsAllowed() {
        listOf("Mensagem", "Pesquisar", "Escreva um comentário", "Título da nota").forEach {
            assertEquals(it, Eligibility.Eligible, eval(text, hint = it))
        }
    }
}

class SensitiveContentFilterTest {
    @Test fun normalText() = assertEquals(Verdict.ALLOWED, SensitiveContentFilter.evaluate("Olá, tudo bem? Chego às 18h."))
    @Test fun empty() = assertEquals(Verdict.EMPTY, SensitiveContentFilter.evaluate("   "))
    @Test fun otp() {
        assertEquals(Verdict.VERIFICATION_CODE, SensitiveContentFilter.evaluate("482913"))
        assertEquals(Verdict.VERIFICATION_CODE, SensitiveContentFilter.evaluate("1234"))
    }
    @Test fun card() {
        assertEquals(Verdict.CARD_NUMBER, SensitiveContentFilter.evaluate("meu cartão 4111 1111 1111 1111 ok"))
        assertEquals(Verdict.CARD_NUMBER, SensitiveContentFilter.evaluate("5555555555554444"))
    }
    @Test fun cpf() = assertEquals(Verdict.DOCUMENT_NUMBER, SensitiveContentFilter.evaluate("cpf 123.456.789-09"))
    @Test fun token() = assertEquals(Verdict.SECRET_TOKEN, SensitiveContentFilter.evaluate("sk_live_51HxYzAbCdEf1234567890"))
    @Test fun luhn() {
        assertTrue(SensitiveContentFilter.luhn("4111111111111111"))
        assertFalse(SensitiveContentFilter.luhn("4111111111111112"))
    }
}

class SearchTokenizerTest {
    @Test fun normalizeRemovesAccents() = assertEquals("acao coracao", SearchTokenizer.normalize("Ação Coração"))
    @Test fun indexTermsArePrefixes() {
        val terms = SearchTokenizer.indexTerms("Olá mundo")
        assertTrue(terms.containsAll(listOf("ol", "ola", "mu", "mun", "mund", "mundo")))
        assertFalse("o" in terms)
    }
    @Test fun queryTermsMatchIndex() {
        val index = SearchTokenizer.indexTerms("Reunião amanhã às 10h")
        assertTrue(index.containsAll(SearchTokenizer.queryTerms("reuniao AMANHA")))
    }
    @Test fun matchesWordsAndPhrase() {
        val c = "Vamos almoçar no restaurante amanhã"
        assertTrue(SearchTokenizer.matches(c, "almoçar amanhã", exactPhrase = false))
        assertTrue(SearchTokenizer.matches(c, "almocar no restaurante", exactPhrase = true))
        assertFalse(SearchTokenizer.matches(c, "restaurante almocar", exactPhrase = true))
        assertFalse(SearchTokenizer.matches(c, "jantar", exactPhrase = false))
    }
}

class CsvWriterTest {
    @Test fun escaping() {
        assertEquals("simples", CsvWriter.escape("simples"))
        assertEquals("\"a;b\"", CsvWriter.escape("a;b"))
        assertEquals("\"diz \"\"oi\"\"\"", CsvWriter.escape("diz \"oi\""))
        assertEquals("\"linha1\nlinha2\"", CsvWriter.escape("linha1\nlinha2"))
        assertEquals("\"'=SOMA(A1)\"", CsvWriter.escape("=SOMA(A1)"))
    }
    @Test fun fullCsv() {
        DateUtils.zone = ZoneId.of("America/Sao_Paulo")
        val ts = 1_700_000_000_000L // 14/11/2023 19:13:20 (BRT)
        val csv = CsvWriter.toCsv(listOf(HistoryRecord(1, "Olá; mundo", "com.whatsapp", "WhatsApp", ts, "Mensagem", true)))
        val lines = csv.trimEnd().split("\r\n")
        assertEquals("Data;Horário;Aplicativo;Conteúdo;Categoria", lines[0])
        assertEquals("14/11/2023;19:13:20;WhatsApp;\"Olá; mundo\";Mensagem", lines[1])
        DateUtils.zone = ZoneId.systemDefault()
    }
}

class KeyboardLogicTest {
    @Test fun deadKeys() {
        assertEquals("á", KeyboardLayouts.compose("´", "a"))
        assertEquals("Ã", KeyboardLayouts.compose("~", "A"))
        assertEquals("ê", KeyboardLayouts.compose("^", "e"))
        assertEquals("ç", KeyboardLayouts.compose("´", "c"))
        assertEquals("~", KeyboardLayouts.compose("~", " "))
        assertEquals("´x", KeyboardLayouts.compose("´", "x"))
    }
    @Test fun layoutHasAbnt2Cedilla() {
        assertTrue(KeyboardLayouts.letters[1].any { it.label == "ç" })
        KeyboardLayouts.letters.forEach { row -> assertEquals(10f, row.sumOf { it.weight.toDouble() }.toFloat(), 0.01f) }
        KeyboardLayouts.symbols.forEach { row -> assertEquals(10f, row.sumOf { it.weight.toDouble() }.toFloat(), 0.01f) }
        KeyboardLayouts.symbols2.forEach { row -> assertEquals(10f, row.sumOf { it.weight.toDouble() }.toFloat(), 0.01f) }
    }
    @Test fun bufferHandlesSurrogates() {
        val b = TypedTextBuffer()
        b.append("oi 😀")
        b.deleteLast()
        assertEquals("oi ", b.text)
        b.deleteLast(); b.deleteLast(); b.deleteLast(); b.deleteLast()
        assertTrue(b.isEmpty())
    }
    @Test fun categories() {
        assertEquals(CategoryResolver.SEARCH, CategoryResolver.resolve(InputType.TYPE_CLASS_TEXT, EditorInfo.IME_ACTION_SEARCH, "com.android.chrome"))
        assertEquals(CategoryResolver.MESSAGE, CategoryResolver.resolve(InputType.TYPE_CLASS_TEXT, 0, "com.whatsapp"))
        assertEquals(CategoryResolver.EMAIL, CategoryResolver.resolve(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, 0, "x"))
        assertEquals(CategoryResolver.TEXT, CategoryResolver.resolve(InputType.TYPE_CLASS_TEXT, 0, "com.example"))
    }
}

class DateUtilsTest {
    @Test fun sections() {
        val now = System.currentTimeMillis()
        val day = 24L * 3600 * 1000
        assertEquals("Hoje", DateUtils.sectionFor(now, now))
        assertEquals("Ontem", DateUtils.sectionFor(DateUtils.startOf(DateUtils.today(now).minusDays(1)) + 1000, now))
        assertEquals("Últimos 7 dias", DateUtils.sectionFor(now - 4 * day, now))
        assertEquals("Últimos 30 dias", DateUtils.sectionFor(now - 20 * day, now))
        assertEquals("Mais antigos", DateUtils.sectionFor(now - 60 * day, now))
    }
    @Test fun ranges() {
        val now = System.currentTimeMillis()
        val today = DateUtils.rangeFor(Period.TODAY, now)
        assertTrue(now in today.from..today.to)
        val yesterday = DateUtils.rangeFor(Period.YESTERDAY, now)
        assertEquals(today.from - 1, yesterday.to)
    }
}
