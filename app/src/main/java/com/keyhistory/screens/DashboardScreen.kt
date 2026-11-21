package com.keyhistory.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keyhistory.R
import com.keyhistory.components.BarChart
import com.keyhistory.components.ConsentDialog
import com.keyhistory.components.HorizontalBars
import com.keyhistory.components.IconBadge
import com.keyhistory.components.KhCard
import com.keyhistory.components.KhColors
import com.keyhistory.components.LineChart
import com.keyhistory.components.OnResume
import com.keyhistory.components.SectionTitle
import com.keyhistory.components.StatusDot
import com.keyhistory.components.StatusPill
import com.keyhistory.navigation.Routes
import com.keyhistory.utils.DateUtils
import com.keyhistory.utils.KeyboardStatus
import com.keyhistory.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(onNavigate: (String) -> Unit, vm: DashboardViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showConsent by rememberSaveable { mutableStateOf(false) }
    OnResume { vm.refresh() }

    val s = ui.settings
    val (statusText, statusColor) = when {
        !s.recordingEnabled -> "Desativado" to KhColors.TextMuted
        s.recordingPaused -> "Pausado" to KhColors.Amber
        !ui.keyboard.selected -> "Teclado inativo" to KhColors.Amber
        else -> "Ativo" to KhColors.Green
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ------------------------------------------------------- Cabeçalho
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_logo), contentDescription = "Logotipo KeyHistory", modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("KeyHistory", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(2.dp))
                StatusPill(statusText, statusColor, pulsing = statusText == "Ativo")
            }
            IconButton(onClick = { onNavigate(Routes.SETTINGS) }) {
                Icon(Icons.Outlined.Settings, contentDescription = "Configurações", tint = KhColors.Text)
            }
        }

        if (!s.recordingEnabled) {
            KhCard {
                Text("Registro desativado", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("Nada é registrado até que você ative. Ative para manter um histórico privado e criptografado do que você digita com o teclado KeyHistory.",
                    color = KhColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { showConsent = true }, colors = ButtonDefaults.buttonColors(containerColor = KhColors.Primary)) {
                    Text("Ativar registro")
                }
            }
        }

        // ------------------------------------------------------------ KPIs
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Kpi("Total de registros", ui.stats.total.toString(), Icons.Outlined.Notes, KhColors.Primary, Modifier.weight(1f))
            Kpi("Registros hoje", ui.stats.today.toString(), Icons.Outlined.Today, KhColors.Green, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Kpi("Aplicativos", ui.stats.apps.toString(), Icons.Outlined.Apps, Color(0xFFA78BFA), Modifier.weight(1f))
            Kpi("Armazenamento", DateUtils.formatBytes(ui.stats.storageBytes), Icons.Outlined.Storage, KhColors.Amber, Modifier.weight(1f))
        }

        // ------------------------------------------------- Status do teclado
        KhCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Outlined.Keyboard, KhColors.Primary)
                Spacer(Modifier.width(12.dp))
                Text("Status do teclado", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            StatusLine("Teclado instalado", ui.keyboard.installed)
            StatusLine("Teclado ativado no sistema", ui.keyboard.enabled)
            StatusLine("Teclado selecionado", ui.keyboard.selected)
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusDot(if (s.isRecordingActive) KhColors.Green else if (s.recordingEnabled) KhColors.Amber else KhColors.TextMuted, s.isRecordingActive)
                Spacer(Modifier.width(10.dp))
                Text("Registro", Modifier.weight(1f), color = KhColors.TextMuted)
                Text(if (!s.recordingEnabled) "Desativado" else if (s.recordingPaused) "Pausado" else "Ativado", fontWeight = FontWeight.Medium)
                if (s.recordingEnabled) {
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = !s.recordingPaused, onCheckedChange = { vm.setPaused(!it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = KhColors.Green),
                        modifier = Modifier.height(28.dp),
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text("Última atividade", Modifier.weight(1f), color = KhColors.TextMuted)
                Text(ui.stats.lastActivity?.let { "${DateUtils.formatDate(it)} ${DateUtils.formatShortTime(it)}" } ?: "Nenhuma",
                    fontWeight = FontWeight.Medium)
            }
            if (!ui.keyboard.enabled || !ui.keyboard.selected) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!ui.keyboard.enabled) {
                        Button(onClick = { KeyboardStatus.openInputMethodSettings(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = KhColors.Primary)) { Text("1. Ativar teclado") }
                    }
                    OutlinedButton(onClick = { KeyboardStatus.showPicker(context) }, enabled = ui.keyboard.enabled) {
                        Text(if (ui.keyboard.enabled) "Selecionar teclado" else "2. Selecionar")
                    }
                }
            }
        }

        // ---------------------------------------------------------- Atalhos
        SectionTitle("Atalhos")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Shortcut("Histórico", Icons.Outlined.History, Modifier.weight(1f)) { onNavigate(Routes.HISTORY) }
            Shortcut("Pesquisar", Icons.Outlined.Search, Modifier.weight(1f)) { onNavigate(Routes.SEARCH) }
            Shortcut("Exportar", Icons.Outlined.FileDownload, Modifier.weight(1f)) { onNavigate(Routes.EXPORT) }
            Shortcut("Ajustes", Icons.Outlined.Settings, Modifier.weight(1f)) { onNavigate(Routes.SETTINGS) }
        }

        // --------------------------------------------------------- Gráficos
        SectionTitle("Atividade")
        KhCard {
            Text("Registros por dia", style = MaterialTheme.typography.titleMedium)
            Text("Últimos 7 dias", style = MaterialTheme.typography.labelMedium, color = KhColors.TextMuted)
            Spacer(Modifier.height(14.dp))
            BarChart(ui.daily)
        }
        KhCard {
            Text("Atividade por aplicativo", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(14.dp))
            if (ui.apps.isEmpty()) Text("Sem dados ainda.", color = KhColors.TextMuted)
            else HorizontalBars(ui.apps)
        }
        KhCard {
            Text("Evolução semanal", style = MaterialTheme.typography.titleMedium)
            Text("Registros por semana (8 semanas)", style = MaterialTheme.typography.labelMedium, color = KhColors.TextMuted)
            Spacer(Modifier.height(10.dp))
            LineChart(ui.weekly)
        }
        Spacer(Modifier.height(8.dp))
    }

    if (showConsent) ConsentDialog(onAccept = { vm.enableRecording(true) }, onDismiss = { showConsent = false })
}

@Composable
private fun Kpi(label: String, value: String, icon: ImageVector, tint: Color, modifier: Modifier) {
    KhCard(modifier, padding = 14) {
        IconBadge(icon, tint, size = 34)
        Spacer(Modifier.height(10.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelMedium, color = KhColors.TextMuted, maxLines = 1)
    }
}

@Composable
private fun StatusLine(label: String, ok: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline, null,
            tint = if (ok) KhColors.Green else KhColors.Amber, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, Modifier.weight(1f), color = KhColors.TextMuted)
        Text(if (ok) "Sim" else "Não", fontWeight = FontWeight.Medium, color = if (ok) KhColors.Text else KhColors.Amber)
    }
}

@Composable
private fun Shortcut(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip16()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconBadge(icon, KhColors.Primary, size = 46)
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = KhColors.Text, maxLines = 1)
    }
}

private fun Modifier.clip16(): Modifier = this
    .clip(RoundedCornerShape(16.dp))
    .background(KhColors.Surface, RoundedCornerShape(16.dp))
