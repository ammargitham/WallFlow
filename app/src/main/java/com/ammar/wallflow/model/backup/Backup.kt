package com.ammar.wallflow.model.backup

import android.net.Uri

interface Backup {
    val version: Int
    fun getRestoreSummary(file: Uri): RestoreSummary
}
