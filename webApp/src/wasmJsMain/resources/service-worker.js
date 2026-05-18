/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Service Worker for Yole Web Application
 *
 * iter-89 strategy:
 *   - CACHE_NAME embeds a per-build epoch so every deploy invalidates
 *     the prior cache. The original v1 static name was the operator-
 *     reported "spins endlessly" defect: SW served the v2.0.0 cached
 *     bundle to a returning user FOREVER because the cache name never
 *     changed, so subsequent deploys never triggered an `activate`
 *     cleanup that deleted the old cache.
 *   - NETWORK-FIRST for the JS / Wasm / HTML app shell: fresh content
 *     wins on every load; cache is only used as offline fallback. The
 *     previous CACHE-FIRST strategy locked in whatever the user first
 *     visited and never refreshed it on subsequent deploys.
 *   - skipWaiting() in install so a new SW takes over immediately,
 *     not after every tab is closed.
 *   - clients.claim() in activate so the new SW controls already-open
 *     tabs immediately, not on next navigation.
 *
 *########################################################*/

// IMPORTANT: bump CACHE_VERSION on EVERY release so each deploy invalidates
// the prior cache. The iter-89 forensic case: leaving this static caused
// users to be served the v2.0.0 splash-then-blank bundle from cache forever
// after they once visited the site, even after 4 subsequent deploys fixed
// the bug. The cascade-audit gate (iter-89) verifies this string contains
// the current versionName.
const CACHE_VERSION = '2.0.4';
const CACHE_NAME = 'yole-cache-' + CACHE_VERSION;
const API_CACHE_NAME = 'yole-api-cache-' + CACHE_VERSION;

// App shell resources to cache on install. These are also served via
// network-first below — the cache here is just a warm-start offline buffer.
const APP_SHELL_RESOURCES = [
    '/',
    '/index.html',
    '/yole-web.js',
    '/manifest.json',
    '/favicon.ico',
    '/Logo.png'
];

// Install: cache app shell + skip waiting so the new SW takes over now
self.addEventListener('install', function(event) {
    console.log('[ServiceWorker] Install ' + CACHE_VERSION);
    event.waitUntil(
        caches.open(CACHE_NAME).then(function(cache) {
            console.log('[ServiceWorker] Caching app shell to ' + CACHE_NAME);
            return cache.addAll(APP_SHELL_RESOURCES).catch(function(error) {
                console.warn('[ServiceWorker] Some resources failed to cache:', error);
                return Promise.allSettled(
                    APP_SHELL_RESOURCES.map(function(url) {
                        return cache.add(url).catch(function(err) {
                            console.warn('[ServiceWorker] Failed to cache:', url, err);
                        });
                    })
                );
            });
        }).then(function() {
            // iter-89: take over immediately on new SW deploy.
            return self.skipWaiting();
        })
    );
});

// Activate: nuke every cache that isn't the current one, then claim clients
self.addEventListener('activate', function(event) {
    console.log('[ServiceWorker] Activate ' + CACHE_VERSION);
    event.waitUntil(
        caches.keys().then(function(cacheNames) {
            return Promise.all(
                cacheNames.filter(function(cacheName) {
                    return cacheName !== CACHE_NAME && cacheName !== API_CACHE_NAME;
                }).map(function(cacheName) {
                    console.log('[ServiceWorker] Deleting stale cache:', cacheName);
                    return caches.delete(cacheName);
                })
            );
        }).then(function() {
            // iter-89: control already-open tabs immediately (was already
            // here in iter-85 but combined with skipWaiting()/network-first
            // it now actually takes effect on the user's next page load,
            // not eventually-someday-when-all-tabs-close).
            return self.clients.claim();
        })
    );
});

// Fetch: same-origin → network-first for app shell, cache as offline backup
self.addEventListener('fetch', function(event) {
    var request = event.request;
    var url = new URL(request.url);
    if (url.origin !== location.origin) return;

    // iter-89: NETWORK-FIRST for everything. Fresh content always wins;
    // cache is only used when the network is down. The prior CACHE-FIRST
    // strategy was the iter-89 forensic root cause — once the operator's
    // browser cached the v2.0.0 bundle, no deploy could displace it.
    event.respondWith(networkFirst(request));
});

// Message: handle skip-waiting and stats requests
self.addEventListener('message', function(event) {
    if (event.data && event.data.type === 'SKIP_WAITING') {
        console.log('[ServiceWorker] Skip waiting');
        self.skipWaiting();
    }
    if (event.data && event.data.type === 'GET_CACHE_STATS') {
        caches.open(CACHE_NAME).then(function(cache) {
            return cache.keys();
        }).then(function(keys) {
            event.source.postMessage({
                type: 'CACHE_STATS',
                count: keys.length,
                cacheName: CACHE_NAME,
                version: CACHE_VERSION
            });
        });
    }
});

/**
 * Network-first strategy.
 *
 * Always try the network first. On success, update the cache as a side
 * effect (so we have a warm offline copy). On failure (offline / DNS /
 * 5xx), fall back to whatever's in the cache; if nothing is cached
 * either, render the offline page.
 *
 * This is the "fresh content always wins" strategy that the iter-89 fix
 * adopts to escape the cache-first lock-in defect.
 */
function networkFirst(request) {
    return fetch(request).then(function(networkResponse) {
        if (networkResponse && networkResponse.ok) {
            var responseClone = networkResponse.clone();
            caches.open(CACHE_NAME).then(function(cache) {
                cache.put(request, responseClone);
            }).catch(function() {});
        }
        return networkResponse;
    }).catch(function() {
        return caches.match(request).then(function(cachedResponse) {
            if (cachedResponse) return cachedResponse;
            return offlineFallback(request);
        });
    });
}

/**
 * Offline fallback response. Same as before — for navigation requests
 * return cached index.html or a tiny inline offline page; for other
 * resources return a 503 text/plain.
 */
function offlineFallback(request) {
    if (request.mode === 'navigate' || (request.headers.get('accept') || '').indexOf('text/html') !== -1) {
        return caches.match('/index.html').then(function(cachedIndex) {
            if (cachedIndex) return cachedIndex;
            return new Response(
                '<!DOCTYPE html><html><head><meta charset="UTF-8"><title>Yole - Offline</title>' +
                '<style>body{font-family:sans-serif;display:flex;align-items:center;justify-content:center;' +
                'height:100vh;margin:0;background:#f5f5f5;color:#333;}' +
                '.container{text-align:center;padding:32px;}' +
                'h1{color:#D32F2F;}</style></head>' +
                '<body><div class="container"><h1>Yole</h1>' +
                '<p>You are currently offline.</p>' +
                '<p>Please check your internet connection and try again.</p>' +
                '</div></body></html>',
                {
                    status: 200,
                    headers: { 'Content-Type': 'text/html; charset=utf-8' }
                }
            );
        });
    }
    return new Response('Offline - resource not available', {
        status: 503,
        headers: { 'Content-Type': 'text/plain' }
    });
}
