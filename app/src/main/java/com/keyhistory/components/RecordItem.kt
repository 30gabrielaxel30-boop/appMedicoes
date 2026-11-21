package com.keyhistory.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShortText
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keyhistory.keyboard.CategoryResolver
import com.keyhistory.repository.HistoryRecord
import com.keyhistory.utils.DateUtils

fun categoryIcon(category: String): ImageVector = when (category) {
    CategoryResolver.MESSAGE -> Icons.Outlined.Sms
    CategoryResolver.SEARCH -> Icons.Outlined.Search
    CategoryResolver.EMAIL -> Icons.Outlined.Email
    CategoryResolver.WEB -> Icons.Outlined.Language
    CategoryResolver.NOTE -> Icons.Outlined.Notes
    CategoryResolver.TEXT -> Icons.Outlined.ShortText
    else -> Icons.Outlined.Apps
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecordItem(
    record: HistoryRecord,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val border = if (selected) KhColors.Primary else KhColors.Outline.copy(alpha = 0.5f)
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (selected) KhColors.PrimarySoft.copy(alpha = 0.35f) else KhColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (selectionMode) {
            Icon(
                if (selected) Icons.Filled.CheckCircle else Icons.Outlined.Apps,
                contentDescription = if (selected) "Selecionado" else "Não selecionado",
                tint = if (selected) KhColors.Primary else KhColors.Outline,
                modifier = Modifier.size(22.dp).padding(top = 2.dp),
            )
        } else {
            IconBadge(categoryIcon(record.category), KhColors.Primary, size = 34)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(record.appLabel, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (record.encrypted) {
                    Icon(Icons.Filled.Lock, contentDescription = "Criptografado", tint = KhColors.Green, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(DateUtils.formatShortTime(record.timestamp), style = MaterialTheme.typography.labelMedium, color = KhColors.TextMuted)
            }
            Spacer(Modifier.padding(top = 4.dp))
            Text(record.content, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis,
                color = if (record.decryptionFailed) KhColors.Red else KhColors.Text)
            Spacer(Modifier.padding(top = 6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    record.category,
                    Modifier.background(KhColors.Graphite, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall, color = KhColors.TextMuted,
                )
                Text(DateUtils.formatDate(record.timestamp), style = MaterialTheme.typography.labelSmall, color = KhColors.TextMuted)
                Text("#${record.id}", style = MaterialTheme.typography.labelSmall, color = KhColors.Outline, fontWeight = FontWeight.Medium)
            }
        }
    }
}
