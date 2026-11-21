package com.keyhistory.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keyhistory.BuildConfig
import com.keyhistory.components.ConfirmDialog
import com.keyhistory.components.ConsentDialog
import com.keyhistory.components.IconBadge
import com.keyhistory.components.KhCard
import com.keyhistory.components.KhColors
import com.keyhistory.components.KhTopBar
import com.keyhistory.components.OnResume
import com.keyhistory.components.SectionTitle
import com.keyhistory.navigation.Routes
import com.keyhistory.utils.DateUtils
import com.keyhistory.utils.KeyboardStatus
import com.keyhistory.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit, vm: SettingsViewModel = viewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val kb by vm.keyboard.collectAsStateWithLifecycle()
    val storage by vm.storage.collectAsStateWithLifecycle()
    val total by vm.total.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var consent by rememberSaveable { mutableStateOf(false) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    OnResume { vm.refresh() }

    Scaffold(containerColor = KhColors.Background, topBar = { KhTopBar("Configurações", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Indicador de privacidade
            KhCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Outlined.Shield, KhColors.Green)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Privacidade protegida", style = MaterialTheme.typography.titleMedium)
                        Text("100% local · sem permissão de internet", color = KhColors.TextMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(10.dp))
                PrivacyLine("Criptografia AES-256-GCM", if (vm.encryptionOk) "Operacional" else "Falha", vm.encryptionOk)
                PrivacyLine("Chaves no Android Keystore", "Sim", true)
                PrivacyLine("Campos de senha/PIN", "Nunca registrados", true)
                PrivacyLine("Backup em nuvem", "Desativado", true)
            }

            SectionTitle("Registro")
            KhCard(padding = 4) {
                ToggleRow(Icons.Outlined.RadioButtonChecked, "Ativar registro", "Consentimento para registrar textos digitados", s.recordingEnabled) {
                    if (it && !s.recordingEnabled) consent = true else vm.setRecording(it)
                }
                ToggleRow(Icons.Outlined.PauseCircle, "Pausar registro", "Também disponível no próprio teclado", s.recordingPaused, enabled = s.recordingEnabled) {
                    vm.setPaused(it)
                }
                ToggleRow(Icons.Outlined.Save, "Salvar ao concluir o campo", "Desative para salvar apenas pelo botão do teclado", s.autoSave) {
                    vm.setAutoSave(it)
                }
                ToggleRow(Icons.Outlined.TouchApp, "Vibração ao digitar", null, s.hapticFeedback) { vm.setHaptic(it) }
            }

            SectionTitle("Teclado")
            KhCard(padding = 4) {
                NavRow(Icons.Outlined.Keyboard, "Ativar nas configurações do Android", if (kb.enabled) "Ativado" else "Não ativado") {
                    KeyboardStatus.openInputMethodSettings(context)
                }
                NavRow(Icons.Outlined.SwapHoriz, "Selecionar teclado", if (kb.selected) "KeyHistory em uso" else "Outro teclado em uso") {
                    KeyboardStatus.showPicker(context)
                }
            }

            SectionTitle("Dados")
            KhCard(padding = 4) {
                NavRow(Icons.Outlined.Storage, "Gerenciar armazenamento", "$total registro(s) · ${DateUtils.formatBytes(storage)}") { onNavigate(Routes.STORAGE) }
                NavRow(Icons.Outlined.FileDownload, "Exportar histórico", "CSV pelo seletor de arquivos") { onNavigate(Routes.EXPORT) }
                NavRow(Icons.Outlined.DeleteForever, "Limpar histórico", "Apagar todos os registros", tint = KhColors.Red) { confirmClear = true }
            }

            SectionTitle("Sobre")
            KhCard(padding = 4) {
                NavRow(Icons.Outlined.Policy, "Política de privacidade", null) { onNavigate(Routes.PRIVACY) }
                NavRow(Icons.Outlined.Info, "Informações sobre o aplicativo", null) { onNavigate(Routes.ABOUT) }
                NavRow(Icons.Outlined.Verified, "Versão instalada", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", showArrow = false) {}
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (consent) ConsentDialog(onAccept = { vm.setRecording(true) }, onDismiss = { consent = false })
    if (confirmClear) ConfirmDialog(
        title = "Limpar todo o histórico?",
        message = "Todos os $total registros serão apagados permanentemente. Esta ação não pode ser desfeita.",
        confirmLabel = "Apagar tudo",
        onConfirm = { vm.clearAll() },
        onDismiss = { confirmClear = false },
    )
}

@Composable
private fun PrivacyLine(label: String, value: String, ok: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, Modifier.weight(1f), color = KhColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = if (ok) KhColors.Green else KhColors.Red, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ToggleRow(
    icon: ImageVector, title: String, subtitle: String?, checked: Boolean, enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled) { onChange(!checked) }.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, if (enabled) KhColors.Primary else KhColors.TextMuted, size = 34)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = if (enabled) KhColors.Text else KhColors.TextMuted)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = KhColors.TextMuted)
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = KhColors.Primary))
    }
}

@Composable
fun NavRow(
    icon: ImageVector, title: String, subtitle: String?, tint: Color = KhColors.Primary, showArrow: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, tint, size = 34)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = if (tint == KhColors.Red) KhColors.Red else KhColors.Text)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = KhColors.TextMuted)
        }
        if (showArrow) Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = KhColors.TextMuted)
    }
}
