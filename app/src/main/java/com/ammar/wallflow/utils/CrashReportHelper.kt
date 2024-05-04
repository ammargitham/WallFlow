package com.ammar.wallflow.utils

/*
 * Copyright (c) 2019
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.WorkerThread
import com.ammar.wallflow.extensions.TAG
import java.io.File
import java.io.IOException
import org.acra.ReportField
import org.acra.config.CoreConfiguration
import org.acra.data.CrashReportData
import org.acra.file.BulkReportDeleter
import org.acra.file.CrashReportPersister
import org.acra.scheduler.SchedulerStarter
import org.acra.util.kGetSerializableExtra
import org.json.JSONException

/**
 * Modified from
 * https://raw.githubusercontent.com/ACRA/acra/master/acra-dialog/src/main/java/org/acra/dialog/CrashReportDialogHelper.kt
 *
 * Use this class to integrate your custom crash report dialog with ACRA.
 *
 * Call this in your [android.app.Activity.onCreate].
 * The intent must contain two extras:
 *
 *  1. [CrashReportInteraction.EXTRA_REPORT_FILE]
 *  2. [CrashReportInteraction.EXTRA_REPORT_CONFIG]
 *
 *
 * @param context a context
 * @param intent  the intent which started this activity
 * @throws IllegalArgumentException if the intent cannot be parsed or does not contain the correct data
 * @author f43nd1r
 * @since 5.4.0
 */
class CrashReportHelper(
    private val context: Context,
    intent: Intent,
) {
    private val reportFile: File

    /**
     * Provides the configuration
     *
     * @return the main config
     */
    val config: CoreConfiguration

    init {
        val sConfig = intent.kGetSerializableExtra<CoreConfiguration>(
            CrashReportInteraction.EXTRA_REPORT_CONFIG,
        )
        val sReportFile = intent.kGetSerializableExtra<File>(
            CrashReportInteraction.EXTRA_REPORT_FILE,
        )
        if (sConfig != null && sReportFile != null) {
            config = sConfig
            reportFile = sReportFile
        } else {
            Log.e(TAG, "Illegal or incomplete call of " + javaClass.simpleName)
            throw IllegalArgumentException()
        }
    }

    /**
     * loads the current report data
     *
     * @return report data
     * @throws IOException if there was a problem with the report file
     */
    @get:Throws(IOException::class)
    @get:WorkerThread
    val reportData: CrashReportData by lazy {
        try {
            CrashReportPersister().load(reportFile)
        } catch (e: JSONException) {
            throw IOException(e)
        }
    }

    /**
     * Cancel any pending crash reports.
     */
    fun cancelReports() {
        cancelReports(context)
    }

    /**
     * Send crash report given user's comment and email address.
     *
     * @param comment   Comment (may be null) provided by the user.
     * @param userEmail Email address (may be null) provided by the user.
     */
    fun sendCrash() {
        Thread {
            try {
                CrashReportPersister().store(reportData, reportFile)
            } catch (e: Exception) {
                Log.w(TAG, "sendCrash: ", e)
            }
            SchedulerStarter(
                context = context,
                config = config,
            ).scheduleReports(
                report = reportFile,
                onlySendSilentReports = false,
            )
        }.start()
    }

    companion object {
        val REPORT_FIELDS = listOf(
            // ReportField.REPORT_ID,
            ReportField.APP_VERSION_CODE,
            ReportField.APP_VERSION_NAME,
            ReportField.PACKAGE_NAME,
            // ReportField.FILE_PATH,
            ReportField.PHONE_MODEL,
            ReportField.ANDROID_VERSION,
            // ReportField.BUILD,
            ReportField.BRAND,
            ReportField.PRODUCT,
            // ReportField.TOTAL_MEM_SIZE,
            // ReportField.AVAILABLE_MEM_SIZE,
            // ReportField.BUILD_CONFIG,
            // ReportField.CUSTOM_DATA,
            ReportField.STACK_TRACE,
            // ReportField.STACK_TRACE_HASH,
            // ReportField.CRASH_CONFIGURATION,
            // ReportField.USER_COMMENT,
            // ReportField.USER_APP_START_DATE,
            // ReportField.USER_CRASH_DATE,
            // ReportField.LOGCAT,
            // ReportField.ENVIRONMENT,
            // ReportField.SHARED_PREFERENCES,
        )

        /**
         * Cancel any pending crash reports.
         */
        fun cancelReports(context: Context) {
            Thread {
                BulkReportDeleter(context).deleteReports(false, 0)
            }.start()
        }
    }
}
