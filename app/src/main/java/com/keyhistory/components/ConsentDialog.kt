package com.keyhistory.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
private fun Bullet(ok: Boolean, text: String) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Icon(if (ok) Icons.Filled.Check else Icons.Filled.Close, null,
            tint = if (ok) KhColors.Green else KhColors.Red, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = KhColors.Text)
    }
}

/** Termo de consentimento exibido antes de ativar o registro. */
@Composable
fun ConsentDialog(onAccept: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KhColors.Graphite,
        icon = { Icon(Icons.Outlined.Shield, null, tint = KhColors.Primary) },
        title = { Text("Ativar o registro de textos?") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("O KeyHistory registrará apenas o que VOCÊ digitar com o teclado KeyHistory:",
                    color = KhColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.padding(4.dp))
                Bullet(true, "Somente campos de texto comuns")
                Bullet(true, "Armazenado apenas neste aparelho, criptografado (AES-256-GCM)")
                Bullet(true, "Indicador visível no teclado e botão para pausar a qualquer momento")
                Bullet(false, "Senhas, PINs, códigos e campos numéricos")
                Bullet(false, "Campos marcados como privados/anônimos e origens não verificadas")
                Bullet(false, "Números de cartão, CPF/CNPJ e tokens")
                Bullet(false, "Nenhum envio pela internet (o app não possui permissão de rede)")
                Spacer(Modifier.padding(4.dp))
                Text("Use apenas no seu próprio aparelho. Você pode desativar e apagar tudo quando quiser.",
                    color = KhColors.TextMuted, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = { onAccept(); onDismiss() },
                colors = ButtonDefaults.buttonColors(containerColor = KhColors.Primary)) { Text("Concordo e ativar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Agora não", color = Color(0xFF8A94A6)) } },
    )
}
