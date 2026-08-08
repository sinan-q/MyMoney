package com.sinxn.mymoney.core.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sinxn.mymoney.core.data.repository.RecurrenceRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class RecurrenceWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface RecurrenceWorkerEntryPoint {
        fun recurrenceRepository(): RecurrenceRepository
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                RecurrenceWorkerEntryPoint::class.java
            )
            entryPoint.recurrenceRepository().processPendingRecurrences()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
