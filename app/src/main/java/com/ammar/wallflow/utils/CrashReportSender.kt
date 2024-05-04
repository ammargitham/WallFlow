package com.ammar.wallflow.utils

import android.content.Context
import androidx.compose.ui.text.SpanStyle
import com.ammar.wallflow.BUG_REPORTS_EMAIL_ADDRESS
import com.ammar.wallflow.BuildConfig
import com.ammar.wallflow.extensions.sendEmail
import com.ammar.wallflow.extensions.toAnnotatedString
import com.ammar.wallflow.extensions.toReportFieldMap
import com.google.auto.service.AutoService
import org.acra.config.CoreConfiguration
import org.acra.data.CrashReportData
import org.acra.sender.ReportSender
import org.acra.sender.ReportSenderFactory

class CrashReportSender : ReportSender {
    override fun send(context: Context, errorContent: CrashReportData) {
        val body = errorContent.toReportFieldMap().toAnnotatedString(SpanStyle()).text
        context.sendEmail(
            address = BUG_REPORTS_EMAIL_ADDRESS,
            subject = "${BuildConfig.VERSION_NAME} crash report",
            body = body,
        )
    }
}

@AutoService(ReportSenderFactory::class)
class CrashReportSenderFactory : ReportSenderFactory {
    override fun create(context: Context, config: CoreConfiguration) = CrashReportSender()
}
