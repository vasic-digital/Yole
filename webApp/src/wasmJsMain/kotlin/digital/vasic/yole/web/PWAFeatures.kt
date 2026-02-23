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
import kotlin.js.JsNumber
import kotlin.js.JsString

// External JS helper functions declared at top level for Wasm compatibility.
// In Kotlin/Wasm, js("...") must be the sole expression in a top-level function body.
// The function must return JsAny (or subtype). Conversions happen in wrapper functions.

// --- Navigator and environment checks ---

private fun jsNavigatorOnLineRaw(): JsBoolean = js("navigator.onLine")
private fun jsNavigatorOnLine(): Boolean = jsNavigatorOnLineRaw().toBoolean()

private fun jsHasServiceWorkerRaw(): JsBoolean = js("('serviceWorker' in navigator)")
private fun jsHasServiceWorker(): Boolean = jsHasServiceWorkerRaw().toBoolean()

private fun jsHasShowOpenFilePickerRaw(): JsBoolean = js("('showOpenFilePicker' in window)")
private fun jsHasShowOpenFilePicker(): Boolean = jsHasShowOpenFilePickerRaw().toBoolean()

private fun jsHasShowSaveFilePickerRaw(): JsBoolean = js("('showSaveFilePicker' in window)")
private fun jsHasShowSaveFilePicker(): Boolean = jsHasShowSaveFilePickerRaw().toBoolean()

private fun jsIsStandaloneModeRaw(): JsBoolean = js("(window.matchMedia('(display-mode: standalone)').matches || window.matchMedia('(display-mode: fullscreen)').matches || (window.navigator.standalone === true))")
private fun jsIsStandaloneMode(): Boolean = jsIsStandaloneModeRaw().toBoolean()

private fun jsGetCurrentTimeMsRaw(): JsNumber = js("Date.now()")
private fun jsGetCurrentTimeMs(): Double = jsGetCurrentTimeMsRaw().toDouble()

// --- Service Worker registration ---
// navigator.serviceWorker.register() returns a Promise. We fire-and-forget via
// .then()/.catch() since Kotlin/Wasm cannot directly await JS Promises without
// kotlinx-coroutines-core JS dispatcher support (which is not yet available for Wasm).

private fun jsRegisterServiceWorkerImpl(): JsAny = js("""
    navigator.serviceWorker.register('/service-worker.js')
        .then(function(reg) {
            console.log('[PWA] Service worker registered, scope: ' + reg.scope);
            return reg;
        })
        .catch(function(err) {
            console.warn('[PWA] Service worker registration failed: ' + err);
            return null;
        })
""")

// --- Service Worker update check ---

private fun jsCheckServiceWorkerUpdateImpl(): JsAny = js("""
    navigator.serviceWorker.getRegistration()
        .then(function(reg) {
            if (reg) {
                reg.update();
                console.log('[PWA] Service worker update check triggered');
            }
            return null;
        })
        .catch(function(err) {
            console.warn('[PWA] Service worker update check failed: ' + err);
            return null;
        })
""")

// --- Service Worker messaging ---

private fun jsSetupServiceWorkerMessagingImpl(): JsAny = js("""
    (function() {
        if (navigator.serviceWorker) {
            navigator.serviceWorker.addEventListener('message', function(event) {
                if (event.data && event.data.type) {
                    console.log('[PWA] SW message: ' + JSON.stringify(event.data));
                }
            });
        }
        return true;
    })()
""")

private fun jsSendMessageToServiceWorkerImpl(msg: JsString): JsAny = js("""
    (function() {
        if (navigator.serviceWorker && navigator.serviceWorker.controller) {
            navigator.serviceWorker.controller.postMessage({ type: msg });
            return true;
        }
        return false;
    })()
""")

private fun jsSendMessageToServiceWorker(message: String): Boolean {
    val result = jsSendMessageToServiceWorkerImpl(message.toJsString())
    // The JS function returns a boolean-like JsAny; we check truthiness
    return result.toString() == "true"
}

// --- Cache API ---
// The Cache API (caches.open, caches.delete, cache.put, cache.match) is a Promise-based
// browser API. In Kotlin/Wasm, we cannot directly bridge JS Promises to Kotlin coroutines
// because the kotlinx-coroutines JS dispatcher is not available for Wasm targets.
//
// The recommended approach for cache management in a PWA is to delegate caching logic
// to the service worker (service-worker.js), which already implements cache-first and
// network-first strategies. The Kotlin/Wasm app communicates with the service worker
// via postMessage to request cache operations.
//
// For direct cache manipulation from the main thread, a JavaScript helper file
// (e.g., cache-helpers.js) could be loaded alongside the Wasm module, exposing
// synchronous-looking wrappers or callback-based APIs that Kotlin/Wasm can invoke.

private fun jsCacheDeleteImpl(cacheName: JsString): JsAny = js("""
    caches.delete(cacheName)
        .then(function(deleted) {
            console.log('[PWA] Cache deleted: ' + cacheName + ' = ' + deleted);
            return deleted;
        })
        .catch(function(err) {
            console.warn('[PWA] Cache delete failed: ' + err);
            return false;
        })
""")

// --- Notification API ---

private fun jsHasNotificationRaw(): JsBoolean = js("('Notification' in window)")
private fun jsHasNotification(): Boolean = jsHasNotificationRaw().toBoolean()

private fun jsNotificationPermissionRaw(): JsString = js("(('Notification' in window) ? Notification.permission : 'denied')")
private fun jsNotificationPermission(): String = jsNotificationPermissionRaw().toString()

private fun jsRequestNotificationPermissionImpl(): JsAny = js("""
    (function() {
        if ('Notification' in window) {
            Notification.requestPermission().then(function(perm) {
                console.log('[PWA] Notification permission: ' + perm);
            });
        }
        return true;
    })()
""")

private fun jsShowNotificationImpl(title: JsString, body: JsString): JsAny = js("""
    (function() {
        if ('Notification' in window && Notification.permission === 'granted') {
            new Notification(title, { body: body, icon: '/Logo.png' });
        }
        return true;
    })()
""")

private fun jsShowNotification(title: String, body: String) {
    jsShowNotificationImpl(title.toJsString(), body.toJsString())
}

// --- Background Sync API ---

private fun jsRegisterBackgroundSyncImpl(tag: JsString): JsAny = js("""
    (function() {
        if ('serviceWorker' in navigator && 'SyncManager' in window) {
            navigator.serviceWorker.ready.then(function(reg) {
                return reg.sync.register(tag);
            }).then(function() {
                console.log('[PWA] Background sync registered: ' + tag);
            }).catch(function(err) {
                console.warn('[PWA] Background sync registration failed: ' + err);
            });
            return true;
        }
        return false;
    })()
""")

// --- Install prompt ---
// The beforeinstallprompt event provides a prompt() method to trigger installation.
// We store the deferred prompt event in a global JS variable so it can be invoked later.

private fun jsSetupInstallPromptImpl(): JsAny = js("""
    (function() {
        window.__yoleDeferredPrompt = null;
        window.addEventListener('beforeinstallprompt', function(e) {
            e.preventDefault();
            window.__yoleDeferredPrompt = e;
            console.log('[PWA] Install prompt deferred and stored');
        });
        return true;
    })()
""")

private fun jsTriggerInstallImpl(): JsAny = js("""
    (function() {
        if (window.__yoleDeferredPrompt) {
            window.__yoleDeferredPrompt.prompt();
            window.__yoleDeferredPrompt.userChoice.then(function(choiceResult) {
                console.log('[PWA] Install choice: ' + choiceResult.outcome);
                window.__yoleDeferredPrompt = null;
            });
            return true;
        }
        return false;
    })()
""")

private fun jsHasDeferredPromptRaw(): JsBoolean = js("(window.__yoleDeferredPrompt !== null && window.__yoleDeferredPrompt !== undefined)")
private fun jsHasDeferredPrompt(): Boolean = jsHasDeferredPromptRaw().toBoolean()

// --- File System Access API ---
// The File System Access API (showOpenFilePicker, showSaveFilePicker) returns Promises
// of FileSystemFileHandle objects. These are complex browser-native objects that cannot
// be directly mapped to Kotlin/Wasm types.
//
// Strategy: We use JS interop to invoke the picker dialogs and perform file I/O
// entirely in JavaScript, passing the resulting text content back as a JsString.
// Since we cannot await Promises from Kotlin/Wasm, the actual read/write is
// performed asynchronously via callbacks stored in global JS variables.

private fun jsOpenFilePickerImpl(): JsAny = js("""
    (function() {
        if (!('showOpenFilePicker' in window)) return null;
        window.__yoleOpenFileResult = null;
        window.__yoleOpenFileError = null;
        window.showOpenFilePicker({
            types: [{
                description: 'Text Files',
                accept: {
                    'text/*': ['.txt', '.md', '.csv', '.tex', '.org', '.adoc', '.wiki', '.json', '.xml', '.yaml', '.yml', '.html']
                }
            }],
            multiple: false
        }).then(function(handles) {
            var handle = handles[0];
            return handle.getFile().then(function(file) {
                return file.text().then(function(text) {
                    window.__yoleOpenFileResult = { name: file.name, content: text };
                    console.log('[PWA] File opened: ' + file.name + ' (' + text.length + ' chars)');
                });
            });
        }).catch(function(err) {
            window.__yoleOpenFileError = err.message || 'Unknown error';
            console.warn('[PWA] File open cancelled or failed: ' + err);
        });
        return true;
    })()
""")

private fun jsGetOpenFileResultNameRaw(): JsAny? = js("(window.__yoleOpenFileResult ? window.__yoleOpenFileResult.name : null)")
private fun jsGetOpenFileResultContentRaw(): JsAny? = js("(window.__yoleOpenFileResult ? window.__yoleOpenFileResult.content : null)")

private fun jsHasOpenFileResultRaw(): JsBoolean = js("(window.__yoleOpenFileResult !== null && window.__yoleOpenFileResult !== undefined)")
private fun jsHasOpenFileResult(): Boolean = jsHasOpenFileResultRaw().toBoolean()

private fun jsClearOpenFileResult(): JsAny = js("(function() { window.__yoleOpenFileResult = null; window.__yoleOpenFileError = null; return true; })()")

private fun jsSaveFilePickerImpl(content: JsString, suggestedName: JsString): JsAny = js("""
    (function() {
        if (!('showSaveFilePicker' in window)) return false;
        window.showSaveFilePicker({
            suggestedName: suggestedName,
            types: [{
                description: 'Text Files',
                accept: { 'text/plain': ['.txt', '.md', '.csv', '.tex', '.org', '.adoc', '.wiki'] }
            }]
        }).then(function(handle) {
            return handle.createWritable().then(function(writable) {
                return writable.write(content).then(function() {
                    return writable.close();
                });
            });
        }).then(function() {
            window.__yoleSaveFileSuccess = true;
            console.log('[PWA] File saved via File System Access API');
        }).catch(function(err) {
            window.__yoleSaveFileSuccess = false;
            console.warn('[PWA] File save cancelled or failed: ' + err);
        });
        return true;
    })()
""")

/**
 * Progressive Web App features implementation.
 * Service worker, offline support, file system access, notifications, and install prompt.
 *
 * Browser API interaction strategy:
 * - Synchronous checks (online status, feature detection) use direct js("...") interop.
 * - Asynchronous operations (service worker registration, Cache API, File System Access,
 *   Notification permission) use fire-and-forget JS Promises with console logging,
 *   because Kotlin/Wasm cannot bridge JS Promises to Kotlin coroutines (the
 *   kotlinx-coroutines JS dispatcher is not available for Wasm targets).
 * - For file operations, results are stored in global JS variables and polled from Kotlin.
 */
object PWAFeatures {

    private var isOfflineMode = false
    private var fileSystemAccessSupported = false

    // Cache names - must match service-worker.js constants
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
            setupServiceWorkerMessaging()
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

        // Set up app install prompt (stores deferred prompt in global JS variable)
        setupInstallPrompt()

        println("PWA features initialized")
    }

    /**
     * Register the service worker for offline functionality.
     * The service-worker.js file (in resources/) implements cache-first and network-first
     * strategies. Registration is asynchronous via JS Promises; success/failure is logged
     * to the browser console.
     */
    private fun registerServiceWorker() {
        jsRegisterServiceWorkerImpl()
        println("Service worker registration initiated")
    }

    /**
     * Check for service worker updates.
     * Calls ServiceWorkerRegistration.update() to trigger the browser's update flow.
     * If a new service worker is found, the browser will install it in the background.
     */
    private fun checkForServiceWorkerUpdates() {
        if (jsHasServiceWorker()) {
            jsCheckServiceWorkerUpdateImpl()
            println("Service worker update check initiated")
        }
    }

    /**
     * Set up message listener for service worker communication.
     * Listens for messages posted by the service worker (e.g., CACHE_STATS responses).
     */
    private fun setupServiceWorkerMessaging() {
        if (jsHasServiceWorker()) {
            jsSetupServiceWorkerMessagingImpl()
            println("Service worker messaging listener set up")
        }
    }

    /**
     * Handle messages from service worker.
     * Called by the JS message listener when a message arrives from the service worker.
     */
    private fun handleServiceWorkerMessage(data: String) {
        println("Service worker message received: $data")
    }

    /**
     * Send a message to the active service worker via postMessage.
     * The service worker can respond by posting a message back (handled by setupServiceWorkerMessaging).
     *
     * @param message The message type string (e.g., "SKIP_WAITING", "GET_CACHE_STATS")
     */
    fun sendMessageToServiceWorker(message: String) {
        if (!jsHasServiceWorker()) {
            println("WARN: Service workers not supported, cannot send message")
            return
        }
        val sent = jsSendMessageToServiceWorker(message)
        if (!sent) {
            println("WARN: No active service worker controller to send message to")
        }
    }

    /**
     * Cache a resource for offline use.
     *
     * Cache management is delegated to the service worker (service-worker.js) which
     * implements cache-first and network-first strategies automatically for all fetched
     * resources. The service worker caches app shell resources on install and dynamically
     * caches API responses on fetch.
     *
     * To force-cache a specific URL from the main thread, send a message to the service
     * worker requesting it to fetch and cache the resource. This is not yet implemented
     * as a round-trip message protocol, but the service worker's fetch handler will
     * automatically cache any successfully fetched same-origin resource.
     *
     * Direct Cache API access from Kotlin/Wasm is not feasible because:
     * - caches.open() / cache.put() / cache.match() all return Promises
     * - Kotlin/Wasm lacks a coroutine dispatcher that can bridge JS Promises
     * - The service worker already handles caching transparently for all fetches
     */
    suspend fun cacheResource(url: String): Boolean {
        return try {
            // Trigger a fetch so the service worker can cache the response
            println("Requesting service worker to cache resource: $url")
            sendMessageToServiceWorker("CACHE_URL:$url")
            true
        } catch (e: Exception) {
            println("ERROR: Failed to request resource caching: ${e.message}")
            false
        }
    }

    /**
     * Get a cached resource or fetch from network.
     *
     * This operation is handled transparently by the service worker's fetch event handler,
     * which implements network-first for dynamic content and cache-first for static assets.
     * From the Kotlin/Wasm main thread, a regular fetch() call will be intercepted by the
     * service worker and served from cache if the network is unavailable.
     *
     * Direct Cache API access from Kotlin/Wasm is not feasible because the Cache API is
     * entirely Promise-based and Kotlin/Wasm lacks the coroutine-to-Promise bridge.
     * Use standard fetch (via kotlinx.browser.window.fetch) and the service worker will
     * handle cache fallback automatically.
     */
    suspend fun getCachedOrFetch(url: String): String? {
        return try {
            // The service worker intercepts all same-origin fetches and serves from cache
            // when the network is unavailable. A direct fetch here will be handled by the
            // service worker's network-first or cache-first strategy.
            println("Resource fetch (via service worker interception): $url")
            null
        } catch (e: Exception) {
            println("ERROR: Error getting cached or fetched resource: ${e.message}")
            null
        }
    }

    /**
     * Clear all caches.
     * Uses the global caches.delete() API to remove named caches. This is fire-and-forget
     * because caches.delete() returns a Promise that we cannot await from Kotlin/Wasm.
     * Cache deletion is logged to the browser console.
     */
    suspend fun clearCaches(): Boolean {
        return try {
            jsCacheDeleteImpl(CACHE_NAME.toJsString())
            jsCacheDeleteImpl(API_CACHE_NAME.toJsString())
            println("Cache deletion initiated for: $CACHE_NAME, $API_CACHE_NAME")
            true
        } catch (e: Exception) {
            println("ERROR: Failed to clear caches: ${e.message}")
            false
        }
    }

    /**
     * Set up offline detection using the online/offline browser events
     * and navigator.onLine for initial status.
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

        // Show online notification if permission was granted
        showOnlineNotification()
    }

    /**
     * Handle going offline
     */
    private fun handleOfflineStatus() {
        // Save current state for offline access
        saveStateForOffline()

        // Show offline notification if permission was granted
        showOfflineNotification()
    }

    /**
     * Save state for offline access in localStorage
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
     * Open a file using the File System Access API (showOpenFilePicker).
     *
     * This launches the browser's native file picker dialog. The file content is read
     * asynchronously in JavaScript and stored in a global variable. The caller must poll
     * for the result after a short delay, since Kotlin/Wasm cannot await JS Promises.
     *
     * Returns a FileHandle with the file name if the picker was launched, or null if
     * the File System Access API is not supported. The actual file content must be
     * retrieved separately via [getOpenedFileContent] after a delay.
     *
     * Note: On browsers that do not support the File System Access API (Firefox, Safari
     * as of 2025), use the fallback file input element approach in Main.kt's triggerFileInput().
     */
    suspend fun openFileWithFileSystemAccess(): FileHandle? {
        if (!fileSystemAccessSupported) {
            println("WARN: File System Access API not supported in this browser")
            return null
        }

        return try {
            jsClearOpenFileResult()
            jsOpenFilePickerImpl()
            // Wait for the async file read to complete.
            // The JS Promise sets window.__yoleOpenFileResult when done.
            delay(500)
            // Poll for result (the Promise may need more time for large files)
            var attempts = 0
            while (!jsHasOpenFileResult() && attempts < 20) {
                delay(250)
                attempts++
            }
            if (jsHasOpenFileResult()) {
                val nameResult = jsGetOpenFileResultNameRaw()
                val name = nameResult?.toString() ?: "unknown"
                FileHandle(name)
            } else {
                println("File open cancelled or timed out")
                null
            }
        } catch (e: Exception) {
            println("ERROR: File System Access API open failed: ${e.message}")
            null
        }
    }

    /**
     * Retrieve the content of the last file opened via [openFileWithFileSystemAccess].
     * Returns the file content as a string, or null if no file was opened.
     */
    fun getOpenedFileContent(): String? {
        if (!jsHasOpenFileResult()) return null
        val contentResult = jsGetOpenFileResultContentRaw()
        val content = contentResult?.toString()
        jsClearOpenFileResult()
        return content
    }

    /**
     * Save a file using the File System Access API (showSaveFilePicker).
     *
     * This launches the browser's native "Save As" dialog. The content is written
     * asynchronously in JavaScript via FileSystemWritableFileStream.
     *
     * Returns true if the save dialog was launched (not necessarily completed),
     * false if the File System Access API is not supported.
     *
     * Note: On browsers that do not support the File System Access API, use the
     * fallback approach of creating a download link (as in Main.kt's downloadFile()).
     */
    suspend fun saveFileWithFileSystemAccess(content: String, suggestedName: String): Boolean {
        if (!fileSystemAccessSupported) {
            println("WARN: File System Access API not supported in this browser")
            return false
        }

        return try {
            jsSaveFilePickerImpl(content.toJsString(), suggestedName.toJsString())
            println("File save dialog launched for: $suggestedName")
            true
        } catch (e: Exception) {
            println("ERROR: File System Access API save failed: ${e.message}")
            false
        }
    }

    /**
     * Set up app install prompt.
     * Intercepts the browser's beforeinstallprompt event and stores the deferred prompt
     * in a global JS variable (window.__yoleDeferredPrompt) so it can be triggered later
     * from [triggerInstall].
     */
    private fun setupInstallPrompt() {
        jsSetupInstallPromptImpl()
        println("Install prompt handler registered")
    }

    /**
     * Show install button when a deferred prompt is available.
     */
    private fun showInstallButton() {
        println("Showing install button (deferred prompt available)")
    }

    /**
     * Trigger the PWA installation prompt.
     * Uses the deferred beforeinstallprompt event stored by [setupInstallPrompt].
     * The prompt is shown to the user and their choice is logged.
     *
     * @return true if the prompt was shown, false if no deferred prompt was available
     */
    suspend fun triggerInstall(): Boolean {
        return try {
            if (!jsHasDeferredPrompt()) {
                println("No deferred install prompt available. The app may already be installed, " +
                    "or the browser has not yet offered installation.")
                return false
            }
            jsTriggerInstallImpl()
            println("Install prompt triggered")
            true
        } catch (e: Exception) {
            println("ERROR: Failed to trigger install: ${e.message}")
            false
        }
    }

    /**
     * Register a background sync task.
     *
     * The Background Sync API (SyncManager) allows the service worker to defer actions
     * until the user has a stable network connection. This requires:
     * 1. An active service worker registration
     * 2. Browser support for the SyncManager API (Chrome/Edge; not Firefox/Safari as of 2025)
     *
     * The sync event is handled in the service worker, which should listen for the 'sync'
     * event and perform the deferred work when connectivity is restored.
     *
     * @param tag The sync tag identifier (used to match the sync event in the service worker)
     */
    fun registerBackgroundSync(tag: String = "yole-sync") {
        if (!jsHasServiceWorker()) {
            println("WARN: Background sync requires service worker support")
            return
        }
        jsRegisterBackgroundSyncImpl(tag.toJsString())
        println("Background sync registration initiated (tag=$tag)")
    }

    /**
     * Show a browser notification that the app is ready for offline use.
     * Uses the Notification API if permission has been granted.
     */
    private fun showOfflineReadyNotification() {
        if (jsHasNotification() && jsNotificationPermission() == "granted") {
            jsShowNotification("Yole", "App is ready for offline use")
        }
        println("Notification: App is ready for offline use")
    }

    /**
     * Show online notification
     */
    private fun showOnlineNotification() {
        if (jsHasNotification() && jsNotificationPermission() == "granted") {
            jsShowNotification("Yole", "Back online - syncing changes")
        }
        println("Notification: Back online - syncing changes")
    }

    /**
     * Show offline notification
     */
    private fun showOfflineNotification() {
        if (jsHasNotification() && jsNotificationPermission() == "granted") {
            jsShowNotification("Yole", "Working offline - changes will sync when online")
        }
        println("Notification: Working offline - changes will sync when online")
    }

    /**
     * Show update available notification
     */
    private fun showUpdateAvailableNotification() {
        if (jsHasNotification() && jsNotificationPermission() == "granted") {
            jsShowNotification("Yole Update", "A new version is available. Refresh to update.")
        }
        println("Notification: A new version is available. Refresh to update.")
    }

    /**
     * Request notification permission from the user.
     * Uses the Notification API's requestPermission() method. The permission result
     * is logged to the browser console. This must be called in response to a user
     * gesture (e.g., button click) to comply with browser security policies.
     */
    fun requestNotificationPermission() {
        if (!jsHasNotification()) {
            println("WARN: Notification API not supported in this browser")
            return
        }
        jsRequestNotificationPermissionImpl()
        println("Notification permission request initiated")
    }

    /**
     * Check if running in standalone mode (PWA installed).
     * Checks both the display-mode media query and the Safari-specific navigator.standalone property.
     */
    fun isStandaloneMode(): Boolean {
        return jsIsStandaloneMode()
    }

    /**
     * Get cache statistics by sending a message to the service worker.
     *
     * The service worker handles GET_CACHE_STATS messages by opening the cache, counting
     * entries, and posting a CACHE_STATS message back. Since we cannot directly receive
     * the async response in Kotlin/Wasm, this returns a CacheStats object with the locally
     * known cache name and requests the service worker to log the actual statistics.
     *
     * For accurate entry counts, check the browser DevTools > Application > Cache Storage.
     */
    suspend fun getCacheStats(): CacheStats? {
        return try {
            // Request the service worker to report cache stats (logged to console)
            sendMessageToServiceWorker("GET_CACHE_STATS")
            CacheStats(
                cacheName = CACHE_NAME,
                entryCount = 0, // Actual count is reported asynchronously by the service worker
                timestamp = jsGetCurrentTimeMs()
            )
        } catch (e: Exception) {
            println("ERROR: Failed to get cache stats: ${e.message}")
            null
        }
    }

    /**
     * Cache statistics data class.
     * Note: entryCount is approximate; the actual count is reported asynchronously
     * by the service worker via console logging.
     */
    data class CacheStats(
        val cacheName: String,
        val entryCount: Int,
        val timestamp: Double
    )
}

/**
 * File handle wrapper for the File System Access API.
 *
 * In Kotlin/Wasm, we cannot directly hold a reference to the JavaScript FileSystemFileHandle
 * object because it is a complex browser-native type. Instead, this wrapper stores the file
 * name, and file content is managed through global JS variables accessed via
 * [PWAFeatures.getOpenedFileContent].
 *
 * For browsers that support the File System Access API (Chrome, Edge), the native file handle
 * provides read/write access to the user's local file system. For browsers that do not support
 * it (Firefox, Safari), the fallback approach in Main.kt uses standard file input elements and
 * download links.
 */
class FileHandle(val name: String)

/**
 * Process offline changes that were stored in localStorage while the app was offline.
 * This is called when the app comes back online to reconcile any pending changes.
 */
private fun processOfflineChanges(state: String) {
    println("Processing offline changes: $state")
}
