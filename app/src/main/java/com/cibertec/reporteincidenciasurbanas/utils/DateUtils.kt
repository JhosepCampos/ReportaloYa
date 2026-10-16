package com.cibertec.reporteincidenciasurbanas.utils

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    private val standardDateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("es", "PE"))
    private val shortDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE"))

    fun formatTimestamp(timestamp: Timestamp?): String {
        if (timestamp == null) return "Fecha no disponible"
        return standardDateFormat.format(timestamp.toDate())
    }

    fun formatDate(date: Date?): String {
        if (date == null) return "Fecha no disponible"
        return standardDateFormat.format(date)
    }

    fun formatShortDate(timestamp: Timestamp?): String {
        if (timestamp == null) return "-"
        return shortDateFormat.format(timestamp.toDate())
    }
}
