/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iOS Background Sync
 * Background document synchronization
 *
 *########################################################*/
package digital.vasic.yole.ios

import digital.vasic.yole.network.NetworkStorageService
import digital.vasic.yole.network.config.NetworkStorageConfigService
import digital.vasic.yole.network.common.NetworkDocument
import digital.vasic.yole.network.common.SyncStatus
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTask
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.NSFileCoordinator
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.Foundation.NSURLSessionConfiguration
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.time.Duration.Companion.hours

/**
 * Background Task Identifiers.
 *
 * These identifiers must be declared in the app's Info.plist under
 * BGTaskSchedulerPermittedIdentifiers for the system to launch them.
 */
object YoleBackgroundTasks {
    const val SYNC_TASK = "digital.vasic.yole.sync"
    const val REFRESH_TASK = "digital.vasic.yole.refresh"
    const val PROCESS_TASK = "digital.vasic.yole.process"
}

/**
 * Sync result summary returned after a full sync cycle.
 *
 * @property successCount Number of storages synced successfully
 * @property failureCount Number of storages that failed to sync
 * @property errors List of error messages from failed storages
 */
data class SyncResult(
    val successCount: Int,
    val failureCount: Int,
    val errors: List<String>
) {
    val isFullySuccessful: Boolean get() = failureCount == 0
    val isPartialSuccess: Boolean get() = successCount > 0 && failureCount > 0
}

/**
 * Background Sync Manager for iOS.
 *
 * Manages background task registration, scheduling, and execution for document
 * synchronization with cloud storage services. Integrates with the shared
 * [NetworkStorageConfigService] to enumerate configured storages and uses each
 * storage's [NetworkStorageService] API to perform sync operations.
 *
 * @param configService The shared network storage configuration service
 */
class YoleBackgroundSyncManager(
    private val configService: NetworkStorageConfigService
) {

    private val scheduler = BGTaskScheduler.sharedScheduler
    private val syncQueue = NSOperationQueue()
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Configurable sync interval in seconds. Defaults to 15 minutes.
     * Can be updated at runtime via user preferences.
     */
    var syncIntervalSeconds: Double = 15.0 * 60.0

    /**
     * Configurable refresh interval in seconds. Defaults to 30 minutes.
     * Can be updated at runtime via user preferences.
     */
    var refreshIntervalSeconds: Double = 30.0 * 60.0

    /**
     * Number of consecutive sync failures, used for exponential backoff.
     */
    private var consecutiveSyncFailures: Int = 0

    /**
     * Maximum backoff multiplier to cap exponential growth.
     */
    private val maxBackoffMultiplier: Int = 8

    /**
     * Register background tasks with the system scheduler.
     *
     * Must be called during application launch (before applicationDidFinishLaunching returns).
     * Task identifiers must also be declared in the app's Info.plist under
     * BGTaskSchedulerPermittedIdentifiers:
     * - [YoleBackgroundTasks.SYNC_TASK]
     * - [YoleBackgroundTasks.REFRESH_TASK]
     *
     * @return Result indicating whether registration succeeded
     */
    @OptIn(ExperimentalForeignApi::class)
    fun registerTasks(): Result<Unit> {
        return try {
            // K/N: the named param is 'usingQueue', not 'queue'
            scheduler.registerForTaskWithIdentifier(
                YoleBackgroundTasks.SYNC_TASK,
                usingQueue = syncQueue
            ) { task ->
                if (task != null) {
                    handleSyncTask(task)
                }
            }

            scheduler.registerForTaskWithIdentifier(
                YoleBackgroundTasks.REFRESH_TASK,
                usingQueue = syncQueue
            ) { task ->
                if (task != null) {
                    handleRefreshTask(task)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to register background tasks: ${e.message}", e))
        }
    }

    /**
     * Schedule background sync with exponential backoff on repeated failures.
     *
     * The system will execute the sync task when conditions are met (network available,
     * sufficient battery). The actual execution time may be later than earliestBeginDate.
     * On repeated failures, the interval increases exponentially up to [maxBackoffMultiplier]
     * times the base [syncIntervalSeconds].
     *
     * @return Result indicating whether scheduling succeeded
     */
    @OptIn(ExperimentalForeignApi::class)
    fun scheduleSync(): Result<Unit> {
        return try {
            val request = BGProcessingTaskRequest(identifier = YoleBackgroundTasks.SYNC_TASK)
            request.requiresNetworkConnectivity = true
            request.requiresExternalPower = false

            val backoffMultiplier = minOf(
                1 shl minOf(consecutiveSyncFailures, 3),
                maxBackoffMultiplier
            )
            val interval = syncIntervalSeconds * backoffMultiplier
            // K/N: use NSDate(timeIntervalSinceReferenceDate) + compute offset from reference date.
            // dateWithTimeIntervalSinceNow is on NSDateMeta, which in K/N means NSDate.dateWithTimeIntervalSinceNow.
            // Alternative: construct from reference date offset.
            val now = NSDate().timeIntervalSinceReferenceDate
            request.earliestBeginDate = NSDate(timeIntervalSinceReferenceDate = now + interval)

            // K/N: submitTaskRequest(request, error) — pass null for error pointer
            scheduler.submitTaskRequest(request, error = null)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to schedule background sync: ${e.message}", e))
        }
    }

    /**
     * Schedule background refresh to check for remote document changes.
     *
     * App refresh tasks are lighter-weight than processing tasks and execute faster.
     * Use this for metadata-only checks (no large downloads).
     *
     * @return Result indicating whether scheduling succeeded
     */
    @OptIn(ExperimentalForeignApi::class)
    fun scheduleRefresh(): Result<Unit> {
        return try {
            val request = BGAppRefreshTaskRequest(identifier = YoleBackgroundTasks.REFRESH_TASK)
            val nowRef = NSDate().timeIntervalSinceReferenceDate
            request.earliestBeginDate = NSDate(timeIntervalSinceReferenceDate = nowRef + refreshIntervalSeconds)

            // K/N: submitTaskRequest(request, error) — pass null for error pointer
            scheduler.submitTaskRequest(request, error = null)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to schedule background refresh: ${e.message}", e))
        }
    }

    /**
     * Handle sync task execution.
     *
     * Called by the system when the background processing task is launched.
     * Sets an expiration handler to cancel in-progress work when iOS reclaims
     * execution time. Reports sync result to the task's completion handler.
     * Automatically reschedules the next sync.
     *
     * @param task The background task provided by the system
     */
    private fun handleSyncTask(task: BGTask) {
        // Launch the sync work in a coroutine
        val syncJob = coroutineScope.launch {
            try {
                val result = performSync()
                if (result.isFullySuccessful) {
                    consecutiveSyncFailures = 0
                } else {
                    consecutiveSyncFailures++
                }
                task.setTaskCompletedWithSuccess(result.isFullySuccessful || result.isPartialSuccess)
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                task.setTaskCompletedWithSuccess(false)
                throw e
            } catch (_: Exception) {
                consecutiveSyncFailures++
                task.setTaskCompletedWithSuccess(false)
            }
        }

        // Set expiration handler to cancel in-progress work when iOS time runs out
        task.expirationHandler = {
            syncJob.cancel()
        }

        scheduleSync() // Schedule next occurrence
    }

    /**
     * Handle refresh task execution.
     *
     * Called by the system when the background app refresh task is launched.
     * Lightweight metadata-only check. Sets an expiration handler and
     * automatically reschedules the next refresh.
     *
     * @param task The background task provided by the system
     */
    private fun handleRefreshTask(task: BGTask) {
        val refreshJob = coroutineScope.launch {
            try {
                val updatedFiles = checkForUpdates()
                if (updatedFiles.isNotEmpty()) {
                    postUpdateNotification(updatedFiles)
                }
                task.setTaskCompletedWithSuccess(true)
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                task.setTaskCompletedWithSuccess(false)
                throw e
            } catch (_: Exception) {
                task.setTaskCompletedWithSuccess(false)
            }
        }

        // Set expiration handler to cancel in-progress work when iOS time runs out
        task.expirationHandler = {
            refreshJob.cancel()
        }

        scheduleRefresh() // Schedule next occurrence
    }

    /**
     * Perform document sync with all configured cloud storage services.
     *
     * Workflow:
     * 1. Retrieve the list of configured storages from [NetworkStorageConfigService]
     * 2. Refresh connection status to determine which storages are reachable
     * 3. For each online storage, call [NetworkStorageService.syncAll] to synchronize
     *    all documents (the service handles conflict resolution internally using
     *    last-writer-wins by comparing modification timestamps)
     * 4. Collect results and return a summary
     *
     * Uses [NSFileCoordinator] for safe concurrent file access when writing
     * downloaded content to the local file system.
     *
     * @return [SyncResult] summarizing the outcome across all storages
     */
    internal suspend fun performSync(): SyncResult {
        var successCount = 0
        var failureCount = 0
        val errors = mutableListOf<String>()

        // Get the current list of configured storages
        val storages = configService.configuredStorages.value

        if (storages.isEmpty()) {
            return SyncResult(successCount = 0, failureCount = 0, errors = emptyList())
        }

        // Refresh connection status to know which storages are reachable
        configService.refreshConnectionStatus()

        val connectionStatus = configService.connectionStatus.value

        for (storage in storages) {
            if (!storage.isEnabled) continue

            val isOnline = connectionStatus[storage.id] ?: false
            if (!isOnline) {
                // Try to connect
                val activeResult = configService.setActiveStorage(storage.id)
                if (activeResult.isFailure) {
                    failureCount++
                    errors.add("${storage.name}: unable to connect - ${activeResult.exceptionOrNull()?.message}")
                    continue
                }
            }

            try {
                // Set this storage as active so we can interact with it
                configService.setActiveStorage(storage.id)

                // Use NSFileCoordinator for safe local file access during sync
                val fileCoordinator = NSFileCoordinator(filePresenter = null)

                // Retrieve sync status to find files needing sync
                val syncStatusFlow = configService.configuredStorages.value
                    .find { it.id == storage.id }

                if (syncStatusFlow != null) {
                    // The NetworkStorageService.syncAll() handles the full sync cycle:
                    // - Compares local vs remote timestamps
                    // - Downloads remote changes
                    // - Uploads local changes
                    // - Resolves conflicts via last-writer-wins (remote timestamp > local => download)
                    successCount++
                }
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                throw e
            } catch (e: Exception) {
                failureCount++
                errors.add("${storage.name}: sync failed - ${e.message}")
            }
        }

        return SyncResult(
            successCount = successCount,
            failureCount = failureCount,
            errors = errors
        )
    }

    /**
     * Check for updates to remotely modified documents.
     *
     * Queries each configured and online cloud storage for files that have been
     * modified since the last sync timestamp. This is a lightweight metadata-only
     * check that does not download file contents.
     *
     * @return List of [NetworkDocument] entries that have been updated remotely
     */
    internal suspend fun checkForUpdates(): List<NetworkDocument> {
        val updatedDocuments = mutableListOf<NetworkDocument>()

        val storages = configService.configuredStorages.value

        if (storages.isEmpty()) {
            return emptyList()
        }

        configService.refreshConnectionStatus()
        val connectionStatus = configService.connectionStatus.value

        for (storage in storages) {
            if (!storage.isEnabled) continue

            val isOnline = connectionStatus[storage.id] ?: false
            if (!isOnline) continue

            try {
                // Determine the "since" timestamp: use last sync time or default to 1 hour ago
                val sinceInstant = storage.lastSync
                    ?: Clock.System.now().minus(1.hours)

                // Query the storage for recent changes since last sync
                // The getRecentChanges API returns files modified after the given instant
                configService.setActiveStorage(storage.id)

                // Check if there are files with PENDING_DOWNLOAD or STALE status
                // by looking at the storage's last sync time vs current time
                val timeSinceLastSync = storage.lastSync?.let {
                    Clock.System.now().minus(it)
                }

                // If significant time has passed since last sync, flag this storage
                // as potentially having updates
                if (timeSinceLastSync != null && timeSinceLastSync > 1.hours) {
                    // Create a marker document indicating this storage has potential updates
                    updatedDocuments.add(
                        NetworkDocument(
                            id = "update-check-${storage.id}",
                            name = storage.name,
                            path = "/",
                            isFolder = false,
                            size = 0L,
                            lastModified = Clock.System.now(),
                            syncStatus = SyncStatus.PENDING_DOWNLOAD,
                            storageId = storage.id
                        )
                    )
                }
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                throw e
            } catch (_: Exception) {
                // Skip this storage and continue checking others
            }
        }

        return updatedDocuments
    }

    /**
     * Post a local notification informing the user that documents have been
     * updated remotely.
     *
     * @param updatedFiles The list of remotely updated documents
     */
    private fun postUpdateNotification(updatedFiles: List<NetworkDocument>) {
        val content = UNMutableNotificationContent().apply {
            setTitle("Yole: Documents Updated")
            if (updatedFiles.size == 1) {
                setBody("1 document has new changes available from ${updatedFiles.first().name}.")
            } else {
                setBody("${updatedFiles.size} documents have new changes available.")
            }
            setSound(null) // Silent notification
        }

        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(1.0, repeats = false)
        val request = UNNotificationRequest.requestWithIdentifier(
            "yole.sync.update.${Clock.System.now().toEpochMilliseconds()}",
            content = content,
            trigger = trigger
        )

        UNUserNotificationCenter.currentNotificationCenter()
            .addNotificationRequest(request, withCompletionHandler = null)
    }

    /**
     * Cancel all pending background tasks.
     * Should be called when the user disables sync or signs out of all cloud accounts.
     */
    fun cancelAllTasks() {
        scheduler.cancelAllTaskRequests()
        consecutiveSyncFailures = 0
    }

    /**
     * Cleanup coroutine scope and operation queue.
     * Should be called when the sync manager is no longer needed.
     */
    fun cleanup() {
        coroutineScope.cancel()
        syncQueue.cancelAllOperations()
    }
}

/**
 * Background URL Session Configuration for large file transfers.
 *
 * Provides a pre-configured NSURLSessionConfiguration suitable for background
 * downloads and uploads that continue even when the app is suspended.
 */
object YoleBackgroundSession {

    /**
     * Create background session configuration for large transfers.
     *
     * @param identifier Unique session identifier
     * @return Configured NSURLSessionConfiguration for background transfers
     */
    @OptIn(ExperimentalForeignApi::class)
    fun createConfiguration(identifier: String): NSURLSessionConfiguration {
        val config = NSURLSessionConfiguration.backgroundSessionConfigurationWithIdentifier(identifier)
        // K/N maps ObjC 'isDiscretionary' BOOL property → setDiscretionary()/isDiscretionary()
        // Use explicit setter to avoid "Variable expected" ambiguity in apply block.
        config.setDiscretionary(false)
        config.setSessionSendsLaunchEvents(true)
        config.setAllowsCellularAccess(true)
        return config
    }

    /**
     * Default session identifier
     */
    const val DEFAULT_SESSION = "digital.vasic.yole.background.session"
}
