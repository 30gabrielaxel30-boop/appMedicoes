package com.keyhistory.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keyhistory.components.ConfirmDialog
import com.keyhistory.components.EmptyState
import com.keyhistory.components.KhColors
import com.keyhistory.components.KhTopBar
import com.keyhistory.components.RecordItem
import com.keyhistory.components.SectionTitle
import com.keyhistory.utils.Period
import com.keyhistory.viewmodel.ExportState
import com.keyhistory.viewmodel.ExportViewModel
import com.keyhistory.viewmodel.HistoryViewModel

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    vm: HistoryViewModel = viewModel(),
    exportVm: ExportViewModel = viewModel(),
) {
    val period by vm.period.collectAsStateWithLifecycle()
    val records by vm.records.collectAsStateWithLifecycle()
    val sections by vm.sections.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val canLoadMore by vm.canLoadMore.collectAsStateWithLifecycle()
    val exportState by exportVm.state.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val selectionMode = selected.isNotEmpty()

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) exportVm.export(uri) else exportVm.pendingIds = null
    }
    LaunchedEffect(exportState) {
        when (val s = exportState) {
            is ExportState.Done -> { snackbar.showSnackbar("${s.count} registro(s) exportado(s)"); exportVm.reset(); vm.clearSelection() }
            is ExportState.Failed -> { snackbar.showSnackbar("Falha na exportação: ${s.message}"); exportVm.reset() }
            else -> Unit
        }
    }

    Scaffold(
        containerColor = KhColors.Background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (selectionMode) {
                KhTopBar("${selected.size} selecionado(s)", onBack = null) {
                    IconButton(onClick = { vm.selectAll() }) { Icon(Icons.Outlined.SelectAll, "Selecionar todos") }
                    IconButton(onClick = {
                        exportVm.pendingIds = selected.toList()
                        exportLauncher.launch(ExportViewModel.suggestedFileName())
                    }) { Icon(Icons.Outlined.FileDownload, "Exportar selecionados") }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, "Excluir selecionados", tint = KhColors.Red) }
                    IconButton(onClick = { vm.clearSelection() }) { Icon(Icons.Outlined.Close, "Cancelar seleção") }
                }
            } else KhTopBar("Histórico", onBack = onBack)
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    val periods = listOf(Period.TODAY, Period.YESTERDAY, Period.LAST_7, Period.LAST_30, Period.ALL)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                        items(periods) { p ->
                            FilterChip(
                                selected = p == period, onClick = { vm.setPeriod(p) },
                                label = { Text(if (p == Period.ALL) "Tudo" else p.label) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = KhColors.Primary),
                            )
                        }
                    }
                }
                when {
                    records == null -> item { Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { CircularProgressIndicator() } }
                    records!!.isEmpty() -> item {
                        EmptyState(Icons.Outlined.History, "Nenhum registro", "Os textos registrados pelo teclado KeyHistory aparecerão aqui.")
                    }
                    else -> {
                        sections.forEach { section ->
                            item(key = "h_${section.title}") { SectionTitle("${section.title} · ${section.records.size}") }
                            items(section.records, key = { it.id }) { r ->
                                RecordItem(
                                    record = r,
                                    selected = r.id in selected,
                                    selectionMode = selectionMode,
                                    onClick = { if (selectionMode) vm.toggle(r.id) else onOpen(r.id) },
                                    onLongClick = { vm.toggle(r.id) },
                                )
                            }
                        }
                        if (canLoadMore) item {
                            Box(Modifier.fillMaxWidth(), Alignment.Center) {
                                TextButton(onClick = { vm.loadMore() }) { Text("Carregar mais") }
                            }
                        }
                    }
                }
            }
            if (exportState is ExportState.Running) CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }

    if (confirmDelete) ConfirmDialog(
        title = "Excluir ${selected.size} registro(s)?",
        message = "Os registros selecionados serão apagados permanentemente deste aparelho.",
        confirmLabel = "Excluir",
        onConfirm = { vm.deleteSelected() },
        onDismiss = { confirmDelete = false },
    )
}
