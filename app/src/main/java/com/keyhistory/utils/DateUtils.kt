package com.keyhistory.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class Period(val label: String) {
    TODAY("Hoje"),
    YESTERDAY("Ontem"),
    LAST_7("Últimos 7 dias"),
    LAST_30("Últimos 30 dias"),
    ALL("Histórico completo"),
    CUSTOM("Personalizado"),
}

data class TimeRange(val from: Long, val to: Long)

object DateUtils {
    val locale: Locale = Locale.forLanguageTag("pt-BR")
    var zone: ZoneId = ZoneId.systemDefault()

    private val dateFmt get() = DateTimeFormatter.ofPattern("dd/MM/yyyy", locale).withZone(zone)
    private val timeFmt get() = DateTimeFormatter.ofPattern("HH:mm:ss", locale).withZone(zone)
    private val shortTimeFmt get() = DateTimeFormatter.ofPattern("HH:mm", locale).withZone(zone)
    private val dayLabelFmt get() = DateTimeFormatter.ofPattern("EEE", locale).withZone(zone)
    private val dayMonthFmt get() = DateTimeFormatter.ofPattern("dd/MM", locale).withZone(zone)
    private val longFmt get() = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", locale).withZone(zone)

    fun formatDate(ts: Long): String = dateFmt.format(Instant.ofEpochMilli(ts))
    fun formatTime(ts: Long): String = timeFmt.format(Instant.ofEpochMilli(ts))
    fun formatShortTime(ts: Long): String = shortTimeFmt.format(Instant.ofEpochMilli(ts))
    fun formatDayMonth(ts: Long): String = dayMonthFmt.format(Instant.ofEpochMilli(ts))
    fun formatLong(ts: Long): String = longFmt.format(Instant.ofEpochMilli(ts)).replaceFirstChar { it.titlecase(locale) }
    fun weekdayShort(date: LocalDate): String =
        dayLabelFmt.format(date.atStartOfDay(zone)).replace(".", "").replaceFirstChar { it.titlecase(locale) }

    fun today(now: Long = System.currentTimeMillis()): LocalDate = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    fun startOf(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()
    fun endOf(date: LocalDate): Long = startOf(date.plusDays(1)) - 1
    fun toLocalDate(ts: Long): LocalDate = Instant.ofEpochMilli(ts).atZone(zone).toLocalDate()

    /** Converte a data UTC retornada pelo DatePicker do Material3 em data local. */
    fun pickerUtcToLocalDate(utcMillis: Long): LocalDate = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()

    fun rangeFor(period: Period, now: Long = System.currentTimeMillis()): TimeRange {
        val today = today(now)
        return when (period) {
            Period.TODAY -> TimeRange(startOf(today), endOf(today))
            Period.YESTERDAY -> today.minusDays(1).let { TimeRange(startOf(it), endOf(it)) }
            Period.LAST_7 -> TimeRange(startOf(today.minusDays(6)), endOf(today))
            Period.LAST_30 -> TimeRange(startOf(today.minusDays(29)), endOf(today))
            Period.ALL, Period.CUSTOM -> TimeRange(0L, Long.MAX_VALUE)
        }
    }

    /** Seção cronológica de um registro: Hoje, Ontem, Últimos 7 dias, Últimos 30 dias, Mais antigos. */
    fun sectionFor(ts: Long, now: Long = System.currentTimeMillis()): String {
        val days = ChronoUnit.DAYS.between(toLocalDate(ts), today(now))
        return when {
            days <= 0L -> "Hoje"
            days == 1L -> "Ontem"
            days < 7L -> "Últimos 7 dias"
            days < 30L -> "Últimos 30 dias"
            else -> "Mais antigos"
        }
    }

    fun formatBytes(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format(locale, "%.1f KB", bytes / 1024.0)
        else -> String.format(locale, "%.2f MB", bytes / (1024.0 * 1024.0))
    }
}
