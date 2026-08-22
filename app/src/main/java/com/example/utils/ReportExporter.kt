package com.example.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ReportExporter {

    fun generateCSVReport(
        context: Context,
        profile: UserProfile,
        readings: List<GlucoseReading>,
        doses: List<InsulinRecord>,
        steps: List<StepCountRecord>,
        bloodPressures: List<BloodPressureRecord>
    ): Uri? {
        try {
            val csvBuilder = StringBuilder()
            // Header
            csvBuilder.append("Date,Time,Glucose (mg/dL),Meal Context,Insulin Type,Dose (Units),BP Systolic (mmHg),BP Diastolic (mmHg),Pulse (bpm),Steps,Notes\n")

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

            // We aggregate all entries by timestamp so we can show a chronological timeline
            val timelineEntries = mutableListOf<TimelineItem>()

            readings.forEach {
                timelineEntries.add(TimelineItem(it.dateTimeMillis, "Glucose", it))
            }
            doses.forEach {
                timelineEntries.add(TimelineItem(it.dateTimeMillis, "Insulin", it))
            }
            steps.forEach {
                timelineEntries.add(TimelineItem(it.dateTimeMillis, "Steps", it))
            }
            bloodPressures.forEach {
                timelineEntries.add(TimelineItem(it.dateTimeMillis, "BP", it))
            }

            timelineEntries.sortByDescending { it.timestamp }

            timelineEntries.forEach { entry ->
                val dateStr = dateFormat.format(Date(entry.timestamp))
                val timeStr = timeFormat.format(Date(entry.timestamp))

                when (entry.type) {
                    "Glucose" -> {
                        val r = entry.data as GlucoseReading
                        csvBuilder.append("$dateStr,$timeStr,${r.readingValue},${r.mealContext},,,,,,,\"${r.notes}\"\n")
                    }
                    "Insulin" -> {
                        val d = entry.data as InsulinRecord
                        csvBuilder.append("$dateStr,$timeStr,,,,${d.insulinType},${d.doseUnits},,,,,\"${d.notes}\"\n")
                    }
                    "Steps" -> {
                        val s = entry.data as StepCountRecord
                        csvBuilder.append("$dateStr,$timeStr,,,,,,,,${s.steps},\"${s.notes}\"\n")
                    }
                    "BP" -> {
                        val bp = entry.data as BloodPressureRecord
                        csvBuilder.append("$dateStr,$timeStr,,,,,,${bp.systolic},${bp.diastolic},${bp.pulse},,\"${bp.notes}\"\n")
                    }
                }
            }

            val filename = "glucolog_report_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, filename)
            FileOutputStream(file).use { fos ->
                fos.write(csvBuilder.toString().toByteArray())
            }

            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun generatePDFReport(
        context: Context,
        profile: UserProfile,
        readings: List<GlucoseReading>,
        doses: List<InsulinRecord>,
        steps: List<StepCountRecord>,
        bloodPressures: List<BloodPressureRecord>
    ): Uri? {
        try {
            val pdfDocument = PdfDocument()
            // Standard A4 dimensions at 72 dpi: 595 x 842 points
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint()
            val textPaint = Paint().apply {
                color = Color.BLACK
                textSize = 10f
                isAntiAlias = true
            }

            val titlePaint = Paint().apply {
                color = Color.rgb(33, 150, 243) // Primary color
                textSize = 18f
                isAntiAlias = true
            }

            val subtitlePaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 12f
                isAntiAlias = true
            }

            val headerPaint = Paint().apply {
                color = Color.rgb(230, 240, 250)
                style = Paint.Style.FILL
            }

            val linePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 1f
            }

            var y = 40f

            // Report Header
            canvas.drawText("GLUCOLOG CLINICAL REPORT", 40f, y, titlePaint)
            y += 20f
            val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            canvas.drawText("Generated on: $nowStr", 40f, y, subtitlePaint)
            y += 25f

            // Patient Info Section
            canvas.drawRect(40f, y, 555f, y + 80f, headerPaint)
            canvas.drawRect(40f, y, 555f, y + 80f, Paint().apply { color = Color.LTGRAY; style = Paint.Style.STROKE; strokeWidth = 1f })
            
            textPaint.textSize = 11f
            textPaint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            canvas.drawText("PATIENT METADATA & PROFILE", 50f, y + 20f, textPaint)
            
            textPaint.typeface = android.graphics.Typeface.DEFAULT
            canvas.drawText("Name: ${profile.userName.ifEmpty { "Guest Patient" }}", 50f, y + 40f, textPaint)
            canvas.drawText("Target Range: ${profile.targetGlucoseMin} - ${profile.targetGlucoseMax} ${profile.glucoseUnit}", 50f, y + 60f, textPaint)
            
            canvas.drawText("Doctor Name: ${profile.doctorName.ifEmpty { "Not set" }}", 300f, y + 40f, textPaint)
            canvas.drawText("Doctor Phone: ${profile.doctorPhone.ifEmpty { "Not set" }}", 300f, y + 60f, textPaint)

            y += 105f

            // Summary Stats Section
            val avgGlucose = if (readings.isNotEmpty()) readings.map { it.readingValue }.average() else 0.0
            val totalInsulin = doses.sumOf { it.doseUnits }
            val avgSteps = if (steps.isNotEmpty()) steps.map { it.steps }.average() else 0.0

            textPaint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            canvas.drawText("CLINICAL SUMMARY (PAST LOGS)", 40f, y, textPaint)
            y += 10f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 20f

            textPaint.typeface = android.graphics.Typeface.DEFAULT
            canvas.drawText("Avg Glucose: %.1f %s".format(Locale.US, avgGlucose, profile.glucoseUnit), 40f, y, textPaint)
            canvas.drawText("Total Insulin: %.1f Units".format(Locale.US, totalInsulin), 220f, y, textPaint)
            canvas.drawText("Avg Daily Steps: %d steps".format(Locale.US, avgSteps.toInt()), 400f, y, textPaint)

            y += 30f

            // Data Table
            textPaint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            canvas.drawText("CHRONOLOGICAL ACTIVITY TIMELINE", 40f, y, textPaint)
            y += 10f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 15f

            // Table Header Background
            canvas.drawRect(40f, y - 10f, 555f, y + 10f, headerPaint)
            textPaint.textSize = 9f
            canvas.drawText("Date/Time", 45f, y + 3f, textPaint)
            canvas.drawText("Type", 150f, y + 3f, textPaint)
            canvas.drawText("Value/Dose", 240f, y + 3f, textPaint)
            canvas.drawText("Metadata/Meal", 340f, y + 3f, textPaint)
            canvas.drawText("Notes", 440f, y + 3f, textPaint)

            y += 22f
            textPaint.typeface = android.graphics.Typeface.DEFAULT

            val timelineEntries = mutableListOf<TimelineItem>()
            readings.forEach { timelineEntries.add(TimelineItem(it.dateTimeMillis, "Glucose", it)) }
            doses.forEach { timelineEntries.add(TimelineItem(it.dateTimeMillis, "Insulin", it)) }
            steps.forEach { timelineEntries.add(TimelineItem(it.dateTimeMillis, "Steps", it)) }
            bloodPressures.forEach { timelineEntries.add(TimelineItem(it.dateTimeMillis, "BP", it)) }

            timelineEntries.sortByDescending { it.timestamp }

            // Take the 20 most recent entries to fit nicely on the A4 single page report
            val recentEntries = timelineEntries.take(20)

            val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

            recentEntries.forEach { entry ->
                canvas.drawLine(40f, y - 10f, 555f, y - 10f, Paint().apply { color = Color.rgb(240, 240, 240); strokeWidth = 1f })
                
                val dateStr = dateFormat.format(Date(entry.timestamp))
                canvas.drawText(dateStr, 45f, y, textPaint)

                when (entry.type) {
                    "Glucose" -> {
                        val r = entry.data as GlucoseReading
                        canvas.drawText("Blood Sugar", 150f, y, textPaint)
                        canvas.drawText("%.1f %s".format(Locale.US, r.readingValue, profile.glucoseUnit), 240f, y, textPaint)
                        canvas.drawText(r.mealContext, 340f, y, textPaint)
                        canvas.drawText(r.notes.take(15), 440f, y, textPaint)
                    }
                    "Insulin" -> {
                        val d = entry.data as InsulinRecord
                        canvas.drawText("Insulin", 150f, y, textPaint)
                        canvas.drawText("%.1f U".format(Locale.US, d.doseUnits), 240f, y, textPaint)
                        canvas.drawText(d.insulinType, 340f, y, textPaint)
                        canvas.drawText(d.notes.take(15), 440f, y, textPaint)
                    }
                    "Steps" -> {
                        val s = entry.data as StepCountRecord
                        canvas.drawText("Steps Logged", 150f, y, textPaint)
                        canvas.drawText("${s.steps} steps", 240f, y, textPaint)
                        canvas.drawText("", 340f, y, textPaint)
                        canvas.drawText(s.notes.take(15), 440f, y, textPaint)
                    }
                    "BP" -> {
                        val bp = entry.data as BloodPressureRecord
                        canvas.drawText("Blood Pressure", 150f, y, textPaint)
                        canvas.drawText("${bp.systolic}/${bp.diastolic} mmHg", 240f, y, textPaint)
                        canvas.drawText("Pulse: ${bp.pulse} bpm", 340f, y, textPaint)
                        canvas.drawText(bp.notes.take(15), 440f, y, textPaint)
                    }
                }
                y += 20f
            }

            // Footer
            y = 810f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            textPaint.textSize = 8f
            textPaint.color = Color.GRAY
            canvas.drawText("GlucoLog Smart Clinical Summary - Confident Diabetes Management.", 40f, y + 15f, textPaint)

            pdfDocument.finishPage(page)

            val filename = "glucolog_report_${System.currentTimeMillis()}.pdf"
            val file = File(context.cacheDir, filename)
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()

            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private class TimelineItem(val timestamp: Long, val type: String, val data: Any)
}
