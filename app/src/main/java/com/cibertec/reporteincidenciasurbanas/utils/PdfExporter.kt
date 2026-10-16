package com.cibertec.reporteincidenciasurbanas.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.cibertec.reporteincidenciasurbanas.R
import com.cibertec.reporteincidenciasurbanas.data.model.Incident
import com.cibertec.reporteincidenciasurbanas.data.model.IncidentStatus
import com.cibertec.reporteincidenciasurbanas.data.model.User
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2)

    private const val COLOR_PRIMARY_DARK = 0xFF0D47A1.toInt()
    private const val COLOR_PRIMARY = 0xFF1565C0.toInt()
    private const val COLOR_PRIMARY_LIGHT = 0xFFE3F2FD.toInt()
    private const val COLOR_TEXT_MAIN = 0xFF1E293B.toInt()
    private const val COLOR_TEXT_MUTED = 0xFF64748B.toInt()
    private const val COLOR_BG_CARD = 0xFFF8FAFC.toInt()
    private const val COLOR_BORDER = 0xFFE2E8F0.toInt()
    private const val COLOR_WHITE = 0xFFFFFFFF.toInt()

    private const val COLOR_PENDING_BG = 0xFFFFF8E1.toInt()
    private const val COLOR_PENDING_TEXT = 0xFFB78103.toInt()
    private const val COLOR_IN_REVIEW_BG = 0xFFE3F2FD.toInt()
    private const val COLOR_IN_REVIEW_TEXT = 0xFF1565C0.toInt()
    private const val COLOR_IN_PROGRESS_BG = 0xFFF3E8FF.toInt()
    private const val COLOR_IN_PROGRESS_TEXT = 0xFF7E22CE.toInt()
    private const val COLOR_RESOLVED_BG = 0xFFDCFCE7.toInt()
    private const val COLOR_RESOLVED_TEXT = 0xFF15803D.toInt()

    fun generateSingleIncidentPdf(
        context: Context,
        incident: Incident,
        currentUser: User? = null
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        var currentY = MARGIN

        currentY = drawHeaderBanner(
            canvas = canvas,
            title = context.getString(R.string.pdf_header_system_title),
            subtitle = context.getString(R.string.pdf_header_single_subtitle),
            startY = currentY
        )

        currentY += 16f

        val cardHeight = 65f
        drawRoundedBox(canvas, MARGIN, currentY, CONTENT_WIDTH, cardHeight, COLOR_BG_CARD, COLOR_BORDER)

        val textPaint = Paint().apply {
            isAntiAlias = true
            color = COLOR_PRIMARY_DARK
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val defaultIncidentCode = context.getString(R.string.pdf_default_incident_prefix)
        canvas.drawText(incident.codigo.ifBlank { defaultIncidentCode }, MARGIN + 14f, currentY + 28f, textPaint)

        textPaint.apply {
            color = COLOR_TEXT_MUTED
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText(context.getString(R.string.pdf_category_prefix, incident.categoriaNombre), MARGIN + 14f, currentY + 48f, textPaint)

        drawStatusPill(
            canvas = canvas,
            context = context,
            status = incident.estado,
            rightX = MARGIN + CONTENT_WIDTH - 14f,
            centerY = currentY + (cardHeight / 2f)
        )

        currentY += cardHeight + 16f

        currentY = drawSectionHeader(canvas, context.getString(R.string.pdf_section_general_info), currentY)

        val infoBoxHeight = 70f
        drawRoundedBox(canvas, MARGIN, currentY, CONTENT_WIDTH, infoBoxHeight, COLOR_WHITE, COLOR_BORDER)

        val labelPaint = Paint().apply {
            isAntiAlias = true
            color = COLOR_TEXT_MUTED
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val valuePaint = Paint().apply {
            isAntiAlias = true
            color = COLOR_TEXT_MAIN
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val col1X = MARGIN + 14f
        val col2X = MARGIN + (CONTENT_WIDTH / 2f) + 8f

        canvas.drawText(context.getString(R.string.pdf_field_citizen_reporter), col1X, currentY + 20f, labelPaint)
        val defaultCitizenName = context.getString(R.string.pdf_default_citizen)
        val citizenName = incident.usuarioNombre.ifBlank { currentUser?.nombreCompleto ?: defaultCitizenName }
        canvas.drawText(citizenName, col1X, currentY + 34f, valuePaint)

        canvas.drawText(context.getString(R.string.pdf_field_creation_date), col2X, currentY + 20f, labelPaint)
        canvas.drawText(DateUtils.formatTimestamp(incident.fechaCreacion), col2X, currentY + 34f, valuePaint)

        canvas.drawText(context.getString(R.string.pdf_field_tracking_code), col1X, currentY + 50f, labelPaint)
        canvas.drawText(incident.codigo, col1X, currentY + 64f, valuePaint)

        canvas.drawText(context.getString(R.string.pdf_field_last_update), col2X, currentY + 50f, labelPaint)
        val updatedDate = incident.fechaActualizacion ?: incident.fechaCreacion
        canvas.drawText(DateUtils.formatTimestamp(updatedDate), col2X, currentY + 64f, valuePaint)

        currentY += infoBoxHeight + 16f

        currentY = drawSectionHeader(canvas, context.getString(R.string.pdf_section_geolocation), currentY)

        val locBoxHeight = 56f
        drawRoundedBox(canvas, MARGIN, currentY, CONTENT_WIDTH, locBoxHeight, COLOR_WHITE, COLOR_BORDER)

        canvas.drawText(context.getString(R.string.pdf_field_address), col1X, currentY + 18f, labelPaint)
        val addressText = incident.direccionReferencial.ifBlank { context.getString(R.string.pdf_default_address) }
        canvas.drawText(truncateText(addressText, 60), col1X, currentY + 32f, valuePaint)

        if (incident.latitud != 0.0 || incident.longitud != 0.0) {
            canvas.drawText(context.getString(R.string.pdf_field_gps), col1X, currentY + 44f, labelPaint)
            val coordsText = String.format(Locale.US, "Latitud: %.5f | Longitud: %.5f", incident.latitud, incident.longitud)
            canvas.drawText(coordsText, col1X + 110f, currentY + 44f, valuePaint)
        }

        currentY += locBoxHeight + 16f

        currentY = drawSectionHeader(canvas, context.getString(R.string.pdf_section_description), currentY)

        val descContent = incident.descripcion.ifBlank { context.getString(R.string.pdf_no_description) }
        val descLines = wrapText(descContent, CONTENT_WIDTH - 28f, valuePaint)
        val descBoxHeight = (descLines.size * 16f) + 24f

        drawRoundedBox(canvas, MARGIN, currentY, CONTENT_WIDTH, descBoxHeight, COLOR_WHITE, COLOR_BORDER)
        var lineY = currentY + 20f
        for (line in descLines) {
            canvas.drawText(line, col1X, lineY, valuePaint)
            lineY += 16f
        }

        currentY += descBoxHeight + 16f

        currentY = drawSectionHeader(canvas, context.getString(R.string.pdf_section_timeline), currentY)

        val historyEntries = if (incident.historialEstados.isNotEmpty()) {
            incident.historialEstados
        } else {
            emptyList()
        }

        val tableHeaderHeight = 22f
        drawRoundedBox(canvas, MARGIN, currentY, CONTENT_WIDTH, tableHeaderHeight, COLOR_PRIMARY_LIGHT, COLOR_PRIMARY)

        val thPaint = Paint().apply {
            isAntiAlias = true
            color = COLOR_PRIMARY_DARK
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val colHDate = MARGIN + 10f
        val colHStatus = MARGIN + 120f
        val colHAdmin = MARGIN + 210f
        val colHObs = MARGIN + 330f

        canvas.drawText(context.getString(R.string.pdf_th_date), colHDate, currentY + 15f, thPaint)
        canvas.drawText(context.getString(R.string.pdf_th_status), colHStatus, currentY + 15f, thPaint)
        canvas.drawText(context.getString(R.string.pdf_th_responsible), colHAdmin, currentY + 15f, thPaint)
        canvas.drawText(context.getString(R.string.pdf_th_observation), colHObs, currentY + 15f, thPaint)

        currentY += tableHeaderHeight

        val tdPaint = Paint().apply {
            isAntiAlias = true
            color = COLOR_TEXT_MAIN
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        if (historyEntries.isEmpty()) {
            val rowH = 22f
            drawRoundedBox(canvas, MARGIN, currentY, CONTENT_WIDTH, rowH, COLOR_WHITE, COLOR_BORDER)
            canvas.drawText(DateUtils.formatTimestamp(incident.fechaCreacion), colHDate, currentY + 15f, tdPaint)
            canvas.drawText(getStatusDisplayName(context, incident.estado), colHStatus, currentY + 15f, tdPaint)
            canvas.drawText(context.getString(R.string.pdf_default_citizen), colHAdmin, currentY + 15f, tdPaint)
            canvas.drawText(context.getString(R.string.pdf_default_initial_record), colHObs, currentY + 15f, tdPaint)
            currentY += rowH
        } else {
            historyEntries.forEachIndexed { index, entry ->
                val rowH = 22f
                val rowBg = if (index % 2 == 0) COLOR_WHITE else COLOR_BG_CARD
                drawRoundedBox(canvas, MARGIN, currentY, CONTENT_WIDTH, rowH, rowBg, COLOR_BORDER)

                canvas.drawText(DateUtils.formatTimestamp(entry.fecha), colHDate, currentY + 15f, tdPaint)
                canvas.drawText(getStatusDisplayName(context, entry.estado), colHStatus, currentY + 15f, tdPaint)
                val adminName = entry.nombreAdmin.ifBlank { context.getString(R.string.pdf_default_admin) }
                canvas.drawText(truncateText(adminName, 18), colHAdmin, currentY + 15f, tdPaint)
                val obsText = entry.observacion.ifBlank { context.getString(R.string.pdf_default_update) }
                canvas.drawText(truncateText(obsText, 32), colHObs, currentY + 15f, tdPaint)

                currentY += rowH
            }
        }

        drawFooter(canvas, context, 1, 1)

        pdfDocument.finishPage(page)

        val fileName = "Constancia_${incident.codigo.ifBlank { "INC" }}_${System.currentTimeMillis()}.pdf"
        return saveAndExportToDownloads(context, pdfDocument, fileName)
    }

    fun generateAllIncidentsPdf(
        context: Context,
        incidents: List<Incident>,
        currentUser: User? = null
    ): File {
        val pdfDocument = PdfDocument()
        val itemsPerPage = 6
        val totalPages = if (incidents.isEmpty()) 1 else ((incidents.size - 1) / itemsPerPage) + 1

        val isAdmin = currentUser?.esAdmin == true
        val roleLabel = if (isAdmin) context.getString(R.string.pdf_field_admin_responsible) else context.getString(R.string.pdf_field_citizen_label)
        val docTitle = if (isAdmin) context.getString(R.string.pdf_header_admin_all_title) else context.getString(R.string.pdf_header_citizen_all_title)
        val docSubtitle = if (isAdmin) context.getString(R.string.pdf_header_admin_all_subtitle) else context.getString(R.string.pdf_header_citizen_all_subtitle)

        val defaultUserLabel = if (isAdmin) context.getString(R.string.pdf_default_admin) else context.getString(R.string.pdf_default_citizen)
        val userName = currentUser?.nombreCompleto?.ifBlank { currentUser.correo } ?: defaultUserLabel
        val currentDateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            var currentY = MARGIN

            currentY = drawHeaderBanner(
                canvas = canvas,
                title = docTitle,
                subtitle = docSubtitle,
                startY = currentY
            )

            currentY += 14f

            if (pageIndex == 0) {
                val statsHeight = 60f
                drawRoundedBox(canvas, MARGIN, currentY, CONTENT_WIDTH, statsHeight, COLOR_BG_CARD, COLOR_BORDER)

                val labelPaint = Paint().apply {
                    isAntiAlias = true
                    color = COLOR_TEXT_MUTED
                    textSize = 9.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val valPaint = Paint().apply {
                    isAntiAlias = true
                    color = COLOR_TEXT_MAIN
                    textSize = 11f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                canvas.drawText(roleLabel, MARGIN + 12f, currentY + 18f, labelPaint)
                canvas.drawText(userName, MARGIN + 12f, currentY + 34f, valPaint)
                canvas.drawText(context.getString(R.string.pdf_field_issue_date, currentDateStr), MARGIN + 12f, currentY + 50f, labelPaint)

                val totalCount = incidents.size
                val pendingCount = incidents.count { it.estado == IncidentStatus.PENDIENTE.name }
                val inProgCount = incidents.count { it.estado == IncidentStatus.EN_REVISION.name || it.estado == IncidentStatus.EN_ATENCION.name }
                val resolvedCount = incidents.count { it.estado == IncidentStatus.RESUELTO.name }

                val colStatX = MARGIN + 260f
                canvas.drawText(context.getString(R.string.pdf_summary_status_title), colStatX, currentY + 18f, labelPaint)

                val statPillPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 10f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                statPillPaint.color = COLOR_PRIMARY_DARK
                canvas.drawText(context.getString(R.string.pdf_stat_total, totalCount), colStatX, currentY + 36f, statPillPaint)

                statPillPaint.color = COLOR_PENDING_TEXT
                canvas.drawText(context.getString(R.string.pdf_stat_pending, pendingCount), colStatX + 65f, currentY + 36f, statPillPaint)

                statPillPaint.color = COLOR_IN_PROGRESS_TEXT
                canvas.drawText(context.getString(R.string.pdf_stat_in_progress, inProgCount), colStatX + 155f, currentY + 36f, statPillPaint)

                statPillPaint.color = COLOR_RESOLVED_TEXT
                canvas.drawText(context.getString(R.string.pdf_stat_resolved, resolvedCount), colStatX, currentY + 50f, statPillPaint)

                currentY += statsHeight + 14f
            }

            currentY = drawSectionHeader(
                canvas = canvas,
                title = context.getString(R.string.pdf_section_reports_list, pageIndex + 1, totalPages),
                startY = currentY
            )

            val thHeight = 22f
            drawRoundedBox(canvas, MARGIN, currentY, CONTENT_WIDTH, thHeight, COLOR_PRIMARY_LIGHT, COLOR_PRIMARY)

            val thPaint = Paint().apply {
                isAntiAlias = true
                color = COLOR_PRIMARY_DARK
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val colCode = MARGIN + 8f
            val colCat = MARGIN + 85f
            val colDesc = MARGIN + 185f
            val colDate = MARGIN + 355f
            val colStatus = MARGIN + 445f

            canvas.drawText(context.getString(R.string.pdf_th_code), colCode, currentY + 15f, thPaint)
            canvas.drawText(context.getString(R.string.pdf_th_category), colCat, currentY + 15f, thPaint)
            canvas.drawText(context.getString(R.string.pdf_th_desc_location), colDesc, currentY + 15f, thPaint)
            canvas.drawText(context.getString(R.string.pdf_th_registered), colDate, currentY + 15f, thPaint)
            canvas.drawText(context.getString(R.string.pdf_th_status), colStatus, currentY + 15f, thPaint)

            currentY += thHeight

            val startIndex = pageIndex * itemsPerPage
            val endIndex = (startIndex + itemsPerPage).coerceAtMost(incidents.size)
            val pageIncidents = if (incidents.isEmpty()) emptyList() else incidents.subList(startIndex, endIndex)

            val tdPaint = Paint().apply {
                isAntiAlias = true
                color = COLOR_TEXT_MAIN
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val tdBold = Paint().apply {
                isAntiAlias = true
                color = COLOR_PRIMARY_DARK
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val tdMuted = Paint().apply {
                isAntiAlias = true
                color = COLOR_TEXT_MUTED
                textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            if (pageIncidents.isEmpty()) {
                val emptyH = 40f
                drawRoundedBox(canvas, MARGIN, currentY, CONTENT_WIDTH, emptyH, COLOR_WHITE, COLOR_BORDER)
                canvas.drawText(context.getString(R.string.pdf_no_reports_registered), MARGIN + 20f, currentY + 24f, tdPaint)
                currentY += emptyH
            } else {
                pageIncidents.forEachIndexed { i, inc ->
                    val rowH = 46f
                    val rowBg = if (i % 2 == 0) COLOR_WHITE else COLOR_BG_CARD
                    drawRoundedBox(canvas, MARGIN, currentY, CONTENT_WIDTH, rowH, rowBg, COLOR_BORDER)

                    val defPrefix = context.getString(R.string.pdf_default_incident_prefix)
                    canvas.drawText(inc.codigo.ifBlank { defPrefix }, colCode, currentY + 18f, tdBold)

                    canvas.drawText(truncateText(inc.categoriaNombre, 16), colCat, currentY + 18f, tdPaint)

                    canvas.drawText(truncateText(inc.descripcion, 32), colDesc, currentY + 16f, tdPaint)
                    val locStr = "📍 " + truncateText(inc.direccionReferencial.ifBlank { context.getString(R.string.pdf_default_address) }, 34)
                    canvas.drawText(locStr, colDesc, currentY + 32f, tdMuted)

                    canvas.drawText(DateUtils.formatTimestamp(inc.fechaCreacion), colDate, currentY + 18f, tdMuted)

                    drawStatusPill(
                        canvas = canvas,
                        context = context,
                        status = inc.estado,
                        rightX = MARGIN + CONTENT_WIDTH - 8f,
                        centerY = currentY + 22f
                    )

                    currentY += rowH + 2f
                }
            }

            drawFooter(canvas, context, pageIndex + 1, totalPages)

            pdfDocument.finishPage(page)
        }

        val prefix = if (isAdmin) "Reporte_Municipal" else "Mis_Reportes"
        val fileName = "${prefix}_${System.currentTimeMillis()}.pdf"
        return saveAndExportToDownloads(context, pdfDocument, fileName)
    }

    private fun saveAndExportToDownloads(
        context: Context,
        pdfDocument: PdfDocument,
        fileName: String
    ): File {
        val outputDir = File(context.cacheDir, "pdf_reports").apply { mkdirs() }
        val cacheFile = File(outputDir, fileName)

        FileOutputStream(cacheFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        cacheFile.inputStream().use { input ->
                            input.copyTo(os)
                        }
                    }
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (downloadsDir.exists() || downloadsDir.mkdirs()) {
                    val destFile = File(downloadsDir, fileName)
                    cacheFile.copyTo(destFile, overwrite = true)
                }
            }
        } catch (_: Exception) {
        }

        return cacheFile
    }

    fun openPdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(intent, context.getString(R.string.pdf_chooser_open_title)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.pdf_chooser_share_title)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    private fun drawHeaderBanner(
        canvas: Canvas,
        title: String,
        subtitle: String,
        startY: Float
    ): Float {
        val bannerHeight = 52f
        val paint = Paint().apply {
            isAntiAlias = true
            color = COLOR_PRIMARY
            style = Paint.Style.FILL
        }

        val rect = RectF(MARGIN, startY, MARGIN + CONTENT_WIDTH, startY + bannerHeight)
        canvas.drawRoundRect(rect, 8f, 8f, paint)

        paint.apply {
            color = COLOR_WHITE
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(title, MARGIN + 16f, startY + 24f, paint)

        paint.apply {
            color = 0xFFBBDEFB.toInt()
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText(subtitle, MARGIN + 16f, startY + 41f, paint)

        return startY + bannerHeight
    }

    private fun drawSectionHeader(canvas: Canvas, title: String, startY: Float): Float {
        val paint = Paint().apply {
            isAntiAlias = true
            color = COLOR_PRIMARY_DARK
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(title, MARGIN + 2f, startY + 12f, paint)
        return startY + 18f
    }

    private fun drawRoundedBox(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        fillColor: Int,
        strokeColor: Int
    ) {
        val rect = RectF(x, y, x + width, y + height)
        val paint = Paint().apply {
            isAntiAlias = true
            color = fillColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, 6f, 6f, paint)

        paint.apply {
            color = strokeColor
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(rect, 6f, 6f, paint)
    }

    private fun getStatusDisplayName(context: Context, status: String): String {
        return when (IncidentStatus.fromString(status)) {
            IncidentStatus.PENDIENTE -> context.getString(R.string.status_pending)
            IncidentStatus.EN_REVISION -> context.getString(R.string.status_in_review)
            IncidentStatus.EN_ATENCION -> context.getString(R.string.status_in_progress)
            IncidentStatus.RESUELTO -> context.getString(R.string.status_resolved)
        }
    }

    private fun drawStatusPill(
        canvas: Canvas,
        context: Context,
        status: String,
        rightX: Float,
        centerY: Float
    ) {
        val incidentStatus = IncidentStatus.fromString(status)
        val (bgColor, textColor) = when (incidentStatus) {
            IncidentStatus.PENDIENTE -> Pair(COLOR_PENDING_BG, COLOR_PENDING_TEXT)
            IncidentStatus.EN_REVISION -> Pair(COLOR_IN_REVIEW_BG, COLOR_IN_REVIEW_TEXT)
            IncidentStatus.EN_ATENCION -> Pair(COLOR_IN_PROGRESS_BG, COLOR_IN_PROGRESS_TEXT)
            IncidentStatus.RESUELTO -> Pair(COLOR_RESOLVED_BG, COLOR_RESOLVED_TEXT)
        }

        val pillText = getStatusDisplayName(context, status)
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = textColor
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val textWidth = textPaint.measureText(pillText)
        val pillWidth = textWidth + 24f
        val pillHeight = 20f
        val pillLeft = rightX - pillWidth
        val pillTop = centerY - (pillHeight / 2f)

        val pillRect = RectF(pillLeft, pillTop, rightX, pillTop + pillHeight)
        val bgPaint = Paint().apply {
            isAntiAlias = true
            color = bgColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(pillRect, 10f, 10f, bgPaint)

        val dotPaint = Paint().apply {
            isAntiAlias = true
            color = textColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(pillLeft + 9f, centerY, 3.5f, dotPaint)

        canvas.drawText(pillText, pillLeft + 16f, centerY + 3.5f, textPaint)
    }

    private fun drawFooter(canvas: Canvas, context: Context, currentPage: Int, totalPages: Int) {
        val footerY = PAGE_HEIGHT - MARGIN + 10f
        val linePaint = Paint().apply {
            color = COLOR_BORDER
            strokeWidth = 1f
        }
        canvas.drawLine(MARGIN, footerY - 14f, MARGIN + CONTENT_WIDTH, footerY - 14f, linePaint)

        val textPaint = Paint().apply {
            isAntiAlias = true
            color = COLOR_TEXT_MUTED
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        canvas.drawText(context.getString(R.string.pdf_footer_issued_by), MARGIN, footerY, textPaint)

        val pageStr = context.getString(R.string.pdf_footer_page_format, currentPage, totalPages)
        val pageStrWidth = textPaint.measureText(pageStr)
        canvas.drawText(pageStr, MARGIN + CONTENT_WIDTH - pageStrWidth, footerY, textPaint)
    }

    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val potentialLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(potentialLine) <= maxWidth) {
                currentLine = StringBuilder(potentialLine)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                }
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines.ifEmpty { listOf(text) }
    }

    private fun truncateText(text: String, maxChars: Int): String {
        return if (text.length > maxChars) {
            text.take(maxChars - 3) + "..."
        } else {
            text
        }
    }
}
