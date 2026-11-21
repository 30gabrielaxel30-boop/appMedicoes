package com.keyhistory.screens

import android.app.Application
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keyhistory.components.ConfirmDialog
import com.keyhistory.components.IconBadge
import com.keyhistory.components.InfoRow
import com.keyhistory.components.KhCard
import com.keyhistory.components.KhColors
import com.keyhistory.components.KhTopBar
import com.keyhistory.components.categoryIcon
import com.keyhistory.utils.DateUtils
import com.keyhistory.viewmodel.DetailViewModel

@Composable
fun DetailScreen(id: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val vm: DetailViewModel = viewModel(key = "detail_$id", factory = DetailViewModel.factory(id, context.applicationContext as Application))
    val record by vm.record.collectAsStateWithLifecycle()
    var confirm by rememberSaveable { mutableStateOf(false) }

    Scaffold(containerColor = KhColors.Background, topBar = { KhTopBar("Detalhes do registro", onBack = onBack) }) { padding ->
        val r = record
        if (r == null) {
            Text("Registro não encontrado.", Modifier.padding(padding).padding(24.dp), color = KhColors.TextMuted)
            return@Scaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            KhCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(categoryIcon(r.category), KhColors.Primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.appLabel, style = MaterialTheme.typography.titleMedium)
                        Text(DateUtils.formatLong(r.timestamp), style = MaterialTheme.typography.labelMedium, color = KhColors.TextMuted)
                    }
                }
                Spacer(Modifier.height(16.dp))
                SelectionContainer {
                    Text(r.content, style = MaterialTheme.typography.bodyLarge)
                }
            }
            KhCard {
                InfoRow("Identificador", "#${r.id}")
                InfoRow("Data", DateUtils.formatDate(r.timestamp))
                InfoRow("Horário", DateUtils.formatTime(r.timestamp))
                InfoRow("Aplicativo", r.applicationName ?: "Indisponível")
                InfoRow("Pacote", r.packageName ?: "Indisponível")
                InfoRow("Categoria", r.category)
                InfoRow("Caracteres", r.content.length.toString())
                Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Criptografia", Modifier.weight(1f), color = KhColors.TextMuted)
                    Icon(Icons.Filled.Lock, null, tint = if (r.encrypted) KhColors.Green else KhColors.Red, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (r.encrypted) "AES-256-GCM (Android Keystore)" else "Não criptografado",
                        color = if (r.encrypted) KhColors.Green else KhColors.Red)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { copyToClipboard(context, r.content) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = KhColors.Primary),
                ) {
                    Icon(Icons.Outlined.ContentCopy, null); Spacer(Modifier.width(8.dp)); Text("Copiar")
                }
                OutlinedButton(onClick = { confirm = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Delete, null, tint = KhColors.Red); Spacer(Modifier.width(8.dp)); Text("Excluir", color = KhColors.Red)
                }
            }
        }
    }

    if (confirm) ConfirmDialog(
        title = "Excluir registro?",
        message = "Este registro será apagado permanentemente.",
        confirmLabel = "Excluir",
        onConfirm = { vm.delete(onBack) },
        onDismiss = { confirm = false },
    )
}

fun copyToClipboard(context: Context, text: String) {
    val clip = ClipData.newPlainText("KeyHistory", text)
    // Marca o conteúdo como sensível para não aparecer na pré-visualização da área de transferência.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
    } else {
        clip.description.extras = PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
    }
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) Toast.makeText(context, "Copiado", Toast.LENGTH_SHORT).show()
}
