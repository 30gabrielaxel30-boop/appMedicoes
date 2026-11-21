package com.keyhistory

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.keyhistory.repository.HistoryQuery
import com.keyhistory.repository.HistoryRecord
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Teste de ponta a ponta: o teclado KeyHistory é ativado e selecionado no sistema e usado
 * para digitar em OUTRO aplicativo (com.keyhistory.testfield), exatamente como em uso real.
 */
@RunWith(AndroidJUnit4::class)
class KeyboardE2ETest {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val app = ApplicationProvider.getApplicationContext<KeyHistoryApp>()
    private val repo get() = app.container.historyRepository
    private val settings get() = app.container.settingsRepository
    private val ime = "com.keyhistory/.keyboard.KeyHistoryImeService"
    private val field = "com.keyhistory.testfield"

    @Before fun setUp(): Unit = runBlocking {
        device.executeShellCommand("settings put secure show_ime_with_hard_keyboard 1")
        device.executeShellCommand("ime enable $ime")
        device.executeShellCommand("ime set $ime")
        settings.setRecordingEnabled(true)
        settings.setAutoSave(true)
        settings.setPaused(false)
        repo.deleteAll()
        device.executeShellCommand("am start -W -S -n $field/.FieldActivity")
        assertTrue("Teclado KeyHistory deve estar selecionado",
            device.executeShellCommand("settings get secure default_input_method").trim() == ime)
    }

    @After fun tearDown(): Unit = runBlocking {
        settings.setRecordingEnabled(false)
        repo.deleteAll()
        Unit
    }

    private fun obj(selector: androidx.test.uiautomator.BySelector, timeout: Long = 8_000): UiObject2 =
        device.wait(Until.findObject(selector), timeout) ?: error("Elemento não encontrado: $selector")

    private fun focus(id: String) {
        obj(By.res(field, id)).click()
        obj(By.desc("Status do registro"))
        device.waitForIdle()
        Thread.sleep(400)
    }

    private fun key(desc: String) {
        obj(By.desc(desc)).click()
        device.waitForIdle()
    }

    private fun type(text: String) = text.forEach { c -> key(if (c == ' ') "Tecla Espaço" else "Tecla $c") }

    private fun status(): String = obj(By.desc("Status do registro")).text

    private suspend fun records(): List<HistoryRecord> = repo.recordsForExport(null, 0, Long.MAX_VALUE)

    private suspend fun awaitCount(n: Int): List<HistoryRecord> {
        repeat(50) {
            val r = records()
            if (r.size >= n) return r
            delay(100)
        }
        return records()
    }

    @Test fun typingInAnotherAppIsRecordedEncryptedAndProtectedFieldsAreNot(): Unit = runBlocking {
        // 1) Digitação convencional + registro autorizado
        focus("field_text")
        assertTrue("Indicador deve mostrar registro ativo: ${status()}", status().contains("Registro ativo"))
        type("ola mundo")
        assertEquals("ola mundo", obj(By.res(field, "field_text")).text)
        device.executeShellCommand("screencap -p /data/local/tmp/kh_keyboard_active.png")
        key("Tecla Enter")
        obj(By.res(field, "last_sent").text("ola mundo"))

        val first = awaitCount(1)
        assertEquals(1, first.size)
        with(first.single()) {
            assertEquals("ola mundo", content)
            assertEquals(field, packageName)
            assertEquals("KeyHistory Test Field", applicationName)
            assertEquals("Mensagem", category)
            assertTrue(encrypted)
        }
        val raw = app.container.database.historyDao().getAll().single()
        assertTrue(raw.content.startsWith("v1:"))
        assertFalse(raw.content.contains("mundo"))

        // 2) Campo de senha: nunca registrado
        focus("field_password")
        assertTrue(status(), status().contains("Campo de senha protegido"))
        device.executeShellCommand("screencap -p /data/local/tmp/kh_keyboard_protected.png")
        type("segredo")
        key("Tecla Enter")

        // 3) PIN numérico: nunca registrado (teclado abre em números)
        focus("field_pin")
        assertTrue(status(), status().contains("PIN"))
        type("4821")
        key("Tecla Enter")

        // 4) Campo "não pessoal" (modo anônimo): nunca registrado
        focus("field_incognito")
        assertTrue(status(), status().contains("não pessoal"))
        type("anonimo")
        key("Tecla Enter")

        // 5) Acentuação por toque longo não é necessária: usa tecla Ç e acento morto
        focus("field_text")
        type("ç")
        key("Tecla ?123"); key("Tecla =\\<"); key("Tecla ´")
        type("e")
        assertEquals("çé", obj(By.res(field, "field_text")).text)
        key("Tecla Enter")

        val afterProtected = awaitCount(2)
        assertEquals(2, afterProtected.size)
        assertTrue(afterProtected.any { it.content == "çé" })
        afterProtected.forEach {
            assertFalse(it.content.contains("segredo"))
            assertFalse(it.content.contains("4821"))
            assertFalse(it.content.contains("anonimo"))
        }

        // 6) Pausa pelo botão do teclado
        focus("field_text")
        key("Pausar registro")
        obj(By.desc("Status do registro").text("Registro pausado"))
        type("pausado")
        key("Tecla Enter")
        delay(1_500)
        assertEquals(2, records().size)
        key("Retomar registro")
        obj(By.desc("Status do registro").textContains("Registro ativo"))

        // 7) Backspace
        type("abcx")
        key("Tecla Apagar")
        assertEquals("abc", obj(By.res(field, "field_text")).text)
        key("Tecla Enter")
        val final = awaitCount(3)
        assertTrue(final.any { it.content == "abc" })

        // 8) Pesquisa sobre os dados registrados pelo teclado
        assertEquals(listOf("ola mundo"), repo.observe(HistoryQuery("mund")).first().map { it.content })
    }
}
