/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Progressive Web App Features for Yole Web
 * Service worker, offline mode, advanced web APIs
 *
 *########################################################*/

package digital.vasic.yole.web

import kotlinx.browser.*
import kotlinx.coroutines.*
import org.w3c.dom.*
import org.w3c.dom.events.Event
import org.w3c.files.*
import kotlin.js.JsAny
import kotlin.js.JsBoolean
import kotlin.js.JsString

// External JS helper functions declared at top level for Wasm compatibility
// In Kotlin/Wasm, js("...") must be the sole expression in a top-level function body.
// The function must return JsAny (or subtype). Conversions happen in wrapper functions.

private fun jsNavigatorOnLineRaw(): JsBoolean = js("navigator.onLine")
private fun jsNavigatorOnLine(): Boolean = jsNavigatorOnLineRaw().toBoolean()

private fun jsHasServiceWorkerRaw(): JsBoolean = js("('serviceWorker' in navigator)")
private fun jsHasServiceWorker(): Boolean = jsHasServiceWorkerRaw().toBoolean()

private fun jsHasShowOpenFilePickerRaw(): JsBoolean = js("('showOpenFilePicker' in window)")
private fun jsHasShowOpenFilePicker(): Boolean = jsHasShowOpenFilePickerRaw().toBoolean()

private fun jsIsStandaloneModeRaw(): JsBoolean = js("(window.matchMedia('(display-mode: standalone)').matches || window.matchMedia('(display-mode: fullscreen)').matches || (window.navigator.standalone === true))")
private fun jsIsStandaloneMode(): Boolean = jsIsStandaloneModeRaw().toBoolean()

private fun jsGetCurrentTimeMsRaw(): kotlin.js.JsNumber = js("Date.now()")
private fun jsGetCurrentTimeMs(): Double = jsGetCurrentTimeMsRaw().toDouble()

/**
 * Progressive Web App features implementation
 * Service worker, offline support, file system access
 *
 * Note: Many advanced PWA features (service workers, File System Access API,
 * notifications, caching) are not fully available in Kotlin/Wasm due to
 * limited JS interop. These features are stubbed out with TODO comments
 * and will be implemented when Wasm JS interop matures.
 */
object PWAFeatures {

    private var isOfflineMode = false
    private var fileSystemAccessSupported = false

    // Cache names
    private const val CACHE_NAME = "yole-cache-v1"
    private const val API_CACHE_NAME = "yole-api-cache-v1"

    /**
     * Initialize all PWA features
     */
    fun initialize() {
        println("Initializing PWA features...")

        // Check if service workers are supported
        if (jsHasServiceWorker()) {
            registerServiceWorker()
        } else {
            println("WARN: Service workers not supported")
        }

        // Check for File System Access API
        if (jsHasShowOpenFilePicker()) {
            fileSystemAccessSupported = true
            println("File System Access API supported")
        }

        // Set up offline detection
        setupOfflineDetection()

        // Set up app install prompt
        setupInstallPrompt()

        println("PWA features initialized")
    }

    /**
     * Register service worker for offline functionality
     */
    private fun registerServiceWorker() {
        // TODO: Service worker registration requires advanced JS interop
        // not yet fully supported in Kotlin/Wasm. Will be implemented
        // when ServiceWorker APIs are available in the Wasm stdlib.
        println("Service worker registration: stubbed for Wasm compatibility")
    }

    /**
     * Check for service worker updates
     */
    private fun checkForServiceWorkerUpdates() {
        // TODO: Requires service worker registration (see registerServiceWorker)
        println("Service worker update check: stubbed for Wasm compatibility")
    }

    /**
     * Set up service worker messaging
     */
    private fun setupServiceWorkerMessaging() {
        // TODO: Requires service worker registration (see registerServiceWorker)
        println("Service worker messaging: stubbed for Wasm compatibility")
    }

    /**
     * Handle messages from service worker
     */
    private fun handleServiceWorkerMessage(data: String) {
        println("Service worker message received: $data")
    }

    /**
     * Send message to service worker
     */
    fun sendMessageToServiceWorker(message: String) {
        // TODO: Requires service worker registration
        println("WARN: No active service worker to send message to")
    }

    /**
     * Cache a resource for offline use
     * TODO: Cache API interop not available in Kotlin/Wasm yet
     */
    suspend fun cacheResource(url: String): Boolean {
        return try {
            println("Caching resource: $url (stubbed for Wasm)")
            // TODO: Implement when Cache API is available in Wasm
            false
        } catch (e: Exception) {
            println("ERROR: Failed to cache resource: ${e.message}")
            false
        }
    }

    /**
     * Get cached resource or fetch from network
     * TODO: Cache API interop not available in Kotlin/Wasm yet
     */
    suspend fun getCachedOrFetch(url: String): String? {
        return try {
            println("Getting cached or fetching: $url (stubbed for Wasm)")
            // TODO: Implement when Cache/Fetch API is available in Wasm
            null
        } catch (e: Exception) {
            println("ERROR: Error getting cached or fetched resource: ${e.message}")
            null
        }
    }

    /**
     * Clear all caches
     * TODO: Cache API interop not available in Kotlin/Wasm yet
     */
    suspend fun clearCaches(): Boolean {
        return try {
            println("Clearing caches (stubbed for Wasm)")
            // TODO: Implement when Cache API is available in Wasm
            false
        } catch (e: Exception) {
            println("ERROR: Failed to clear caches: ${e.message}")
            false
        }
    }

    /**
     * Set up offline detection
     */
    private fun setupOfflineDetection() {
        window.addEventListener("online", { _: Event ->
            isOfflineMode = false
            println("Back online")
            handleOnlineStatus()
        })

        window.addEventListener("offline", { _: Event ->
            isOfflineMode = true
            println("Gone offline")
            handleOfflineStatus()
        })

        // Check initial status
        isOfflineMode = !jsNavigatorOnLine()
        println("Initial online status: ${jsNavigatorOnLine()}")
    }

    /**
     * Handle going online
     */
    private fun handleOnlineStatus() {
        // Sync any offline changes
        syncOfflineChanges()

        // Show online notification
        showOnlineNotification()
    }

    /**
     * Handle going offline
     */
    private fun handleOfflineStatus() {
        // Save current state for offline access
        saveStateForOffline()

        // Show offline notification
        showOfflineNotification()
    }

    /**
     * Save state for offline access
     */
    private fun saveStateForOffline() {
        try {
            val timestamp = jsGetCurrentTimeMs()
            val stateJson = """{"timestamp":$timestamp,"cacheVersion":"1.0"}"""
            localStorage.setItem("offline_state", stateJson)
            println("State saved for offline access")
        } catch (e: Exception) {
            println("ERROR: Failed to save state for offline: ${e.message}")
        }
    }

    /**
     * Sync offline changes when coming back online
     */
    private fun syncOfflineChanges() {
        try {
            val offlineState = localStorage.getItem("offline_state")
            if (offlineState != null) {
                println("Syncing offline changes: $offlineState")

                // Process offline changes
                processOfflineChanges(offlineState)

                // Clear offline state after sync
                localStorage.removeItem("offline_state")
            }
        } catch (e: Exception) {
            println("ERROR: Failed to sync offline changes: ${e.message}")
        }
    }

    /**
     * Advanced file system access using File System Access API
     * TODO: File System Access API requires advanced JS interop not yet available in Wasm
     */
    suspend fun openFileWithFileSystemAccess(): FileHandle? {
        if (!fileSystemAccessSupported) {
            println("WARN: File System Access API not supported")
            return null
        }

        // TODO: Implement when showOpenFilePicker is available in Wasm
        println("File System Access API: stubbed for Wasm compatibility")
        return null
    }

    /**
     * Save file using File System Access API
     * TODO: File System Access API requires advanced JS interop not yet available in Wasm
     */
    suspend fun saveFileWithFileSystemAccess(content: String, suggestedName: String): Boolean {
        if (!fileSystemAccessSupported) {
            println("WARN: File System Access API not supported")
            return false
        }

        // TODO: Implement when showSaveFilePicker is available in Wasm
        println("File System Access API save: stubbed for Wasm compatibility")
        return false
    }

    /**
     * Set up app install prompt
     */
    private fun setupInstallPrompt() {
        window.addEventListener("beforeinstallprompt", { event: Event ->
            event.preventDefault()
            println("Install prompt deferred")
            showInstallButton()
        })
    }

    /**
     * Show install button
     */
    private fun showInstallButton() {
        // This would show a UI element to prompt installation
        println("Showing install button")
    }

    /**
     * Trigger app installation
     */
    suspend fun triggerInstall(): Boolean {
        return try {
            // TODO: Implementation depends on browser support and JS interop
            println("Triggering app installation (stubbed for Wasm)")
            true
        } catch (e: Exception) {
            println("ERROR: Failed to trigger install: ${e.message}")
            false
        }
    }

    /**
     * Background sync for offline support
     * TODO: Background Sync API not available in Kotlin/Wasm yet
     */
    fun registerBackgroundSync(tag: String = "yole-sync") {
        println("Background sync registration: stubbed for Wasm (tag=$tag)")
    }

    /**
     * Show offline ready notification
     */
    private fun showOfflineReadyNotification() {
        // TODO: Notification API not available in Kotlin/Wasm yet
        println("Notification: App is ready for offline use")
    }

    /**
     * Show online notification
     */
    private fun showOnlineNotification() {
        println("Notification: Back online - syncing changes")
    }

    /**
     * Show offline notification
     */
    private fun showOfflineNotification() {
        println("Notification: Working offline - changes will sync when online")
    }

    /**
     * Show update available notification
     */
    private fun showUpdateAvailableNotification() {
        println("Notification: A new version is available. Refresh to update.")
    }

    /**
     * Request notification permission
     * TODO: Notification API not available in Kotlin/Wasm yet
     */
    fun requestNotificationPermission() {
        println("Notification permission request: stubbed for Wasm")
    }

    /**
     * Check if running in standalone mode (PWA installed)
     */
    fun isStandaloneMode(): Boolean {
        return jsIsStandaloneMode()
    }

    /**
     * Get cache statistics
     * TODO: Cache API not available in Kotlin/Wasm yet
     */
    suspend fun getCacheStats(): CacheStats? {
        return try {
            CacheStats(
                cacheName = CACHE_NAME,
                entryCount = 0,
                timestamp = jsGetCurrentTimeMs()
            )
        } catch (e: Exception) {
            println("ERROR: Failed to get cache stats: ${e.message}")
            null
        }
    }

    /**
     * Cache statistics
     */
    data class CacheStats(
        val cacheName: String,
        val entryCount: Int,
        val timestamp: Double
    )
}

/**
 * Simple file handle wrapper for Wasm compatibility
 */
class FileHandle(val name: String) {
    // TODO: Implement actual file handle when File System Access API
    // is available in Kotlin/Wasm
}

/**
 * Helper functions
 */
private fun processOfflineChanges(state: String) {
    // Implementation would process offline changes
    println("Processing offline changes: $state")
}
