package com.subconverter.domain

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.subconverter.core.AppContainer
import com.subconverter.data.SubscriptionSourceEntity
import java.util.concurrent.TimeUnit

class RefreshSubscriptionWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val sourceId = inputData.getLong(KEY_SOURCE_ID, -1L)
        if (sourceId <= 0) return Result.failure()

        val container = AppContainer.get(applicationContext)
        val globalUserAgent = container.settingsStore.current().globalUserAgent
        val outcome = container.subscriptionRepository.refreshSource(sourceId, globalUserAgent)

        return if (outcome.success) Result.success() else Result.retry()
    }

    companion object {
        const val KEY_SOURCE_ID = "source_id"
    }
}

class RefreshScheduler(private val context: Context) {
    private val workManager = WorkManager.getInstance(context)

    fun reschedule(source: SubscriptionSourceEntity) {
        if (!source.autoRefreshEnabled) {
            cancel(source.id)
            return
        }

        val request = PeriodicWorkRequestBuilder<RefreshSubscriptionWorker>(
            source.refreshIntervalMinutes.coerceAtLeast(15),
            TimeUnit.MINUTES,
        )
            .setInputData(workDataOf(RefreshSubscriptionWorker.KEY_SOURCE_ID to source.id))
            .build()

        workManager.enqueueUniquePeriodicWork(
            workName(source.id),
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancel(sourceId: Long) {
        workManager.cancelUniqueWork(workName(sourceId))
    }

    private fun workName(sourceId: Long): String = "refresh-source-$sourceId"
}
