/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Service Worker for Yole Web Application
 * Offline caching, cache-first for app shell, network-first for dynamic content
 *
 *########################################################*/

const CACHE_NAME = 'yole-cache-v1';
const API_CACHE_NAME = 'yole-api-cache-v1';

// App shell resources to cache on install
const APP_SHELL_RESOURCES = [
    '/',
    '/index.html',
    '/yole-web.js',
    '/manifest.json',
    '/favicon.ico',
    '/Logo.png'
];

// Install event: cache app shell resources
self.addEventListener('install', function(event) {
    console.log('[ServiceWorker] Install');
    event.waitUntil(
        caches.open(CACHE_NAME).then(function(cache) {
            console.log('[ServiceWorker] Caching app shell');
            return cache.addAll(APP_SHELL_RESOURCES).catch(function(error) {
                console.warn('[ServiceWorker] Some resources failed to cache:', error);
                // Cache what we can, don't fail the install
                return Promise.allSettled(
                    APP_SHELL_RESOURCES.map(function(url) {
                        return cache.add(url).catch(function(err) {
                            console.warn('[ServiceWorker] Failed to cache:', url, err);
                        });
                    })
                );
            });
        })
    );
});

// Activate event: clean up old caches
self.addEventListener('activate', function(event) {
    console.log('[ServiceWorker] Activate');
    event.waitUntil(
        caches.keys().then(function(cacheNames) {
            return Promise.all(
                cacheNames.filter(function(cacheName) {
                    // Delete old versioned caches
                    return cacheName !== CACHE_NAME && cacheName !== API_CACHE_NAME;
                }).map(function(cacheName) {
                    console.log('[ServiceWorker] Deleting old cache:', cacheName);
                    return caches.delete(cacheName);
                })
            );
        }).then(function() {
            // Claim all clients so the new service worker takes effect immediately
            return self.clients.claim();
        })
    );
});

// Fetch event: serve from cache or network
self.addEventListener('fetch', function(event) {
    var request = event.request;
    var url = new URL(request.url);

    // Only handle same-origin requests
    if (url.origin !== location.origin) {
        return;
    }

    // Determine caching strategy based on resource type
    if (isAppShellResource(url.pathname)) {
        // Cache-first strategy for app shell resources
        event.respondWith(cacheFirst(request));
    } else {
        // Network-first strategy for dynamic content
        event.respondWith(networkFirst(request));
    }
});

// Message event: handle skip-waiting and other messages
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
                cacheName: CACHE_NAME
            });
        });
    }
});

/**
 * Check if a path is an app shell resource
 */
function isAppShellResource(pathname) {
    // Exact matches
    if (APP_SHELL_RESOURCES.indexOf(pathname) !== -1) {
        return true;
    }
    // Static assets (JS, CSS, images, fonts, wasm)
    if (/\.(js|css|png|jpg|jpeg|gif|svg|ico|woff|woff2|ttf|eot|wasm)$/.test(pathname)) {
        return true;
    }
    return false;
}

/**
 * Cache-first strategy: try cache, fall back to network
 * Used for app shell resources that rarely change
 */
function cacheFirst(request) {
    return caches.match(request).then(function(cachedResponse) {
        if (cachedResponse) {
            // Return cached response and update cache in background
            fetchAndCache(request);
            return cachedResponse;
        }
        // Not in cache, fetch from network
        return fetchAndCache(request);
    }).catch(function() {
        // Both cache and network failed, return offline fallback
        return offlineFallback(request);
    });
}

/**
 * Network-first strategy: try network, fall back to cache
 * Used for dynamic content that should be fresh when possible
 */
function networkFirst(request) {
    return fetch(request).then(function(networkResponse) {
        // Clone the response before caching (response can only be consumed once)
        if (networkResponse && networkResponse.ok) {
            var responseClone = networkResponse.clone();
            caches.open(API_CACHE_NAME).then(function(cache) {
                cache.put(request, responseClone);
            });
        }
        return networkResponse;
    }).catch(function() {
        // Network failed, try cache
        return caches.match(request).then(function(cachedResponse) {
            if (cachedResponse) {
                return cachedResponse;
            }
            // No cache either, return offline fallback
            return offlineFallback(request);
        });
    });
}

/**
 * Fetch a resource and cache it for future use
 */
function fetchAndCache(request) {
    return fetch(request).then(function(networkResponse) {
        if (networkResponse && networkResponse.ok) {
            var responseClone = networkResponse.clone();
            caches.open(CACHE_NAME).then(function(cache) {
                cache.put(request, responseClone);
            });
        }
        return networkResponse;
    });
}

/**
 * Offline fallback response
 * Returns a basic HTML page when both cache and network are unavailable
 */
function offlineFallback(request) {
    // For navigation requests (HTML pages), return the cached index.html
    if (request.mode === 'navigate' || request.headers.get('accept').indexOf('text/html') !== -1) {
        return caches.match('/index.html').then(function(cachedIndex) {
            if (cachedIndex) {
                return cachedIndex;
            }
            // Last resort: generate a minimal offline page
            return new Response(
                '<!DOCTYPE html><html><head><meta charset="UTF-8"><title>Yole - Offline</title>' +
                '<style>body{font-family:sans-serif;display:flex;align-items:center;justify-content:center;' +
                'height:100vh;margin:0;background:#f5f5f5;color:#333;}' +
                '.container{text-align:center;padding:32px;}' +
                'h1{color:#1976d2;}</style></head>' +
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

    // For other resources, return a simple error response
    return new Response('Offline - resource not available', {
        status: 503,
        headers: { 'Content-Type': 'text/plain' }
    });
}
