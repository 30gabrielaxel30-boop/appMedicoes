package com.keyhistory.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keyhistory.KeyHistoryApp
import com.keyhistory.database.AppUsage
import com.keyhistory.repository.AppSettings
import com.keyhistory.repository.DashboardStats
import com.keyhistory.repository.HistoryQuery
import com.keyhistory.repository.HistoryRecord
import com.keyhistory.repository.HistoryRepository
import com.keyhistory.repository.SettingsRepository
import com.keyhistory.components.ChartPoint
import com.keyhistory.utils.CsvWriter
import com.keyhistory.utils.DateUtils
import com.keyhistory.utils.KeyboardState
import com.keyhistory.utils.KeyboardStatus
import com.keyhistory.utils.Period
import com.keyhistory.utils.TimeRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek

private val Application.container get() = (this as KeyHistoryApp).container

// =============================================================== Dashboard

data class DashboardUi(
    val stats: DashboardStats = DashboardStats(),
    val daily: List<ChartPoint> = emptyList(),
    val weekly: List<ChartPoint> = emptyList(),
    val apps: List<ChartPoint> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val keyboard: KeyboardState = KeyboardState(installed = true, enabled = false, selected = false),
)

class DashboardViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = app.container.historyRepository
    private val settingsRepo = app.container.settingsRepository
    private val keyboard = MutableStateFlow(KeyboardStatus.read(app))
    private val storage = MutableStateFlow(repo.storageBytes())

    private val since = DateUtils.startOf(DateUtils.today().minusWeeks(7).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))

    private val counts = combine(
        repo.observeCount(),
        repo.observeCountSince(DateUtils.startOf(DateUtils.today())),
        repo.observeAppCount(),
        repo.observeLastTimestamp(),
        storage,
    ) { total, today, apps, last, bytes -> DashboardStats(total, today, apps, bytes, last) }

    val ui: StateFlow<DashboardUi> = combine(
        counts,
        repo.observeTimestampsSince(since),
        repo.observeAppUsage(),
        settingsRepo.settings,
        keyboard,
    ) { stats, timestamps, usage, settings, kb ->
        DashboardUi(
            stats = stats,
            daily = dailyPoints(timestamps),
            weekly = weeklyPoints(timestamps),
            apps = appPoints(usage),
            settings = settings,
            keyboard = kb,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUi())

    fun refresh() {
        keyboard.value = KeyboardStatus.read(getApplication())
        storage.value = repo.storageBytes()
    }

    fun enableRecording(enabled: Boolean) = viewModelScope.launch { settingsRepo.setRecordingEnabled(enabled) }
    fun setPaused(paused: Boolean) = viewModelScope.launch { settingsRepo.setPaused(paused) }

    private fun dailyPoints(ts: List<Long>): List<ChartPoint> {
        val today = DateUtils.today()
        val byDay = ts.groupingBy { DateUtils.toLocalDate(it) }.eachCount()
        return (6 downTo 0).map { offset ->
            val d = today.minusDays(offset.toLong())
            ChartPoint(if (offset == 0) "Hoje" else DateUtils.weekdayShort(d), byDay[d] ?: 0)
        }
    }

    private fun weeklyPoints(ts: List<Long>): List<ChartPoint> {
        val thisWeek = DateUtils.today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val byWeek = ts.groupingBy {
            ChronoUnit.WEEKS.between(DateUtils.toLocalDate(it).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), thisWeek)
        }.eachCount()
        return (7 downTo 0).map { w ->
            val start = thisWeek.minusWeeks(w.toLong())
            ChartPoint(DateUtils.formatDayMonth(DateUtils.startOf(start)), byWeek[w.toLong()] ?: 0)
        }
    }

    private fun appPoints(usage: List<AppUsage>): List<ChartPoint> {
        val top = usage.take(5).map { ChartPoint(it.applicationName ?: it.packageName, it.total) }
        val others = usage.drop(5).sumOf { it.total }
        return if (others > 0) top + ChartPoint("Outros", others) else top
    }
}

// ================================================================= History

data class HistorySection(val title: String, val records: List<HistoryRecord>)

class HistoryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = app.container.historyRepository
    val period = MutableStateFlow(Period.ALL)
    private val limit = MutableStateFlow(PAGE)
    private val _selected = MutableStateFlow<Set<Long>>(emptySet())
    val selected: StateFlow<Set<Long>> = _selected.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val records: StateFlow<List<HistoryRecord>?> = combine(period, limit) { p, l -> p to l }
        .flatMapLatest { (p, l) ->
            val r = DateUtils.rangeFor(p)
            repo.observe(HistoryQuery(from = r.from, to = r.to), l)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val sections: StateFlow<List<HistorySection>> = records.map { list ->
        val now = System.currentTimeMillis()
        list.orEmpty().groupBy { DateUtils.sectionFor(it.timestamp, now) }.map { (k, v) -> HistorySection(k, v) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val canLoadMore: StateFlow<Boolean> = combine(records, limit) { r, l -> (r?.size ?: 0) >= l }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setPeriod(p: Period) { period.value = p; limit.value = PAGE; clearSelection() }
    fun loadMore() = limit.update { it + PAGE }
    fun toggle(id: Long) = _selected.update { if (id in it) it - id else it + id }
    fun selectAll() { _selected.value = records.value.orEmpty().map { it.id }.toSet() }
    fun clearSelection() { _selected.value = emptySet() }
    fun deleteSelected() = viewModelScope.launch { repo.delete(_selected.value); clearSelection() }

    companion object { const val PAGE = 300 }
}

// ================================================================== Search

data class SearchFilters(
    val text: String = "",
    val exactPhrase: Boolean = false,
    val period: Period = Period.ALL,
    val customRange: TimeRange? = null,
    val packageName: String? = null,
)

class SearchViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = app.container.historyRepository
    private val _filters = MutableStateFlow(SearchFilters())
    val filters: StateFlow<SearchFilters> = _filters.asStateFlow()

    val apps: StateFlow<List<AppUsage>> = repo.observeAppUsage()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val results: StateFlow<List<HistoryRecord>?> = _filters.debounce(200).flatMapLatest { f ->
        val range = if (f.period == Period.CUSTOM) f.customRange ?: DateUtils.rangeFor(Period.ALL) else DateUtils.rangeFor(f.period)
        repo.observe(HistoryQuery(f.text, f.exactPhrase, range.from, range.to, f.packageName), 300)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setText(t: String) = _filters.update { it.copy(text = t) }
    fun setExact(v: Boolean) = _filters.update { it.copy(exactPhrase = v) }
    fun setPeriod(p: Period) = _filters.update { it.copy(period = p) }
    fun setCustomRange(r: TimeRange) = _filters.update { it.copy(period = Period.CUSTOM, customRange = r) }
    fun setPackage(pkg: String?) = _filters.update { it.copy(packageName = pkg) }
    fun clear() { _filters.value = SearchFilters() }
}

// ================================================================== Detail

class DetailViewModel(private val id: Long, private val repo: HistoryRepository) : ViewModel() {
    val record: StateFlow<HistoryRecord?> = repo.observeRecord(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun delete(onDone: () -> Unit) = viewModelScope.launch { repo.delete(id); onDone() }

    companion object {
        fun factory(id: Long, app: Application): ViewModelProvider.Factory = viewModelFactory {
            initializer { DetailViewModel(id, app.container.historyRepository) }
        }
    }
}

// ================================================================= Settings

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = app.container.historyRepository
    private val settingsRepo = app.container.settingsRepository
    val settings: StateFlow<AppSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    val apps: StateFlow<List<AppUsage>> = repo.observeAppUsage()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val total: StateFlow<Int> = repo.observeCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    private val _storage = MutableStateFlow(repo.storageBytes())
    val storage: StateFlow<Long> = _storage.asStateFlow()
    private val _keyboard = MutableStateFlow(KeyboardStatus.read(app))
    val keyboard: StateFlow<KeyboardState> = _keyboard.asStateFlow()
    val encryptionOk: Boolean = app.container.crypto.selfTest()

    fun refresh() {
        _keyboard.value = KeyboardStatus.read(getApplication())
        _storage.value = repo.storageBytes()
    }

    fun setRecording(v: Boolean) = viewModelScope.launch { settingsRepo.setRecordingEnabled(v) }
    fun setPaused(v: Boolean) = viewModelScope.launch { settingsRepo.setPaused(v) }
    fun setAutoSave(v: Boolean) = viewModelScope.launch { settingsRepo.setAutoSave(v) }
    fun setHaptic(v: Boolean) = viewModelScope.launch { settingsRepo.setHaptic(v) }
    fun setExcluded(pkg: String, v: Boolean) = viewModelScope.launch { settingsRepo.setExcluded(pkg, v) }
    fun deleteApp(pkg: String) = viewModelScope.launch { repo.deleteByPackage(pkg); refresh() }
    fun deleteOlderThan(days: Int) = viewModelScope.launch {
        repo.deleteOlderThan(DateUtils.startOf(DateUtils.today().minusDays(days.toLong()))); refresh()
    }
    fun clearAll() = viewModelScope.launch { repo.deleteAll(); refresh() }
}

// =================================================================== Export

sealed interface ExportState {
    data object Idle : ExportState
    data object Running : ExportState
    data class Done(val count: Int) : ExportState
    data class Failed(val message: String) : ExportState
}

class ExportViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = app.container.historyRepository
    private val _state = MutableStateFlow<ExportState>(ExportState.Idle)
    val state: StateFlow<ExportState> = _state.asStateFlow()
    val period = MutableStateFlow(Period.ALL)
    val customRange = MutableStateFlow<TimeRange?>(null)
    /** IDs selecionados no histórico (exportação parcial). */
    var pendingIds: List<Long>? = null

    fun export(uri: Uri, ids: List<Long>? = pendingIds) = viewModelScope.launch {
        _state.value = ExportState.Running
        _state.value = runCatching {
            val range = if (period.value == Period.CUSTOM) customRange.value ?: DateUtils.rangeFor(Period.ALL)
            else DateUtils.rangeFor(period.value)
            val records = repo.recordsForExport(ids, range.from, range.to)
            withContext(Dispatchers.IO) {
                val out = getApplication<Application>().contentResolver.openOutputStream(uri, "wt")
                    ?: error("Não foi possível abrir o arquivo de destino")
                CsvWriter.write(records, out)
            }
            ExportState.Done(records.size)
        }.getOrElse { ExportState.Failed(it.message ?: "Erro desconhecido") }
        pendingIds = null
    }

    fun reset() { _state.value = ExportState.Idle }

    companion object {
        fun suggestedFileName(): String =
            "keyhistory_" + DateUtils.formatDate(System.currentTimeMillis()).replace("/", "-") + ".csv"
    }
}
