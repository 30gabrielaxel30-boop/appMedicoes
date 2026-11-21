package com.keyhistory.screens

import androidx.compose.foundation.layout.height
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.keyhistory.utils.DateUtils
import com.keyhistory.utils.TimeRange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeDialog(onConfirm: (TimeRange) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDateRangePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = state.selectedStartDateMillis != null,
                onClick = {
                    val start = DateUtils.pickerUtcToLocalDate(state.selectedStartDateMillis!!)
                    val end = state.selectedEndDateMillis?.let { DateUtils.pickerUtcToLocalDate(it) } ?: start
                    onConfirm(TimeRange(DateUtils.startOf(start), DateUtils.endOf(end)))
                    onDismiss()
                },
            ) { Text("Aplicar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    ) {
        DateRangePicker(
            state = state,
            modifier = Modifier.height(480.dp),
            title = { Text("Selecione o intervalo", modifier = Modifier) },
        )
    }
}
