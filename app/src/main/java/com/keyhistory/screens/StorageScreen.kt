package com.keyhistory.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keyhistory.components.ChartLegendValue
import com.keyhistory.components.ConfirmDialog
import com.keyhistory.components.KhCard
import com.keyhistory.components.KhColors
import com.keyhistory.components.KhTopBar
import com.keyhistory.components.OnResume
import com.keyhistory.components.ProgressBar
import com.keyhistory.components.SectionTitle
import com.keyhistory.utils.DateUtils
import com.keyhistory.viewmodel.SettingsViewModel

private sealed interface PendingDelete {
    data class Older(val days: Int) : PendingDelete
    data class App(val pkg: String, val label: String) : PendingDelete
    data object All : PendingDelete
}

@Composable
fun StorageScreen(onBack: () -> Unit, vm: SettingsViewModel = viewModel()) {
    val storage by vm.storage.collectAsStateWithLifecycle()
    val total by vm.total.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var pending by remember { mutableStateOf<PendingDelete?>(null) }
    OnResume { vm.refresh() }

    Scaffold(containerColor = KhColors.Background, topBar = { KhTopBar("Armazenamento", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KhCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ChartLegendValue("Registros", total.toString())
                    ChartLegendValue("Banco de dados", DateUtils.formatBytes(storage))
                    ChartLegendValue("Aplicativos", apps.size.toString())
                }
                Spacer(Modifier.height(12.dp))
                ProgressBar((storage / (50f * 1024 * 1024)).coerceAtMost(1f), KhColors.Primary)
                Spacer(Modifier.height(4.dp))
                Text("Armazenado somente neste aparelho, em área privada do aplicativo.",
                    style = MaterialTheme.typography.bodySmall, color = KhColors.TextMuted)
            }

            SectionTitle("Limpeza")
            KhCard {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pending = PendingDelete.Older(30) }, Modifier.weight(1f)) { Text("> 30 dias") }
                    OutlinedButton(onClick = { pending = PendingDelete.Older(90) }, Modifier.weight(1f)) { Text("> 90 dias") }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { pending = PendingDelete.All }, Modifier.fillMaxWidth()) {
                    Text("Apagar todo o histórico", color = KhColors.Red)
                }
            }

            SectionTitle("Aplicativos")
            KhCard(padding = 8) {
                if (apps.isEmpty()) Text("Nenhum aplicativo registrado ainda.", Modifier.padding(8.dp), color = KhColors.TextMuted)
                apps.forEachIndexed { i, a ->
                    val label = a.applicationName ?: a.packageName
                    val excluded = a.packageName in settings.excludedPackages
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                            Text("${a.total} registro(s) · ${if (excluded) "não registrar" else "registro permitido"}",
                                style = MaterialTheme.typography.bodySmall, color = if (excluded) KhColors.Amber else KhColors.TextMuted)
                        }
                        Checkbox(
                            checked = !excluded, onCheckedChange = { vm.setExcluded(a.packageName, !it) },
                            colors = CheckboxDefaults.colors(checkedColor = KhColors.Primary),
                        )
                        IconButton(onClick = { pending = PendingDelete.App(a.packageName, label) }) {
                            Icon(Icons.Outlined.Delete, "Apagar registros de $label", tint = KhColors.Red)
                        }
                    }
                    if (i < apps.lastIndex) HorizontalDivider(color = KhColors.Outline.copy(alpha = 0.5f))
                }
            }
            Text("Desmarque um aplicativo para que o teclado nunca registre textos digitados nele.",
                style = MaterialTheme.typography.bodySmall, color = KhColors.TextMuted)
        }
    }

    when (val p = pending) {
        is PendingDelete.Older -> ConfirmDialog("Apagar registros antigos?", "Registros com mais de ${p.days} dias serão apagados.",
            "Apagar", onConfirm = { vm.deleteOlderThan(p.days) }, onDismiss = { pending = null })
        is PendingDelete.App -> ConfirmDialog("Apagar registros de ${p.label}?", "Todos os registros deste aplicativo serão apagados.",
            "Apagar", onConfirm = { vm.deleteApp(p.pkg) }, onDismiss = { pending = null })
        PendingDelete.All -> ConfirmDialog("Apagar todo o histórico?", "Todos os $total registros serão apagados permanentemente.",
            "Apagar tudo", onConfirm = { vm.clearAll() }, onDismiss = { pending = null })
        null -> Unit
    }
}
