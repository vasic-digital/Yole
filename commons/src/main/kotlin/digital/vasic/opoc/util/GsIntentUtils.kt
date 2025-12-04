/*#######################################################
 *
 * SPDX-FileCopyrightText: 2016-2025 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2016-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 * Migrated to Kotlin 2025 by Milos Vasic
 *
 * To the extent possible under law, the author(s) have dedicated all copyright
 * and related and neighboring rights to this software to the public domain worldwide.
 * This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 #########################################################*/
package digital.vasic.opoc.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.CalendarContract
import android.provider.MediaStore
import android.text.TextUtils
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import digital.vasic.opoc.format.GsTextUtils
import digital.vasic.opoc.wrapper.GsCallback
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.Random

/**
 * Intent and activity utilities for Android applications.
 *
 * Provides functionality for:
 * - Activity and intent launching
 * - Content sharing (text, files, images)
 * - Communication intents (email, phone, SMS)
 * - App and package operations
 * - Calendar events
 * - Camera and media capture
 * - File operations via intents
 * - Chrome Custom Tabs
 * - System intents and shortcuts
 * - Broadcast receivers
 *
 * Extracted from GsContextUtils as part of modularization.
 * Depends on: GsResourceUtils, GsStorageUtils, GsImageUtils
 */
@Suppress("unused", "MemberVisibilityCanBePrivate", "DEPRECATION")
object GsIntentUtils {

    //########################
    //## Constants
    //########################

    private var m_chooserTitle = "➥"
    private var _lastCameraPictureFilepath: String? = null
    private var _receivePathCallback: WeakReference<GsCallback.a1<String>>? = null

    //########################
    //## Activity & Intent Starting
    //########################

    /**
     * Restart the current app. Supply the class to start on startup.
     *
     * @param context Android context
     * @param classToStart Activity class to start on restart
     */
    @JvmStatic
    fun restartApp(context: Context, classToStart: Class<*>) {
        val intent = Intent(context, classToStart)
        var flags = PendingIntent.FLAG_CANCEL_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        val pendi = PendingIntent.getActivity(context, 555, intent, flags)
        val mgr = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

        if (context is Activity) {
            context.finish()
        }

        if (mgr != null) {
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendi)
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        Runtime.getRuntime().exit(0)
    }

    /**
     * Send an [Intent.ACTION_VIEW] Intent with given parameter.
     * If the parameter is a string, a browser will get triggered.
     *
     * @param context Android context
     * @param url URL to open
     */
    @JvmStatic
    fun openWebpageInExternalBrowser(context: Context, url: String) {
        try {
            startActivity(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Animate to specified Activity.
     *
     * @param context Activity context
     * @param to The class of the activity
     * @param finishFromActivity true: Finish the current activity
     * @param requestCode Request code for starting the activity, not waiting for result if null
     */
    @JvmStatic
    fun animateToActivity(
        context: Activity,
        to: Class<*>,
        finishFromActivity: Boolean?,
        requestCode: Int?
    ) {
        animateToActivity(context, Intent(context, to), finishFromActivity, requestCode)
    }

    /**
     * Animate to Activity specified in intent.
     * Requires animation resources.
     *
     * @param context Activity context
     * @param intent Intent to start an activity
     * @param finishFromActivity true: Finish the current activity
     * @param requestCode Request code for starting the activity, not waiting for result if null
     */
    @JvmStatic
    fun animateToActivity(
        context: Activity,
        intent: Intent,
        finishFromActivity: Boolean?,
        requestCode: Int?
    ) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        if (requestCode != null) {
            context.startActivityForResult(intent, requestCode)
        } else {
            context.startActivity(intent)
        }
        context.overridePendingTransition(
            GsResourceUtils.getResId(context, GsResourceUtils.ResType.DIMEN, "fadein"),
            GsResourceUtils.getResId(context, GsResourceUtils.ResType.DIMEN, "fadeout")
        )
        if (finishFromActivity == true) {
            context.finish()
        }
    }

    /**
     * Set chooser title for share dialogs.
     *
     * @param title Chooser title
     */
    @JvmStatic
    fun setChooserTitle(title: String) {
        m_chooserTitle = title
    }

    /**
     * Start activity specified by Intent.
     * Add FLAG_ACTIVITY_NEW_TASK if passed context is not an Activity.
     *
     * @param context Context, preferably an Activity
     * @param intent Intent
     */
    @JvmStatic
    fun startActivity(context: Context, intent: Intent) {
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    //########################
    //## Sharing Intents
    //########################

    /**
     * Allow to choose a handling app for given intent.
     *
     * @param context Android context
     * @param intent Thing to be shared
     * @param chooserText The title text for the chooser, or null for default
     */
    @JvmStatic
    fun showChooser(context: Context, intent: Intent, chooserText: String?) {
        try {
            startActivity(context, Intent.createChooser(intent, chooserText ?: m_chooserTitle))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Share text with given mime-type.
     *
     * @param context Android context
     * @param text The text to share
     * @param mimeType MimeType or null (uses text/plain)
     */
    @JvmStatic
    fun shareText(context: Context, text: String, mimeType: String?) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_TEXT, text)
        intent.type = mimeType ?: GsResourceUtils.MIME_TEXT_PLAIN
        showChooser(context, intent, null)
    }

    //########################
    //## Communication Intents
    //########################

    /**
     * Draft an email with given data. Unknown data can be supplied as null.
     * This will open a chooser with installed mail clients.
     *
     * @param context Android context
     * @param subject Subject (top/title) text to be prefilled in the mail
     * @param body Body (content) text to be prefilled in the mail
     * @param to Recipients to be prefilled in the mail
     */
    @JvmStatic
    fun draftEmail(context: Context, subject: String?, body: String?, vararg to: String?) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:")
        if (subject != null) {
            intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        if (body != null) {
            intent.putExtra(Intent.EXTRA_TEXT, body)
        }
        if (to.isNotEmpty() && to[0] != null) {
            intent.putExtra(Intent.EXTRA_EMAIL, to)
        }
        showChooser(context, intent, null)
    }

    /**
     * Call telephone number.
     * Non-direct call opens up the dialer and pre-sets the telephone number.
     * Direct call requires M permission granted and manifest permission:
     * <uses-permission android:name="android.permission.CALL_PHONE" />
     *
     * @param context Activity context
     * @param telNo The telephone number to call
     * @param directCall Direct call number if possible
     */
    @JvmStatic
    fun callTelephoneNumber(context: Activity, telNo: String, vararg directCall: Boolean) {
        var ldirectCall = if (directCall.isNotEmpty()) directCall[0] else true
        val cleanedTelNo = telNo.replace(Regex("(?i)(tel:?)+"), "")

        if (Build.VERSION.SDK_INT >= 23 && ldirectCall) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(context, arrayOf(Manifest.permission.CALL_PHONE), 4001)
                ldirectCall = false
            } else {
                try {
                    val callIntent = Intent(Intent.ACTION_CALL)
                    callIntent.data = Uri.parse("tel:$cleanedTelNo")
                    startActivity(context, callIntent)
                } catch (ignored: Exception) {
                    ldirectCall = false
                }
            }
        }

        // Show dialer with telephone number pre-inserted
        if (!ldirectCall) {
            startActivity(context, Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", cleanedTelNo, null)))
        }
    }

    //########################
    //## App & Package Intents
    //########################

    /**
     * Set launcher activity enabled/disabled state.
     *
     * @param context Android context
     * @param activityClass Activity class
     * @param enable Whether to enable or disable
     */
    @JvmStatic
    fun setLauncherActivityEnabled(context: Context, activityClass: Class<*>, enable: Boolean) {
        try {
            val component = ComponentName(context, activityClass)
            context.packageManager.setComponentEnabledSetting(
                component,
                if (enable) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (ignored: Exception) {
        }
    }

    /**
     * Set launcher activity enabled/disabled state from string class name.
     *
     * @param context Android context
     * @param activityClass Activity class name as string
     * @param enable Whether to enable or disable
     */
    @JvmStatic
    fun setLauncherActivityEnabledFromString(context: Context, activityClass: String, enable: Boolean) {
        try {
            val component = ComponentName(context, activityClass)
            context.packageManager.setComponentEnabledSetting(
                component,
                if (enable) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (ignored: Exception) {
        }
    }

    /**
     * Check if launcher activity is enabled.
     *
     * @param context Android context
     * @param activityClass Activity class
     * @return True if enabled
     */
    @JvmStatic
    fun isLauncherEnabled(context: Context, activityClass: Class<*>): Boolean {
        return try {
            val component = ComponentName(context, activityClass)
            context.packageManager.getComponentEnabledSetting(component) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } catch (ignored: Exception) {
            false
        }
    }

    /**
     * Request installation of APK specified by file.
     * Permission required:
     * <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
     *
     * @param context Android context
     * @param file The apk file to install
     * @return True if installation request was successful
     */
    @JvmStatic
    fun requestApkInstallation(context: Context, file: File?): Boolean {
        if (file == null || !file.name.lowercase().endsWith(".apk")) {
            return false
        }

        var fileUri: Uri? = null
        try {
            fileUri = FileProvider.getUriForFile(context, GsStorageUtils.getFileProvider(context), file)
        } catch (ignored: Exception) {
            try {
                fileUri = Uri.fromFile(file)
            } catch (ignored2: Exception) {
            }
        }

        if (fileUri != null) {
            val MIME_TYPE_APK = "application/vnd.android.package-archive"

            var hasRequestInstallPackagesPermission = true
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.packageManager.canRequestPackageInstalls()
                }
            } catch (ignored: Exception) {
                hasRequestInstallPackagesPermission = false
            }

            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !hasRequestInstallPackagesPermission -> {
                    GsStorageUtils.shareStream(context, file, MIME_TYPE_APK)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                    startActivity(
                        context,
                        Intent(Intent.ACTION_INSTALL_PACKAGE)
                            .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            .setDataAndType(fileUri, MIME_TYPE_APK)
                    )
                }
                else -> {
                    startActivity(
                        context,
                        Intent(Intent.ACTION_VIEW)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .setDataAndType(fileUri, MIME_TYPE_APK)
                    )
                }
            }
            return true
        }
        return false
    }

    //########################
    //## Calendar Events
    //########################

    /**
     * Start calendar application to add new event, with given details prefilled.
     *
     * @param context Android context
     * @param title Event title
     * @param description Event description
     * @param location Event location
     * @param startAndEndTime Start and end time in milliseconds
     * @return True if calendar app was opened successfully
     */
    @JvmStatic
    fun createCalendarAppointment(
        context: Context,
        title: String?,
        description: String?,
        location: String?,
        vararg startAndEndTime: Long
    ): Boolean {
        val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)

        if (title != null) {
            intent.putExtra(CalendarContract.Events.TITLE, title)
        }
        if (description != null) {
            val truncatedDesc = if (description.length > 800) description.substring(0, 800) else description
            intent.putExtra(CalendarContract.Events.DESCRIPTION, truncatedDesc)
        }
        if (location != null) {
            intent.putExtra(CalendarContract.Events.EVENT_LOCATION, location)
        }
        if (startAndEndTime.isNotEmpty()) {
            if (startAndEndTime[0] > 0) {
                intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startAndEndTime[0])
            }
            if (startAndEndTime.size > 1 && startAndEndTime[1] > 0) {
                intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startAndEndTime[1])
            }
        }

        return try {
            startActivity(context, intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    //########################
    //## Camera & Media Capture
    //########################

    /**
     * Request a picture from gallery.
     * Result will be available from Activity.onActivityResult.
     *
     * @param activity Activity context
     * @param callback Callback with file path
     */
    @JvmStatic
    fun requestGalleryPicture(activity: Activity, callback: GsCallback.a1<String>) {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        try {
            activity.startActivityForResult(intent, GsStorageUtils.REQUEST_PICK_PICTURE)
            setPathCallback(callback)
        } catch (ex: Exception) {
            Toast.makeText(activity, "No gallery app installed!", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Request audio recording.
     *
     * @param activity Activity context
     * @param callback Callback with file path
     * @return True if recording app was opened
     */
    @JvmStatic
    fun requestAudioRecording(activity: Activity, callback: GsCallback.a1<String>): Boolean {
        val intent = Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)
        return try {
            activity.startActivityForResult(intent, GsStorageUtils.REQUEST_RECORD_AUDIO)
            setPathCallback(callback)
            true
        } catch (ignored: Exception) {
            false
        }
    }

    /**
     * Request a picture from camera-like apps.
     * Result will be available from Activity.onActivityResult.
     *
     * @param activity Activity context
     * @param callback Callback with file path
     */
    @JvmStatic
    fun requestCameraPicture(activity: Activity, callback: GsCallback.a1<String>) {
        try {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val picDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return
            val name = GsFileUtils.getFilenameWithTimestamp("IMG", "", ".jpg")
            val imageTemp = GsFileUtils.findNonConflictingDest(picDir, name)

            if (takePictureIntent.resolveActivity(activity.packageManager) != null && imageTemp.createNewFile()) {
                imageTemp.deleteOnExit()

                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(activity, GsStorageUtils.getFileProvider(activity), imageTemp)
                } else {
                    Uri.fromFile(imageTemp)
                }

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                takePictureIntent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
                activity.startActivityForResult(takePictureIntent, GsStorageUtils.REQUEST_CAMERA_PICTURE)
                _lastCameraPictureFilepath = imageTemp.absolutePath
                setPathCallback(callback)
            }
        } catch (ignored: IOException) {
        }
    }

    private fun setPathCallback(callback: GsCallback.a1<String>) {
        _receivePathCallback = WeakReference(callback)
    }

    /**
     * Send path to registered callback (used internally).
     *
     * @param path File path
     */
    @JvmStatic
    fun sendPathCallback(path: String?) {
        if (!GsTextUtils.isNullOrEmpty(path) && _receivePathCallback != null) {
            val cb = _receivePathCallback?.get()
            cb?.callback(path!!)
        }
        _receivePathCallback = null
    }

    /**
     * Get the last camera picture filepath (used internally).
     *
     * @return Last camera picture filepath
     */
    @JvmStatic
    fun getLastCameraPictureFilepath(): String? {
        return _lastCameraPictureFilepath
    }

    //########################
    //## Chrome Custom Tabs
    //########################

    /**
     * By default Chrome Custom Tabs only uses Chrome Stable to open links.
     * This method changes the custom tab intent to use an available compatible browser.
     *
     * @param context Android context
     * @param customTabIntent Custom tab intent to modify
     */
    @JvmStatic
    fun enableChromeCustomTabsForOtherBrowsers(context: Context, customTabIntent: Intent) {
        val checkpkgs = arrayOf(
            "com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.google.android.apps.chrome", "org.chromium.chrome",
            "org.mozilla.fennec_fdroid", "org.mozilla.firefox", "org.mozilla.firefox_beta", "org.mozilla.fennec_aurora",
            "org.mozilla.klar", "org.mozilla.focus"
        )

        val pm = context.packageManager
        val urlIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com"))
        val browsers = mutableListOf<String>()

        for (ri in pm.queryIntentActivities(urlIntent, 0)) {
            val i = Intent("android.support.customtabs.action.CustomTabsService")
            i.setPackage(ri.activityInfo.packageName)
            if (pm.resolveService(i, 0) != null) {
                browsers.add(ri.activityInfo.packageName)
            }
        }

        val ri = pm.resolveActivity(urlIntent, 0)
        val userDefaultBrowser = ri?.activityInfo?.packageName

        val pkg = when {
            browsers.size == 1 -> browsers[0]
            !TextUtils.isEmpty(userDefaultBrowser) && browsers.contains(userDefaultBrowser) -> userDefaultBrowser
            browsers.isNotEmpty() -> {
                checkpkgs.firstOrNull { browsers.contains(it) } ?: browsers[0]
            }
            else -> null
        }

        if (pkg != null) {
            customTabIntent.setPackage(pkg)
        }
    }

    /**
     * Open webpage in Chrome Custom Tab.
     *
     * @param context Android context
     * @param url URL to open
     * @return True if opened successfully
     */
    @JvmStatic
    @Suppress("DEPRECATION")
    fun openWebpageInChromeCustomTab(context: Context, url: String): Boolean {
        return try {
            val builder = CustomTabsIntent.Builder()
            builder.setToolbarColor(
                GsResourceUtils.rcolor(context, GsResourceUtils.getResId(context, GsResourceUtils.ResType.COLOR, "primary"))
            )
            builder.setSecondaryToolbarColor(
                GsResourceUtils.rcolor(context, GsResourceUtils.getResId(context, GsResourceUtils.ResType.COLOR, "primary_dark"))
            )
            builder.addDefaultShareMenuItem()
            val customTabsIntent = builder.build()
            enableChromeCustomTabsForOtherBrowsers(context, customTabsIntent.intent)
            customTabsIntent.launchUrl(context, Uri.parse(url))
            true
        } catch (ignored: Exception) {
            false
        }
    }

    //########################
    //## Shortcuts
    //########################

    /**
     * Try to create a new desktop shortcut on the launcher.
     * Add permissions:
     * <uses-permission android:name="android.permission.INSTALL_SHORTCUT" />
     * <uses-permission android:name="com.android.launcher.permission.INSTALL_SHORTCUT" />
     *
     * @param context Android context
     * @param intent The intent to be invoked on tap
     * @param iconRes Icon resource for the item
     * @param title Title of the item
     */
    @JvmStatic
    fun createLauncherDesktopShortcut(
        context: Context,
        intent: Intent,
        @DrawableRes iconRes: Int,
        title: String
    ) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        if (intent.action == null) {
            intent.action = Intent.ACTION_VIEW
        }

        val shortcut = ShortcutInfoCompat.Builder(context, Random().nextLong().toString())
            .setIntent(intent)
            .setIcon(IconCompat.createWithResource(context, iconRes))
            .setShortLabel(title)
            .setLongLabel(title)
            .build()
        ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }

    /**
     * Try to create a new desktop shortcut on the launcher (legacy method).
     * This will not work on Api > 25.
     *
     * @param context Android context
     * @param intent The intent to be invoked on tap
     * @param iconRes Icon resource for the item
     * @param title Title of the item
     */
    @JvmStatic
    fun createLauncherDesktopShortcutLegacy(
        context: Context,
        intent: Intent,
        @DrawableRes iconRes: Int,
        title: String
    ) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (intent.action == null) {
            intent.action = Intent.ACTION_VIEW
        }

        val creationIntent = Intent("com.android.launcher.action.INSTALL_SHORTCUT")
        creationIntent.putExtra("duplicate", true)
        creationIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent)
        creationIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, title)
        creationIntent.putExtra(
            Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
            Intent.ShortcutIconResource.fromContext(context, iconRes)
        )
        context.sendBroadcast(creationIntent)
    }

    //########################
    //## Broadcast & Local Broadcast
    //########################

    /**
     * Send a local broadcast (to receive within app), with given action and string-extra+value.
     * This is a convenience method for quickly sending just one thing.
     *
     * @param context Android context
     * @param action Broadcast action
     * @param extra Extra key
     * @param value Extra value
     */
    @JvmStatic
    fun sendLocalBroadcastWithStringExtra(context: Context, action: String, extra: String, value: CharSequence) {
        val intent = Intent(action)
        intent.putExtra(extra, value)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    /**
     * Receive broadcast results via a callback method.
     *
     * @param context Android context
     * @param callback Function to call with received Intent
     * @param autoUnregister Whether to automatically unregister receiver after first match
     * @param filterActions All IntentFilter actions to filter for
     * @return The created BroadcastReceiver instance (must be unregistered on Activity lifecycle events)
     */
    @JvmStatic
    fun receiveResultFromLocalBroadcast(
        context: Context,
        callback: GsCallback.a2<Intent, BroadcastReceiver>,
        autoUnregister: Boolean,
        vararg filterActions: String
    ): BroadcastReceiver {
        val intentFilter = IntentFilter()
        for (filterAction in filterActions) {
            intentFilter.addAction(filterAction)
        }

        val br = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                if (intent != null) {
                    if (autoUnregister) {
                        LocalBroadcastManager.getInstance(context).unregisterReceiver(this)
                    }
                    try {
                        callback.callback(intent, this)
                    } catch (ignored: Exception) {
                    }
                }
            }
        }

        LocalBroadcastManager.getInstance(context).registerReceiver(br, intentFilter)
        return br
    }

    //########################
    //## Helper Methods
    //########################

    /**
     * Show mount SD card dialog.
     *
     * @param context Activity context
     * @param title Dialog title resource id
     * @param description Dialog description resource id
     * @param mountDescriptionGraphic Drawable resource for mount description
     */
    @JvmStatic
    fun showMountSdDialog(
        context: Activity,
        @StringRes title: Int,
        @StringRes description: Int,
        @DrawableRes mountDescriptionGraphic: Int
    ) {
        val imv = ImageView(context)
        imv.setImageResource(mountDescriptionGraphic)
        imv.adjustViewBounds = true

        val dialog = AlertDialog.Builder(context)
        dialog.setView(imv)
        dialog.setTitle(title)
        dialog.setMessage("${context.getString(description)}\n\n")
        dialog.setNegativeButton(android.R.string.cancel, null)
        dialog.setPositiveButton(android.R.string.yes) { _, _ ->
            GsStorageUtils.requestStorageAccessFramework(context)
        }
        dialog.create().show()
    }
}
