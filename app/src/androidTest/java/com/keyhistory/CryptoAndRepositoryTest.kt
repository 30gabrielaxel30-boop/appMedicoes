package com.keyhistory

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keyhistory.database.KeyHistoryDatabase
import com.keyhistory.repository.HistoryQuery
import com.keyhistory.repository.HistoryRepository
import com.keyhistory.security.CryptoManager
import com.keyhistory.utils.CsvWriter
import com.keyhistory.utils.DateUtils
import com.keyhistory.utils.Period
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class CryptoInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val crypto = CryptoManager(context)

    @Test fun roundTrip() {
        val text = "Olá, KeyHistory! Ação & coração 😀"
        val enc = crypto.encrypt(text)
        assertTrue(enc.startsWith(CryptoManager.PREFIX))
        assertFalse(enc.contains("KeyHistory"))
        assertEquals(text, crypto.decrypt(enc))
    }

    @Test fun randomIvPerEncryption() {
        assertNotEquals(crypto.encrypt("mesmo texto"), crypto.encrypt("mesmo texto"))
    }

    @Test fun tamperingIsDetected() {
        val enc = crypto.encrypt("integridade")
        val chars = enc.toCharArray()
        val i = chars.size - 3
        chars[i] = if (chars[i] == 'A') 'B' else 'A'
        val result = runCatching { crypto.decrypt(String(chars)) }
        assertTrue("GCM deve rejeitar conteúdo adulterado", result.isFailure)
    }

    @Test fun keysPersistAcrossInstances() {
        val enc = crypto.encrypt("persistente")
        assertEquals("persistente", CryptoManager(context).decrypt(enc))
        assertEquals(crypto.blindToken("ola"), CryptoManager(context).blindToken("ola"))
    }

    @Test fun selfTest() { assertTrue(crypto.selfTest()) }
}

@RunWith(AndroidJUnit4::class)
class RepositoryInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: KeyHistoryDatabase
    private lateinit var repo: HistoryRepository

    @Before fun setUp() {
        db = KeyHistoryDatabase.build(context, inMemory = true)
        repo = HistoryRepository(context, db.historyDao(), CryptoManager(context))
    }

    @After fun tearDown() { db.close() }

    @Test fun storesEncryptedAndSearches(): Unit = runBlocking {
        val now = System.currentTimeMillis()
        val id1 = repo.save("Reunião de equipe amanhã às 10h", "com.whatsapp", "WhatsApp", "Mensagem", now)
        repo.save("Comprar pão e leite", "com.google.android.keep", "Keep", "Nota", now - 2 * 86_400_000L)
        repo.save("receita de bolo de cenoura", "com.android.chrome", "Chrome", "Pesquisa", now - 40 * 86_400_000L)

        // Conteúdo cifrado no banco
        val raw = db.historyDao().getAll()
        assertEquals(3, raw.size)
        raw.forEach {
            assertTrue(it.encrypted)
            assertTrue(it.content.startsWith("v1:"))
            assertFalse(it.content.contains("Reunião") || it.content.contains("pão") || it.content.contains("bolo"))
        }
        assertTrue(db.historyDao().tokenCount() > 0)

        // Pesquisa por palavra (sem acento, prefixo)
        val r1 = repo.observe(HistoryQuery("reuniao")).first()
        assertEquals(listOf(id1), r1.map { it.id })
        assertEquals("Reunião de equipe amanhã às 10h", r1.first().content)
        assertEquals(1, repo.observe(HistoryQuery("equi amanh")).first().size)

        // Frase exata
        assertEquals(1, repo.observe(HistoryQuery("bolo de cenoura", exactPhrase = true)).first().size)
        assertEquals(0, repo.observe(HistoryQuery("cenoura de bolo", exactPhrase = true)).first().size)

        // Filtro por aplicativo e data
        assertEquals(1, repo.observe(HistoryQuery(packageName = "com.google.android.keep")).first().size)
        val last7 = DateUtils.rangeFor(Period.LAST_7)
        assertEquals(2, repo.observe(HistoryQuery(from = last7.from, to = last7.to)).first().size)
        val today = DateUtils.rangeFor(Period.TODAY)
        assertEquals(1, repo.observe(HistoryQuery(from = today.from, to = today.to)).first().size)
        assertEquals(0, repo.observe(HistoryQuery("inexistente")).first().size)

        // Estatísticas
        assertEquals(3, repo.observeCount().first())
        assertEquals(3, repo.observeAppCount().first())

        // Exportação CSV
        val records = repo.recordsForExport(null, 0, Long.MAX_VALUE)
        val out = ByteArrayOutputStream()
        CsvWriter.write(records, out)
        val csv = out.toString(Charsets.UTF_8.name())
        assertTrue(csv.startsWith("﻿Data;Horário;Aplicativo;Conteúdo;Categoria"))
        assertTrue(csv.contains("Comprar pão e leite"))
        assertEquals(4, csv.trim().split("\r\n").size)

        // Exclusão (cascata no índice)
        repo.delete(id1)
        assertEquals(0, repo.observe(HistoryQuery("reuniao")).first().size)
        assertEquals(1, repo.deleteOlderThan(now - 30 * 86_400_000L))
        repo.deleteAll()
        assertEquals(0, repo.observeCount().first())
        assertEquals(0, db.historyDao().tokenCount())
    }
}
