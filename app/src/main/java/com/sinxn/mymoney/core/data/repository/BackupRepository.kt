package com.sinxn.mymoney.core.data.repository

import android.net.Uri
import com.sinxn.mymoney.core.data.importBackup.BackupImporter
import com.sinxn.mymoney.core.data.importBackup.model.BackupAnalysisReport
import javax.inject.Inject
import javax.inject.Singleton

interface BackupRepository {
    suspend fun importBackup(uri: Uri)
    suspend fun analyzeBackup(uri: Uri): BackupAnalysisReport
}

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val importer: BackupImporter
) : BackupRepository {
    override suspend fun importBackup(uri: Uri) {
        importer.importBackup(uri)
    }

    override suspend fun analyzeBackup(uri: Uri): BackupAnalysisReport {
        return importer.analyzeBackup(uri)
    }
}
