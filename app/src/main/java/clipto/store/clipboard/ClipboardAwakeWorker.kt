package clipto.store.clipboard

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import clipto.analytics.Analytics
import clipto.store.app.AppState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class ClipboardAwakeWorker @AssistedInject constructor(
        val appState: AppState,
        @Assisted val context: Context,
        @Assisted workerParams: WorkerParameters
) : Worker(
        context,
        workerParams) {

    override fun doWork(): Result {
        try {
            if (appState.clipboard.setValue(true)) {
                Analytics.onRestoreAfterKill()
            }
        } catch (e: Exception) {
            Analytics.onError("error_track_clipboard_aware_worker", e)
        }
        return Result.success()
    }

    companion object {
        const val ID = "TrackClipboardAwakeWorker"

        fun schedule(context: Context, interval: Long) {
            if (interval >= PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS) {
                runCatching {
                    val request = PeriodicWorkRequest.Builder(ClipboardAwakeWorker::class.java, interval, TimeUnit.MILLISECONDS)
                            .setInitialDelay(interval, TimeUnit.MILLISECONDS)
                            .build()
                    WorkManager.getInstance(context).enqueueUniquePeriodicWork(ID,
                            ExistingPeriodicWorkPolicy.KEEP,
                            request)
                }
            }
        }

        fun cancel(context: Context) {
            runCatching {
                WorkManager.getInstance(context).cancelUniqueWork(ID)
            }
        }
    }

}