package com.ammar.wallflow.extensions

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.ammar.wallflow.utils.CrashReportHelper
import org.acra.ReportField
import org.acra.data.CrashReportData

fun CrashReportData.toReportFieldMap() = mutableMapOf<ReportField, String>().apply {
    CrashReportHelper.REPORT_FIELDS.map { f ->
        this@toReportFieldMap.getString(f)?.let { v ->
            put(f, v)
        }
    }
}

fun Map<ReportField, String>.toAnnotatedString(style: SpanStyle) = buildAnnotatedString {
    withStyle(style = style) {
        entries
            .filter { it.key != ReportField.STACK_TRACE }
            .forEach {
                appendLine("${it.key.name} = ${it.value}")
            }
        appendLine()
        appendLine("Stacktrace: ")
        append(this@toAnnotatedString[ReportField.STACK_TRACE] ?: "No stacktrace")
    }
}
