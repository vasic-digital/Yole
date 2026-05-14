/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Main Activity for Yole Android App
 * Modern Android app with Compose Multiplatform
 *
 *########################################################*/

package digital.vasic.yole.android

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import digital.vasic.yole.android.ui.YoleApp
import digital.vasic.yole.android.util.FirebaseUtil
import digital.vasic.yole.syntax.theme.ThemeProvider
import digital.vasic.yole.syntax.theme.ThemeRegistry
import digital.vasic.yole.util.AppContextHolder
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

class MainActivity : ComponentActivity() {
    
    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // After returning from settings, check if permission was granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                // Permission granted, reload the app content
                setContent {
                    // iter-57 Phase 3: every Composable subtree consuming theme
                    // colors flows through this provider. Loads "Yole Dark" by
                    // default so legacy color values continue to render until
                    // the user picks a different theme.
                    ThemeProvider {
                        LaunchedEffect(Unit) {
                            try {
                                ThemeRegistry.setActive("Yole Dark")
                            } catch (t: Throwable) {
                                android.util.Log.w(
                                    "MainActivity",
                                    "ThemeRegistry init skipped: ${t.message}",
                                )
                            }
                        }
                        MaterialTheme {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                YoleApp()
                            }
                        }
                    }
                }
            } else {
                // Permission not granted, request again
                requestManageExternalStoragePermission()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContextHolder.context = applicationContext

        // Initialize Firebase Analytics, Crashlytics, Performance + Remote Config.
        // Wrapped in try/catch so the app launches cleanly when Firebase isn't
        // available — most notably under Robolectric (where @Config(manifest=NONE)
        // skips the merged-manifest FirebaseInitProvider). Firebase is non-critical:
        // if it fails to init, the app continues to work; telemetry is silently
        // dropped. Without this guard, MainActivity.onCreate would throw
        // IllegalStateException at FirebaseCrashlytics.getInstance() and
        // every UI test fails.
        try {
            FirebaseUtil.init(
                FirebaseAnalytics.getInstance(this),
                FirebaseCrashlytics.getInstance()
            )
            FirebaseUtil.initPerformanceAndConfig(
                defaults = mapOf(
                    FirebaseUtil.ConfigKeys.EDITOR_OPEN_WARN_BYTES to 5L * 1024 * 1024,  // 5 MB
                    FirebaseUtil.ConfigKeys.BACKUP_RETENTION_DAYS to 30L,
                    FirebaseUtil.ConfigKeys.ENABLE_WASM_EDITOR to false
                )
            )
            FirebaseUtil.fetchRemoteConfig()  // async, no-op on failure
            FirebaseUtil.logEvent(FirebaseUtil.Events.APP_OPEN)
        } catch (t: Throwable) {
            // Robolectric / unsupported test env / Firebase outage — log only.
            // Cannot recordNonFatal here because crashlytics itself failed.
            android.util.Log.w("MainActivity", "Firebase init skipped: ${t.message}")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                if (!Environment.isExternalStorageManager()) {
                    requestManageExternalStoragePermission()
                }
            } catch (t: Throwable) {
                // Robolectric's shadow Environment can throw from
                // isExternalStorageManager(). Production rarely hits this,
                // but if it does, surface to Crashlytics as a non-fatal.
                FirebaseUtil.recordNonFatal(t, "storage permission probe failed at onCreate")
            }
        }
        
        setContent {
            // iter-57 Phase 3: ThemeProvider seeds LocalTheme with the active
            // VS Code theme (default Yole Dark). LaunchedEffect runs once on
            // first composition and is a no-op on Robolectric / missing resources.
            ThemeProvider {
                LaunchedEffect(Unit) {
                    try {
                        ThemeRegistry.setActive("Yole Dark")
                    } catch (t: Throwable) {
                        android.util.Log.w(
                            "MainActivity",
                            "ThemeRegistry init skipped: ${t.message}",
                        )
                    }
                }
                MaterialTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        YoleApp()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check permission every time app resumes and request if not granted.
        // Try/catch protects against Robolectric's shadow Environment which
        // throws ArrayIndexOutOfBoundsException; production-runtime paths
        // return a normal boolean.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                if (!Environment.isExternalStorageManager()) {
                    requestManageExternalStoragePermission()
                }
            } catch (t: Throwable) {
                FirebaseUtil.recordNonFatal(t, "storage permission probe failed at onResume")
            }
        }
    }
    
    private fun requestManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                manageStorageLauncher.launch(intent)
            } catch (e: Exception) {
                // Fallback to general settings if specific package intent fails
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                manageStorageLauncher.launch(intent)
            }
        }
    }
}