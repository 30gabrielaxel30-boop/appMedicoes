package com.keyhistory.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun KhCard(modifier: Modifier = Modifier, padding: Int = 16, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = KhColors.Surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, KhColors.Outline.copy(alpha = 0.6f)),
    ) {
        Column(Modifier.padding(padding.dp), content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhTopBar(title: String, onBack: (() -> Unit)?, actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {}) {
    CenterAlignedTopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            if (onBack != null) IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = KhColors.Background),
    )
}

@Composable
fun StatusDot(color: Color, pulsing: Boolean, size: Int = 10) {
    val alpha = if (pulsing) {
        val transition = rememberInfiniteTransition(label = "pulse")
        val a by transition.animateFloat(
            initialValue = 1f, targetValue = 0.35f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "alpha"
        )
        a
    } else 1f
    Box(Modifier.size(size.dp).alpha(alpha).background(color, CircleShape))
}

@Composable
fun StatusPill(text: String, color: Color, pulsing: Boolean = false) {
    Row(
        Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(50))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(color, pulsing, 8)
        Spacer(Modifier.width(6.dp))
        Text(text, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun IconBadge(icon: ImageVector, tint: Color, modifier: Modifier = Modifier, size: Int = 38) {
    Box(
        modifier.size(size.dp).background(tint.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size((size * 0.55f).dp)) }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier.padding(top = 8.dp, bottom = 8.dp, start = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = KhColors.TextMuted,
        letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing * 1.5f,
    )
}

@Composable
fun InfoRow(label: String, value: String, valueColor: Color = KhColors.Text) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = KhColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(12.dp))
        Text(value, color = valueColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    destructive: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KhColors.Graphite,
        title = { Text(title) },
        text = { Text(message, color = KhColors.TextMuted) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirmLabel, color = if (destructive) KhColors.Red else KhColors.Primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = KhColors.TextMuted) } },
    )
}

@Composable
fun EmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        IconBadge(icon, KhColors.Primary, size = 56)
        Spacer(Modifier.padding(6.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.padding(2.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = KhColors.TextMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
