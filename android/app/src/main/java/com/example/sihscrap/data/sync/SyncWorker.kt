package com.example.sihscrap.data.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.sihscrap.api.BackendApi
import com.example.sihscrap.api.RetrofitClient
import com.example.sihscrap.api.TransactionSubmissionRequest
import com.example.sihscrap.data.local.AppDatabase
import com.example.sihscrap.data.local.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"
        const val WORK_NAME = "LowBandwidthSyncWork"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }

        fun syncNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTimeRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "SyncNowWork",
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(applicationContext)
        val syncQueueDao = db.syncQueueDao()
        val transactionDao = db.transactionDao()
        val pendingItems = syncQueueDao.pendingQueue

        if (pendingItems.isEmpty()) {
            Log.d(TAG, "No pending items in sync queue.")
            return@withContext Result.success()
        }

        var anyFailed = false
        val now = System.currentTimeMillis()

        for (item in pendingItems) {
            if (now - item.lastAttemptMillis < item.backoffDelayMillis) {
                // Exponential backoff interval has not elapsed yet
                continue
            }

            try {
                val payload = CompactPayloadSerializer.deserialize(item.payloadJson)
                val submissionRequest = TransactionSubmissionRequest(
                    receiptHash = payload.receiptHash,
                    materialCode = payload.materialCode,
                    weightKg = payload.weightKg,
                    purityFactor = 1.0,
                    calculatedPrice = payload.finalPayout,
                    pickupLatitude = payload.latitude,
                    pickupLongitude = payload.longitude,
                    nonce = payload.nonce,
                    epochMillis = payload.epochMillis,
                    voiceInputTranscript = payload.voiceTranscript,
                    notes = "Offline synced via 2G/EDGE worker"
                )

                val api = RetrofitClient.instance
                val response = try {
                    api.submitTransaction(submissionRequest)
                } catch (e: Exception) {
                    api.submitTransactionFallback(submissionRequest)
                }

                val serverId = if (response.id > 0) response.id else response.transactionId ?: (item.transactionId + 1000)
                transactionDao.updateSyncStatus(
                    item.transactionId,
                    TransactionEntity.STATUS_SYNCED,
                    serverId,
                    null
                )
                syncQueueDao.delete(item)
                Log.d(TAG, "Successfully synced transaction ${item.transactionId} -> server ID: $serverId")

            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync transaction ${item.transactionId}: ${e.message}")
                anyFailed = true
                item.retryCount += 1
                item.lastAttemptMillis = now
                // Exponential backoff: 2s, 4s, 8s, 16s, up to 5 min
                val backoffSec = Math.min(2L * (1L shl Math.min(item.retryCount, 7)), 300L)
                item.backoffDelayMillis = backoffSec * 1000L
                item.lastError = e.message ?: "Network error"

                if (item.retryCount >= 5) {
                    transactionDao.updateSyncStatus(
                        item.transactionId,
                        TransactionEntity.STATUS_FAILED,
                        null,
                        "Sync failed after ${item.retryCount} attempts: ${e.message}"
                    )
                }
                syncQueueDao.update(item)
            }
        }

        if (anyFailed) Result.retry() else Result.success()
    }
}
