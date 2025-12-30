package com.sinxn.mymoney.core.data.repository

import android.net.Uri
import com.sinxn.mymoney.core.data.importBackup.BackupImporter
import javax.inject.Inject
import javax.inject.Singleton

interface BackupRepository {
    suspend fun importBackup(uri: Uri)
}

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val backupImporter: BackupImporter
) : BackupRepository {
    override suspend fun importBackup(uri: Uri) {
        backupImporter.importBackup(uri)
    }
}
