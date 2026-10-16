package com.cibertec.reporteincidenciasurbanas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cibertec.reporteincidenciasurbanas.data.model.IncidentStatus
import com.cibertec.reporteincidenciasurbanas.ui.theme.ReporteIncidenciasUrbanasTheme
import com.cibertec.reporteincidenciasurbanas.ui.theme.StatusInProgressBg
import com.cibertec.reporteincidenciasurbanas.ui.theme.StatusInProgressColor
import com.cibertec.reporteincidenciasurbanas.ui.theme.StatusInReviewBg
import com.cibertec.reporteincidenciasurbanas.ui.theme.StatusInReviewColor
import com.cibertec.reporteincidenciasurbanas.ui.theme.StatusPendingBg
import com.cibertec.reporteincidenciasurbanas.ui.theme.StatusPendingColor
import com.cibertec.reporteincidenciasurbanas.ui.theme.StatusResolvedBg
import com.cibertec.reporteincidenciasurbanas.ui.theme.StatusResolvedColor

import androidx.compose.ui.res.stringResource
import com.cibertec.reporteincidenciasurbanas.R

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val incidentStatus = IncidentStatus.fromString(status)
    val (bgColor, textColor, textRes) = when (incidentStatus) {
        IncidentStatus.PENDIENTE -> Triple(StatusPendingBg, StatusPendingColor, R.string.status_pending)
        IncidentStatus.EN_REVISION -> Triple(StatusInReviewBg, StatusInReviewColor, R.string.status_in_review)
        IncidentStatus.EN_ATENCION -> Triple(StatusInProgressBg, StatusInProgressColor, R.string.status_in_progress)
        IncidentStatus.RESUELTO -> Triple(StatusResolvedBg, StatusResolvedColor, R.string.status_resolved)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(textRes),
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StatusBadgePreview() {
    ReporteIncidenciasUrbanasTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusBadge(status = IncidentStatus.PENDIENTE.name)
            StatusBadge(status = IncidentStatus.EN_REVISION.name)
            StatusBadge(status = IncidentStatus.EN_ATENCION.name)
            StatusBadge(status = IncidentStatus.RESUELTO.name)
        }
    }
}
