package com.ammar.wallflow.utils

import android.content.Context
import android.content.Intent
import com.ammar.wallflow.activities.crashreport.CrashReportActivity
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.google.auto.service.AutoService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.File
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.acra.config.CoreConfiguration
import org.acra.interaction.ReportInteraction

/**
 * Modified from
 * https://github.com/ACRA/acra/blob/master/acra-dialog/src/main/java/org/acra/interaction/DialogInteraction.kt
 */
@AutoService(ReportInteraction::class)
class CrashReportInteraction : ReportInteraction {
    override fun performInteraction(
        context: Context,
        config: CoreConfiguration,
        reportFile: File,
    ): Boolean {
        val appPreferencesRepository = getAppPreferencesRepository(context)
        val enabled = runBlocking {
            appPreferencesRepository.appPreferencesFlow
                .firstOrNull()
                ?.acraEnabled
                ?: true
        }
        if (!enabled) {
            CrashReportHelper.cancelReports(context)
            return false
        }
        // Create a new activity task with the confirmation dialog.
        // This new task will be persisted on application restart
        // right after its death.
        val intent = createCrashReportDialogIntent(
            context = context,
            config = config,
            reportFile = reportFile,
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return false
    }

    private fun getAppPreferencesRepository(context: Context): AppPreferencesRepository {
        val entryPoint = EntryPointAccessors.fromApplication(
            context,
            CrashReportInteractionEntryPoint::class.java,
        )
        return entryPoint.provideAppPreferencesRepository()
    }

    /**
     * Creates an Intent that can be used to create and show a CrashReportDialog.
     *
     * @param reportFile Error report file to display in the crash report dialog.
     */
    private fun createCrashReportDialogIntent(
        context: Context,
        config: CoreConfiguration,
        reportFile: File,
    ) = Intent(context, CrashReportActivity::class.java).apply {
        putExtra(EXTRA_REPORT_FILE, reportFile)
        putExtra(EXTRA_REPORT_CONFIG, config)
    }

    companion object {
        /**
         * Used in the intent starting CrashReportDialog to provide the name of the
         * latest generated report file in order to be able to associate the user
         * comment.
         */
        const val EXTRA_REPORT_FILE = "REPORT_FILE"

        /**
         * Used in the intent starting CrashReportDialog to provide the AcraConfig to use when gathering the crash info.
         *
         *
         * This can be used by any BaseCrashReportDialog subclass to custom the dialog.
         */
        const val EXTRA_REPORT_CONFIG = "REPORT_CONFIG"
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CrashReportInteractionEntryPoint {
    fun provideAppPreferencesRepository(): AppPreferencesRepository
}
