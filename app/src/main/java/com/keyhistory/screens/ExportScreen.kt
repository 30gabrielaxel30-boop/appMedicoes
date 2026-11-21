package com.keyhistory.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keyhistory.components.IconBadge
import com.keyhistory.components.KhCard
import com.keyhistory.components.KhColors
import com.keyhistory.components.KhTopBar
import com.keyhistory.utils.CsvWriter
import com.keyhistory.utils.DateUtils
import com.keyhistory.utils.Period
import com.keyhistory.viewmodel.ExportState
import com.keyhistory.viewmodel.ExportViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportScreen(onBack: () -> Unit, vm: ExportViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val period by vm.period.collectAsStateWithLifecycle()
    val custom by vm.customRange.collectAsStateWithLifecycle()
    var showRange by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) vm.export(uri, ids = null)
    }

    Scaffold(containerColor = KhColors.Background, topBar = { KhTopBar("Exportar dados", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            KhCard {
                IconBadge(Icons.Outlined.TableChart, KhColors.Primary)
                Spacer(Modifier.height(10.dp))
                Text("Exportação CSV", style = MaterialTheme.typography.titleMedium)
                Text("Colunas: ${CsvWriter.HEADER.joinToString(", ")}. Separador ';' e codificação UTF-8 (compatível com Excel e Planilhas).",
                    color = KhColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Text("O arquivo é salvo somente no local que você escolher. Nada é enviado pela internet. O CSV não é criptografado: guarde-o com cuidado.",
                    color = KhColors.Amber, style = MaterialTheme.typography.bodySmall)
            }
            KhCard {
                Text("Período", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Period.ALL, Period.TODAY, Period.YESTERDAY, Period.LAST_7, Period.LAST_30, Period.CUSTOM).forEach { p ->
                        val label = if (p == Period.CUSTOM && custom != null && period == Period.CUSTOM)
                            "${DateUtils.formatDate(custom!!.from)} – ${DateUtils.formatDate(custom!!.to)}" else p.label
                        FilterChip(
                            selected = p == period,
                            onClick = { if (p == Period.CUSTOM) showRange = true else vm.period.value = p },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = KhColors.Primary),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("Para exportar registros específicos, selecione-os no Histórico (toque longo).",
                    style = MaterialTheme.typography.bodySmall, color = KhColors.TextMuted)
            }
            Button(
                onClick = { launcher.launch(ExportViewModel.suggestedFileName()) },
                enabled = state !is ExportState.Running,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KhColors.Primary),
            ) {
                if (state is ExportState.Running) CircularProgressIndicator(Modifier.height(20.dp), color = KhColors.Text)
                else { Icon(Icons.Outlined.FileDownload, null); Spacer(Modifier.width(8.dp)); Text("Escolher local e exportar") }
            }
            when (val s = state) {
                is ExportState.Done -> Text("✔ ${s.count} registro(s) exportado(s) com sucesso.", color = KhColors.Green)
                is ExportState.Failed -> Text("Falha na exportação: ${s.message}", color = KhColors.Red)
                else -> Unit
            }
        }
    }
    if (showRange) DateRangeDialog(onConfirm = { vm.customRange.value = it; vm.period.value = Period.CUSTOM }, onDismiss = { showRange = false })
}
