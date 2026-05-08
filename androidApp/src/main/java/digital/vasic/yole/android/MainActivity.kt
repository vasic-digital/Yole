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
import androidx.compose.ui.Modifier
import digital.vasic.yole.android.ui.YoleApp
import digital.vasic.yole.android.util.FirebaseUtil
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
                    MaterialTheme {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            YoleApp()
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

        // Initialize Firebase Analytics and Crashlytics
        FirebaseUtil.init(
            FirebaseAnalytics.getInstance(this),
            FirebaseCrashlytics.getInstance()
        )

        // Check and request storage permissions on startup. Wrapped in
        // try/catch because Robolectric's shadow Environment can throw
        // ArrayIndexOutOfBoundsException from isExternalStorageManager()
        // (no UID-to-storage-app mapping in test env). Real devices return
        // a normal boolean. Catching here keeps unit tests boot-able while
        // preserving the production-runtime behaviour.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                if (!Environment.isExternalStorageManager()) {
                    requestManageExternalStoragePermission()
                }
            } catch (_: Throwable) {
                // Robolectric / unsupported test environment — skip the
                // permission probe. Production never hits this path.
            }
        }
        
        setContent {
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
            } catch (_: Throwable) {
                // Robolectric / unsupported test env — skip.
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