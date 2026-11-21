package com.keyhistory.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keyhistory.components.EmptyState
import com.keyhistory.components.KhColors
import com.keyhistory.components.KhTopBar
import com.keyhistory.components.RecordItem
import com.keyhistory.utils.DateUtils
import com.keyhistory.utils.Period
import com.keyhistory.viewmodel.SearchViewModel

@Composable
fun SearchScreen(onBack: () -> Unit, onOpen: (Long) -> Unit, vm: SearchViewModel = viewModel()) {
    val filters by vm.filters.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    var showRange by rememberSaveable { mutableStateOf(false) }
    var showApps by rememberSaveable { mutableStateOf(false) }

    Scaffold(containerColor = KhColors.Background, topBar = { KhTopBar("Pesquisar", onBack = onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                OutlinedTextField(
                    value = filters.text,
                    onValueChange = vm::setText,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Palavra ou frase…") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = {
                        if (filters.text.isNotEmpty()) IconButton(onClick = { vm.setText("") }) { Icon(Icons.Outlined.Close, "Limpar") }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KhColors.Primary, unfocusedBorderColor = KhColors.Outline,
                        focusedContainerColor = KhColors.Surface, unfocusedContainerColor = KhColors.Surface,
                    ),
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = filters.exactPhrase, onClick = { vm.setExact(!filters.exactPhrase) },
                            label = { Text("Frase exata") }, leadingIcon = { Icon(Icons.Outlined.FormatQuote, null) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = KhColors.Primary),
                        )
                    }
                    item {
                        Box {
                            val appLabel = apps.firstOrNull { it.packageName == filters.packageName }?.let { it.applicationName ?: it.packageName }
                            FilterChip(
                                selected = filters.packageName != null, onClick = { showApps = true },
                                label = { Text(appLabel ?: "Aplicativo") }, leadingIcon = { Icon(Icons.Outlined.Apps, null) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = KhColors.Primary),
                            )
                            DropdownMenu(expanded = showApps, onDismissRequest = { showApps = false }) {
                                DropdownMenuItem(text = { Text("Todos os aplicativos") }, onClick = { vm.setPackage(null); showApps = false })
                                apps.forEach { a ->
                                    DropdownMenuItem(
                                        text = { Text("${a.applicationName ?: a.packageName} (${a.total})") },
                                        onClick = { vm.setPackage(a.packageName); showApps = false },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                val periods = listOf(Period.ALL, Period.TODAY, Period.YESTERDAY, Period.LAST_7, Period.LAST_30, Period.CUSTOM)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(periods) { p ->
                        val label = when {
                            p == Period.ALL -> "Qualquer data"
                            p == Period.CUSTOM && filters.period == Period.CUSTOM && filters.customRange != null ->
                                "${DateUtils.formatDayMonth(filters.customRange!!.from)} – ${DateUtils.formatDayMonth(filters.customRange!!.to)}"
                            else -> p.label
                        }
                        FilterChip(
                            selected = filters.period == p,
                            onClick = { if (p == Period.CUSTOM) showRange = true else vm.setPeriod(p) },
                            label = { Text(label) },
                            leadingIcon = if (p == Period.CUSTOM) ({ Icon(Icons.Outlined.DateRange, null) }) else null,
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = KhColors.Primary),
                        )
                    }
                }
            }
            val list = results
            when {
                list == null -> item { Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator() } }
                list.isEmpty() -> item {
                    EmptyState(Icons.Outlined.SearchOff, "Nada encontrado", "Tente outra palavra, aplicativo ou intervalo de datas.")
                }
                else -> {
                    item {
                        Text("${list.size} resultado(s)", style = MaterialTheme.typography.labelMedium, color = KhColors.TextMuted)
                        Spacer(Modifier.height(2.dp))
                    }
                    items(list, key = { it.id }) { r ->
                        RecordItem(r, selected = false, selectionMode = false, onClick = { onOpen(r.id) }, onLongClick = { onOpen(r.id) })
                    }
                }
            }
        }
    }

    if (showRange) DateRangeDialog(onConfirm = vm::setCustomRange, onDismiss = { showRange = false })
}
