package com.cibertec.reporteincidenciasurbanas.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cibertec.reporteincidenciasurbanas.R
import com.cibertec.reporteincidenciasurbanas.data.model.IncidentStatus
import com.cibertec.reporteincidenciasurbanas.ui.theme.PrimaryBlue
import com.cibertec.reporteincidenciasurbanas.ui.theme.ReporteIncidenciasUrbanasTheme

@Composable
fun ChangeStatusDialog(
    currentStatus: String,
    onDismiss: () -> Unit,
    onConfirm: (newStatus: String, observation: String?) -> Unit
) {
    val options = listOf(
        IncidentStatus.PENDIENTE,
        IncidentStatus.EN_REVISION,
        IncidentStatus.EN_ATENCION,
        IncidentStatus.RESUELTO
    )

    var selectedStatus by remember { mutableStateOf(currentStatus) }
    var observation by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.change_status_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.change_status_instruction),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                options.forEach { status ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedStatus = status.name }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedStatus == status.name,
                            onClick = { selectedStatus = status.name },
                            colors = RadioButtonDefaults.colors(selectedColor = PrimaryBlue)
                        )
                        Text(
                            text = status.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedStatus == status.name) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = observation,
                    onValueChange = { observation = it },
                    label = { Text(stringResource(R.string.change_status_observation_label)) },
                    placeholder = { Text(stringResource(R.string.change_status_observation_placeholder)) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedStatus, observation.ifBlank { null })
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(stringResource(R.string.change_status_btn_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun ChangeStatusDialogPreview() {
    ReporteIncidenciasUrbanasTheme {
        ChangeStatusDialog(
            currentStatus = IncidentStatus.PENDIENTE.name,
            onDismiss = {},
            onConfirm = { _, _ -> }
        )
    }
}
