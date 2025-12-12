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
import org.w3c.workers.*
import kotlin.js.Promise

/**
 * Progressive Web App features implementation
 * Service worker, offline support, file system access
 */
object PWAFeatures {
    
    private var serviceWorkerRegistration: ServiceWorkerRegistration? = null
    private var isOfflineMode = false
    private var fileSystemAccessSupported = false
    
    // Cache names
    private const val CACHE_NAME = "yole-cache-v1"
    private const val API_CACHE_NAME = "yole-api-cache-v1"
    
    /**
     * Initialize all PWA features
     */
    fun initialize() {
        console.log("Initializing PWA features...")
        
        // Check if service workers are supported
        if (js("'serviceWorker' in navigator") as Boolean) {
            registerServiceWorker()
        } else {
            console.warn("Service workers not supported")
        }
        
        // Check for File System Access API
        if (js("'showOpenFilePicker' in window") as Boolean) {
            fileSystemAccessSupported = true
            console.log("File System Access API supported")
        }
        
        // Set up offline detection
        setupOfflineDetection()
        
        // Set up app install prompt
        setupInstallPrompt()
        
        console.log("PWA features initialized")
    }
    
    /**
     * Register service worker for offline functionality
     */
    private fun registerServiceWorker() {
        try {
            navigator.serviceWorker.register("/service-worker.js").then { registration ->
                serviceWorkerRegistration = registration
                console.log("Service worker registered successfully", registration)
                
                // Check for updates
                checkForServiceWorkerUpdates()
                
                // Set up message handling
                setupServiceWorkerMessaging()
                
            }.catch { error ->
                console.error("Service worker registration failed:", error)
            }
        } catch (e: dynamic) {
            console.error("Error registering service worker:", e)
        }
    }
    
    /**
     * Check for service worker updates
     */
    private fun checkForServiceWorkerUpdates() {
        navigator.serviceWorker.ready.then { registration ->
            registration.update().then {
                console.log("Service worker update check completed")
            }.catch { error ->
                console.error("Service worker update failed:", error)
            }
        }
    }
    
    /**
     * Set up service worker messaging
     */
    private fun setupServiceWorkerMessaging() {
        navigator.serviceWorker.addEventListener("message", { event ->
            val messageEvent = event as MessageEvent
            handleServiceWorkerMessage(messageEvent.data)
        })
    }
    
    /**
     * Handle messages from service worker
     */
    private fun handleServiceWorkerMessage(data: dynamic) {
        when (data.type as? String) {
            "offline-ready" -> {
                console.log("App is ready for offline use")
                showOfflineReadyNotification()
            }
            "update-available" -> {
                console.log("App update available")
                showUpdateAvailableNotification()
            }
            else -> {
                console.log("Unknown service worker message:", data)
            }
        }
    }
    
    /**
     * Send message to service worker
     */
    fun sendMessageToServiceWorker(message: dynamic) {
        if (serviceWorkerRegistration?.active != null) {
            serviceWorkerRegistration?.active?.postMessage(message)
        } else {
            console.warn("No active service worker to send message to")
        }
    }
    
    /**
     * Cache a resource for offline use
     */
    suspend fun cacheResource(url: String, response: Response): Boolean {
        return try {
            val cache = window.caches.open(CACHE_NAME).await()
            cache.put(url, response).await()
            console.log("Cached resource:", url)
            true
        } catch (e: dynamic) {
            console.error("Failed to cache resource:", e)
            false
        }
    }
    
    /**
     * Get cached resource or fetch from network
     */
    suspend fun getCachedOrFetch(url: String): Response? {
        return try {
            val cache = window.caches.open(CACHE_NAME).await()
            
            // Try cache first
            val cachedResponse = cache.match(url).await()
            if (cachedResponse != null) {
                console.log("Found cached resource:", url)
                return cachedResponse
            }
            
            // If not in cache, fetch from network
            if (!isOfflineMode) {
                val networkResponse = window.fetch(url).await()
                // Cache for future use
                cacheResource(url, networkResponse)
                return networkResponse
            }
            
            null
        } catch (e: dynamic) {
            console.error("Error getting cached or fetched resource:", e)
            null
        }
    }
    
    /**
     * Clear all caches
     */
    suspend fun clearCaches(): Boolean {
        return try {
            window.caches.delete(CACHE_NAME).await()
            window.caches.delete(API_CACHE_NAME).await()
            console.log("All caches cleared")
            true
        } catch (e: dynamic) {
            console.error("Failed to clear caches:", e)
            false
        }
    }
    
    /**
     * Set up offline detection
     */
    private fun setupOfflineDetection() {
        window.addEventListener("online", { event ->
            isOfflineMode = false
            console.log("Back online")
            handleOnlineStatus()
        })
        
        window.addEventListener("offline", { event ->
            isOfflineMode = true
            console.log("Gone offline")
            handleOfflineStatus()
        })
        
        // Check initial status
        isOfflineMode = !navigator.onLine
        console.log("Initial online status:", navigator.onLine)
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
            val state = getCurrentApplicationState()
            localStorage.setItem("offline_state", JSON.stringify(state))
            console.log("State saved for offline access")
        } catch (e: dynamic) {
            console.error("Failed to save state for offline:", e)
        }
    }
    
    /**
     * Get current application state for offline storage
     */
    private fun getCurrentApplicationState(): dynamic {
        return js("({})").apply {
            this.timestamp = Date().getTime()
            this.documents = getOpenDocuments()
            this.settings = getApplicationSettings()
            this.cacheVersion = "1.0"
        }
    }
    
    /**
     * Sync offline changes when coming back online
     */
    private fun syncOfflineChanges() {
        try {
            val offlineState = localStorage.getItem("offline_state")
            if (offlineState != null) {
                val state = JSON.parse(offlineState)
                console.log("Syncing offline changes", state)
                
                // Process offline changes
                processOfflineChanges(state)
                
                // Clear offline state after sync
                localStorage.removeItem("offline_state")
            }
        } catch (e: dynamic) {
            console.error("Failed to sync offline changes:", e)
        }
    }
    
    /**
     * Advanced file system access using File System Access API
     */
    suspend fun openFileWithFileSystemAccess(): FileHandle? {
        if (!fileSystemAccessSupported) {
            console.warn("File System Access API not supported")
            return null
        }
        
        return try {
            val options = js("({})")
            options.types = arrayOf(
                js("({})").apply {
                    this.description = "Text Files"
                    this.accept = js("({})")
                    this.accept["text/*"] = arrayOf(".txt", ".md", ".csv", ".tex", ".org", ".adoc", ".wiki")
                }
            )
            
            val fileHandle = window.showOpenFilePicker(options).await<dynamic>()
            if (fileHandle is Array<*> && fileHandle.isNotEmpty()) {
                val handle = fileHandle[0] as FileSystemFileHandle
                console.log("File selected:", handle.name)
                FileSystemFileHandle(handle)
            } else {
                null
            }
        } catch (e: dynamic) {
            console.error("Error opening file with File System Access API:", e)
            null
        }
    }
    
    /**
     * Save file using File System Access API
     */
    suspend fun saveFileWithFileSystemAccess(content: String, suggestedName: String): Boolean {
        if (!fileSystemAccessSupported) {
            console.warn("File System Access API not supported")
            return false
        }
        
        return try {
            val options = js("({})")
            options.suggestedName = suggestedName
            options.types = arrayOf(
                js("({})").apply {
                    this.description = "Text Files"
                    this.accept = js("({})")
                    this.accept["text/plain"] = arrayOf(".txt")
                }
            )
            
            val fileHandle = window.showSaveFilePicker(options).await<dynamic>()
            if (fileHandle is FileSystemFileHandle) {
                // Write content to file
                val writableStream = fileHandle.createWritable().await<dynamic>()
                writableStream.write(content).await<dynamic>()
                writableStream.close().await<dynamic>()
                
                console.log("File saved successfully:", fileHandle.name)
                true
            } else {
                false
            }
        } catch (e: dynamic) {
            console.error("Error saving file with File System Access API:", e)
            false
        }
    }
    
    /**
     * Set up app install prompt
     */
    private fun setupInstallPrompt() {
        var deferredPrompt: dynamic = null
        
        window.addEventListener("beforeinstallprompt", { event ->
            event.preventDefault()
            deferredPrompt = event
            console.log("Install prompt deferred")
            showInstallButton()
        })
    }
    
    /**
     * Show install button
     */
    private fun showInstallButton() {
        // This would show a UI element to prompt installation
        console.log("Showing install button")
    }
    
    /**
     * Trigger app installation
     */
    suspend fun triggerInstall(): Boolean {
        return try {
            // Implementation depends on browser support
            console.log("Triggering app installation")
            true
        } catch (e: dynamic) {
            console.error("Failed to trigger install:", e)
            false
        }
    }
    
    /**
     * Background sync for offline support
     */
    fun registerBackgroundSync(tag: String = "yole-sync") {
        if (js("'sync' in registration") as Boolean) {
            serviceWorkerRegistration?.sync?.register(tag)?.then {
                console.log("Background sync registered:", tag)
            }?.catch { error ->
                console.error("Failed to register background sync:", error)
            }
        }
    }
    
    /**
     * Show offline ready notification
     */
    private fun showOfflineReadyNotification() {
        if (Notification.permission == "granted") {
            Notification("Yole Offline Ready", js("({})").apply {
                this.body = "App is ready to work offline"
                this.icon = "/icon-192.png"
            })
        }
    }
    
    /**
     * Show online notification
     */
    private fun showOnlineNotification() {
        if (Notification.permission == "granted") {
            Notification("Yole Online", js("({})").apply {
                this.body = "Back online - syncing changes"
                this.icon = "/icon-192.png"
            })
        }
    }
    
    /**
     * Show offline notification
     */
    private fun showOfflineNotification() {
        if (Notification.permission == "granted") {
            Notification("Yole Offline", js("({})").apply {
                this.body = "Working offline - changes will sync when online"
                this.icon = "/icon-192.png"
            })
        }
    }
    
    /**
     * Show update available notification
     */
    private fun showUpdateAvailableNotification() {
        if (Notification.permission == "granted") {
            Notification("Yole Update Available", js("({})").apply {
                this.body = "A new version is available. Refresh to update."
                this.icon = "/icon-192.png"
            })
        }
    }
    
    /**
     * Request notification permission
     */
    fun requestNotificationPermission() {
        if (Notification.permission != "granted") {
            Notification.requestPermission().then { permission ->
                console.log("Notification permission:", permission)
            }
        }
    }
    
    /**
     * Check if running in standalone mode (PWA installed)
     */
    fun isStandaloneMode(): Boolean {
        return window.matchMedia("(display-mode: standalone)").matches ||
               window.matchMedia("(display-mode: fullscreen)").matches ||
               (js("window.navigator.standalone") as? Boolean) == true
    }
    
    /**
     * Get cache statistics
     */
    suspend fun getCacheStats(): CacheStats? {
        return try {
            val cache = window.caches.open(CACHE_NAME).await()
            val keys = cache.keys().await()
            
            CacheStats(
                cacheName = CACHE_NAME,
                entryCount = keys.length,
                timestamp = Date().getTime()
            )
        } catch (e: dynamic) {
            console.error("Failed to get cache stats:", e)
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
 * Wrapper for File System Access API file handles
 */
class FileSystemFileHandle(private val handle: dynamic) {
    val name: String get() = handle.name as String
    
    suspend fun getFile(): File {
        return handle.getFile().await()
    }
    
    suspend fun createWritable(): dynamic {
        return handle.createWritable().await()
    }
}

/**
 * Helper functions for DOM manipulation
 */
private fun getOpenDocuments(): Array<dynamic> {
    // Implementation would get currently open documents
    return emptyArray()
}

private fun getApplicationSettings(): dynamic {
    // Implementation would get current application settings
    return js("({})")
}

private fun processOfflineChanges(state: dynamic) {
    // Implementation would process offline changes
    console.log("Processing offline changes:", state)
}

private fun showOfflineReadyNotification() {
    PWAFeatures.showOfflineReadyNotification()
}

private fun showOnlineNotification() {
    PWAFeatures.showOnlineNotification()
}

private fun showOfflineNotification() {
    PWAFeatures.showOfflineNotification()
}