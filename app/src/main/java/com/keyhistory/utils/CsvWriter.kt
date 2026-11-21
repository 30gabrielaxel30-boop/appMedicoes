package com.keyhistory.utils

import com.keyhistory.repository.HistoryRecord
import java.io.OutputStream
import java.io.OutputStreamWriter

/** Exportação CSV (separador ';' e BOM UTF-8 para compatibilidade com o Excel em pt-BR). */
object CsvWriter {
    const val SEPARATOR = ';'
    val HEADER = listOf("Data", "Horário", "Aplicativo", "Conteúdo", "Categoria")

    fun escape(value: String): String {
        val needsQuotes = value.any { it == SEPARATOR || it == '"' || it == '\n' || it == '\r' || it == ',' }
        // Evita injeção de fórmulas em planilhas.
        val safe = if (value.isNotEmpty() && value[0] in "=+-@\t") "'$value" else value
        return if (needsQuotes || safe != value) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }

    fun row(values: List<String>): String = values.joinToString(SEPARATOR.toString()) { escape(it) }

    fun toCsv(records: List<HistoryRecord>): String = buildString {
        append(row(HEADER)).append("\r\n")
        for (r in records) {
            append(
                row(
                    listOf(
                        DateUtils.formatDate(r.timestamp),
                        DateUtils.formatTime(r.timestamp),
                        r.applicationName ?: r.packageName ?: "Desconhecido",
                        r.content,
                        r.category,
                    )
                )
            ).append("\r\n")
        }
    }

    fun write(records: List<HistoryRecord>, out: OutputStream) {
        OutputStreamWriter(out, Charsets.UTF_8).use { w ->
            w.write("﻿")
            w.write(toCsv(records))
        }
    }
}
