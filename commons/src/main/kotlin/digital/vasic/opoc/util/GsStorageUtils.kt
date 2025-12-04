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
import android.content.ClipData
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.text.TextUtils
import android.text.format.DateUtils
import android.util.Pair
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import digital.vasic.opoc.format.GsTextUtils
import digital.vasic.opoc.wrapper.GsCallback
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

/**
 * Storage and file system utilities for Android applications.
 *
 * Provides functionality for:
 * - Storage Access Framework (SAF) handling
 * - External storage and SD card access
 * - File permissions and write access checks
 * - Content resolver and URI operations
 * - File provider integration
 * - Media scanner integration
 * - File metadata extraction
 * - Storage directories management
 *
 * Extracted from GsContextUtils as part of modularization.
 * Depends on: GsResourceUtils
 */
@Suppress("unused", "MemberVisibilityCanBePrivate", "DEPRECATION")
object GsStorageUtils {

    //########################
    //## Constants
    //########################

    @JvmField
    val PREF_KEY__SAF_TREE_URI = "pref_key__saf_tree_uri"

    @JvmField
    val CONTENT_RESOLVER_FILE_PROXY_SEGMENT = "CONTENT_RESOLVER_FILE_PROXY_SEGMENT"

    const val REQUEST_SAF = 50003
    const val REQUEST_CAMERA_PICTURE = 50001
    const val REQUEST_PICK_PICTURE = 50002
    const val REQUEST_RECORD_AUDIO = 50006
    const val REQUEST_STORAGE_PERMISSION_M = 50004
    const val REQUEST_STORAGE_PERMISSION_R = 50005

    const val TEXTFILE_OVERWRITE_MIN_TEXT_LENGTH = 2

    //########################
    //## Private State
    //########################

    private var m_cacheLastExtractFileMetadata: Pair<File, List<Pair<String, String>>>? = null
    private var _lastCameraPictureFilepath: String? = null
    private var _receivePathCallback: WeakReference<GsCallback.a1<String>>? = null

    //########################
    //## Storage & File System
    //########################

    /**
     * Get the private directory for the current package (usually /data/data/package.name/files).
     *
     * @param context Android context
     * @return Private files directory
     */
    @JvmStatic
    fun getAppDataPrivateDir(context: Context): File {
        val filesDir = try {
            val appId = GsResourceUtils.getAppIdFlavorSpecific(context)
            val appInfo = context.packageManager.getPackageInfo(appId, 0).applicationInfo
            File(File(appInfo?.dataDir ?: context.filesDir.parent!!), "files")
        } catch (e: PackageManager.NameNotFoundException) {
            context.filesDir
        }

        if (!filesDir.exists()) {
            filesDir.mkdirs()
        }
        return filesDir
    }

    /**
     * Get public (accessible) app data folders.
     *
     * @param context Android context
     * @param internalStorageFolder Include internal storage folder
     * @param sdcardFolders Include SD card folders
     * @param storageNameWithoutType Return storage name without type prefix
     * @return List of (File, Name) pairs
     */
    @JvmStatic
    fun getAppDataPublicDirs(
        context: Context,
        internalStorageFolder: Boolean,
        sdcardFolders: Boolean,
        storageNameWithoutType: Boolean
    ): List<Pair<File, String>> {
        val dirs = mutableListOf<Pair<File, String>>()
        val externalStorageDir = Environment.getExternalStorageDirectory()

        for (externalFileDir in ContextCompat.getExternalFilesDirs(context, null)) {
            if (externalFileDir == null || externalStorageDir == null) {
                continue
            }
            val isInt = externalFileDir.absolutePath.startsWith(externalStorageDir.absolutePath)
            val add = (internalStorageFolder && isInt) || (sdcardFolders && !isInt)

            if (add) {
                dirs.add(Pair(externalFileDir, getStorageName(externalFileDir, storageNameWithoutType)))
                if (!externalFileDir.exists()) {
                    externalFileDir.mkdirs()
                }
            }
        }
        return dirs
    }

    /**
     * Get human-readable storage name.
     *
     * @param externalFileDir The external file directory
     * @param storageNameWithoutType Return name without type prefix
     * @return Storage name
     */
    @JvmStatic
    fun getStorageName(externalFileDir: File, storageNameWithoutType: Boolean): String {
        val externalStorageDir = Environment.getExternalStorageDirectory()
        val isInt = externalFileDir.absolutePath.startsWith(externalStorageDir.absolutePath)

        val split = externalFileDir.absolutePath.split("/")
        return if (split.size > 2) {
            if (isInt) {
                if (storageNameWithoutType) "Internal Storage" else ""
            } else {
                if (storageNameWithoutType) split[2] else "SD Card (${split[2]})"
            }
        } else {
            "Storage"
        }
    }

    /**
     * Get list of storages (internal and SD cards).
     *
     * @param context Android context
     * @param internalStorageFolder Include internal storage
     * @param sdcardFolders Include SD card folders
     * @return List of (File, Name) pairs
     */
    @JvmStatic
    fun getStorages(context: Context, internalStorageFolder: Boolean, sdcardFolders: Boolean): List<Pair<File, String>> {
        val storages = mutableListOf<Pair<File, String>>()
        for (pair in getAppDataPublicDirs(context, internalStorageFolder, sdcardFolders, true)) {
            if (pair.first.absolutePath.lastIndexOf("/Android/data") > 0) {
                try {
                    val canonicalPath = pair.first.canonicalPath.replaceFirst("/Android/data.*".toRegex(), "")
                    storages.add(Pair(File(canonicalPath), pair.second))
                } catch (ignored: IOException) {
                }
            }
        }
        return storages
    }

    /**
     * Get the storage root folder for a given file.
     *
     * @param context Android context
     * @param file File to find root for
     * @return Storage root folder, or null if not found
     */
    @JvmStatic
    fun getStorageRootFolder(context: Context, file: File): File? {
        val filepath = try {
            file.canonicalPath
        } catch (ignored: Exception) {
            return null
        }

        for (storage in getStorages(context, false, true)) {
            if (filepath.startsWith(storage.first.absolutePath)) {
                return storage.first
            }
        }
        return null
    }

    /**
     * Get all cache directories.
     *
     * @param context Android context
     * @return Collection of cache directories
     */
    @JvmStatic
    fun getCacheDirs(context: Context): Collection<File> {
        val dirs = mutableSetOf<File>()
        dirs.add(context.cacheDir)
        context.externalCacheDir?.let { dirs.add(it) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            dirs.addAll(context.externalCacheDirs.filterNotNull())
        }
        return dirs
    }

    //########################
    //## Media Scanner
    //########################

    /**
     * Request the given paths to be scanned by MediaScanner.
     *
     * @param context Android context
     * @param files Files and folders to scan
     */
    @JvmStatic
    fun mediaScannerScanFile(context: Context, vararg files: File) {
        if (Build.VERSION.SDK_INT > 19) {
            val paths = files.map { it.absolutePath }.toTypedArray()
            MediaScannerConnection.scanFile(context, paths, null, null)
        } else {
            for (file in files) {
                context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
            }
        }
    }

    //########################
    //## File Provider
    //########################

    /**
     * Get all provider infos for the current app.
     *
     * @param context Android context
     * @return List of ProviderInfo
     */
    @JvmStatic
    fun getProvidersInfos(context: Context): List<ProviderInfo> {
        val providers = mutableListOf<ProviderInfo>()
        for (info in context.packageManager.queryContentProviders(null, 0, 0)) {
            if (info.applicationInfo.uid == context.applicationInfo.uid) {
                providers.add(info)
            }
        }
        return providers
    }

    /**
     * Get the file provider authority for the current app.
     *
     * @param context Android context
     * @return FileProvider authority
     * @throws RuntimeException if no FileProvider found
     */
    @JvmStatic
    fun getFileProvider(context: Context): String {
        for (info in getProvidersInfos(context)) {
            if (info.name.matches(Regex("(?i).*fileprovider.*"))) {
                return info.authority
            }
        }
        throw RuntimeException("Error at GsStorageUtils::getFileProvider(context): No FileProvider authority setup")
    }

    //########################
    //## File Sharing
    //########################

    /**
     * Share the given file as stream with given mime-type.
     *
     * @param context Android context
     * @param file The file to share
     * @param mimeType The file's mime type
     * @return True if sharing was successful
     */
    @JvmStatic
    fun shareStream(context: Context, file: File, mimeType: String): Boolean {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(GsResourceUtils.EXTRA_FILEPATH, file.absolutePath)
        intent.type = mimeType

        return try {
            val fileUri = FileProvider.getUriForFile(context, getFileProvider(context), file)
            intent.putExtra(Intent.EXTRA_STREAM, fileUri)
            showChooser(context, intent, null)
            true
        } catch (ignored: Exception) {
            false
        }
    }

    /**
     * Share the given files as stream with given mime-type.
     *
     * @param context Android context
     * @param files The files to share
     * @param mimeType The files mime type (usually wildcard is the best option)
     * @return True if sharing was successful
     */
    @JvmStatic
    fun shareStreamMultiple(context: Context, files: Collection<File>, mimeType: String): Boolean {
        val uris = ArrayList<Uri>()
        for (file in files) {
            uris.add(FileProvider.getUriForFile(context, getFileProvider(context), file))
        }

        return try {
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
            intent.type = mimeType
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            showChooser(context, intent, null)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Open given file in another app.
     *
     * @param context Android context
     * @param file The file to view
     * @param type MIME type (optional, will be detected if null)
     * @return True if successful
     */
    @JvmStatic
    fun viewFileInOtherApp(context: Context, file: File, type: String?): Boolean {
        var fileUri: Uri? = null
        try {
            fileUri = FileProvider.getUriForFile(context, getFileProvider(context), file)
        } catch (ignored: Exception) {
            try {
                fileUri = Uri.fromFile(file)
            } catch (ignored2: Exception) {
            }
        }

        if (fileUri != null) {
            val intent = Intent(Intent.ACTION_VIEW)
            val mimeType = if (TextUtils.isEmpty(type)) {
                GsFileUtils.getMimeType(file)
            } else {
                type!!
            }
            intent.setDataAndType(fileUri, mimeType)
            intent.putExtra(Intent.EXTRA_STREAM, fileUri)
            intent.clipData = ClipData.newRawUri(file.name, fileUri)
            intent.putExtra(GsResourceUtils.EXTRA_FILEPATH, file.absolutePath)
            intent.putExtra(Intent.EXTRA_TITLE, file.name)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            startActivity(context, intent)
            return true
        }
        return false
    }

    private fun showChooser(context: Context, intent: Intent, chooserTitle: String?) {
        context.startActivity(Intent.createChooser(intent, chooserTitle ?: "➥"))
    }

    private fun startActivity(context: Context, intent: Intent) {
        context.startActivity(intent)
    }

    //########################
    //## URI and Intent Helpers
    //########################

    private fun checkPath(path: String?): File? {
        if (path.isNullOrEmpty()) return null
        val f = File(path)
        return if (f.canRead()) f else null
    }

    private fun getUriFromIntent(intent: Intent, context: Context?): Uri? {
        var uri = intent.data
        if (uri == null) {
            uri = intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
        if (uri == null && context != null) {
            uri = ShareCompat.IntentReader(context, intent).stream
        }
        return uri
    }

    /**
     * Try to force extract an absolute filepath from an intent.
     *
     * @param receivingIntent The intent from Activity.getIntent()
     * @param context Android context
     * @return A file or null if extraction did not succeed
     */
    @JvmStatic
    fun extractFileFromIntent(receivingIntent: Intent, context: Context): File? {
        val action = receivingIntent.action
        val type = receivingIntent.type
        val extPath = Environment.getExternalStorageDirectory().absolutePath
        val fileUri = getUriFromIntent(receivingIntent, context)

        var tmps: String
        var fileStr: String?
        var result: File? = null

        if (action == Intent.ACTION_VIEW || action == Intent.ACTION_EDIT ||
            action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE) {

            // Yole, SimpleMobileTools FileManager
            if (receivingIntent.hasExtra(GsResourceUtils.EXTRA_FILEPATH.also { tmps = it })) {
                result = checkPath(receivingIntent.getStringExtra(tmps))
            }

            // Analyze data/Uri
            if (result == null && fileUri != null) {
                fileStr = fileUri.toString()

                // Uri contains file
                if (fileStr.startsWith("file://")) {
                    result = checkPath(fileUri.path)
                }

                if (fileStr.startsWith("content://".also { tmps = it })) {
                    fileStr = fileStr.substring(tmps.length)
                    var fileProvider = fileStr.substring(0, fileStr.indexOf("/"))
                    fileStr = fileStr.substring(fileProvider.length + 1)

                    // Some file managers don't add leading slash
                    if (fileStr.startsWith("storage/")) {
                        fileStr = "/$fileStr"
                    }

                    // Some do add some custom prefix
                    for (prefix in arrayOf("file", "document", "root_files", "name")) {
                        if (fileStr?.startsWith(prefix) == true) {
                            fileStr = fileStr.substring(prefix.length)
                        }
                    }

                    // prefix for External storage
                    for (prefix in arrayOf("external/", "media/", "storage_root/", "external-path/")) {
                        if (result == null && fileStr?.startsWith(prefix.also { tmps = it }) == true) {
                            result = checkPath(Uri.decode("$extPath/${fileStr.substring(tmps.length)}"))
                        }
                    }

                    // Next/OwnCloud Fileprovider
                    for (fp in arrayOf("org.nextcloud.files", "org.nextcloud.beta.files", "org.owncloud.files")) {
                        if (result == null && fileProvider == fp && fileStr?.startsWith("external_files/".also { tmps = it }) == true) {
                            result = checkPath(Uri.decode("/storage/${fileStr.substring(tmps.length).trim()}"))
                        }
                    }

                    // AOSP File Manager/Documents
                    if (result == null && fileProvider == "com.android.externalstorage.documents" &&
                        fileStr?.startsWith("/primary%3A".also { tmps = it }) == true) {
                        result = checkPath(Uri.decode("$extPath/${fileStr.substring(tmps.length)}"))
                    }

                    // Mi File Explorer
                    if (result == null && fileProvider == "com.mi.android.globalFileexplorer.myprovider" &&
                        fileStr?.startsWith("external_files".also { tmps = it }) == true) {
                        result = checkPath(Uri.decode(extPath + fileStr.substring(tmps.length)))
                    }

                    if (result == null && fileStr?.startsWith("external_files/".also { tmps = it }) == true) {
                        for (prefix in arrayOf(extPath, "/storage", "")) {
                            if (result == null) {
                                result = checkPath(Uri.decode("$prefix/${fileStr.substring(tmps.length)}"))
                            }
                        }
                    }

                    // URI Encoded paths with full path after content://package/
                    if (result == null && (fileStr?.startsWith("/") == true || fileStr?.startsWith("%2F") == true)) {
                        result = checkPath(Uri.decode(fileStr))
                        if (result == null) {
                            result = checkPath(fileStr)
                        }
                    }
                }
            }

            if (result == null && fileUri != null) {
                fileUri.path?.let { path ->
                    if (path.startsWith("/")) {
                        result = checkPath(path)
                    }
                }
            }

            // Scan MediaStore.MediaColumns
            val sarr = contentColumnData(
                context, receivingIntent, MediaStore.MediaColumns.DATA,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.MediaColumns.DATA else null
            )
            if (result == null && sarr[0] != null) {
                result = checkPath(sarr[0])
            }
            if (result == null && sarr[1] != null) {
                result = checkPath("${Environment.getExternalStorageDirectory()}/${sarr[1]}")
            }
        }

        // Try build proxy by ContentResolver if no file found
        if (result == null && fileUri != null) {
            try {
                val sarr = contentColumnData(context, receivingIntent, OpenableColumns.DISPLAY_NAME)
                val filename = if (!sarr.isNullOrEmpty() && !TextUtils.isEmpty(sarr[0])) {
                    sarr[0]
                } else {
                    fileUri.lastPathSegment
                }

                // Proxy file to app-private storage
                val f = File(context.cacheDir, "$CONTENT_RESOLVER_FILE_PROXY_SEGMENT/$filename")
                f.parentFile?.mkdirs()
                val inputStream = context.contentResolver.openInputStream(fileUri)
                if (inputStream != null) {
                    val data = GsFileUtils.readCloseBinaryStream(inputStream)
                    GsFileUtils.writeFile(f, data, null)
                    f.setReadable(true)
                    f.setWritable(true)
                    result = checkPath(f.absolutePath)
                }
            } catch (ignored: Exception) {
            }
        }

        return result
    }

    /**
     * Extract column data from content resolver.
     *
     * @param context Android context
     * @param intent Intent to extract from
     * @param columns Columns to query
     * @return Array of extracted values
     */
    @JvmStatic
    fun contentColumnData(context: Context, intent: Intent, vararg columns: String?): Array<String?> {
        val uri = getUriFromIntent(intent, context) ?: return Array(columns.size) { null }
        val out = Array<String?>(columns.size) { null }
        val INVALID = -1

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, columns.filterNotNull().toTypedArray(), null, null, null)
        } catch (ignored: Exception) {
        }

        if (cursor != null && cursor.moveToFirst()) {
            for (i in columns.indices) {
                val coli = if (TextUtils.isEmpty(columns[i])) INVALID else cursor.getColumnIndex(columns[i])
                out[i] = if (coli == INVALID) null else cursor.getString(coli)
            }
        }
        cursor?.close()
        return out
    }

    //########################
    //## Path Callbacks
    //########################

    private fun setPathCallback(callback: GsCallback.a1<String>) {
        _receivePathCallback = WeakReference(callback)
    }

    private fun sendPathCallback(path: String?) {
        if (!GsTextUtils.isNullOrEmpty(path) && _receivePathCallback != null) {
            val cb = _receivePathCallback?.get()
            cb?.callback(path!!)
        }
        _receivePathCallback = null
    }

    //########################
    //## Activity Result Extraction
    //########################

    /**
     * Extract result data from Activity.onActivityResult.
     * Forward all arguments from context. Only requestCodes as implemented are analyzed.
     *
     * @param context Activity context
     * @param requestCode Request code from onActivityResult
     * @param resultCode Result code from onActivityResult
     * @param intent Intent data from onActivityResult
     */
    @JvmStatic
    @SuppressLint("ApplySharedPref")
    fun extractResultFromActivityResult(context: Activity, requestCode: Int, resultCode: Int, intent: Intent?) {
        when (requestCode) {
            REQUEST_CAMERA_PICTURE -> {
                sendPathCallback(if (resultCode == Activity.RESULT_OK) _lastCameraPictureFilepath else null)
            }

            REQUEST_PICK_PICTURE -> {
                if (resultCode == Activity.RESULT_OK && intent != null) {
                    val selectedImage = intent.data
                    val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
                    var picturePath: String? = null

                    if (selectedImage != null) {
                        var cursor: Cursor? = null
                        try {
                            cursor = context.contentResolver.query(selectedImage, filePathColumn, null, null, null)
                            if (cursor != null && cursor.moveToFirst()) {
                                for (column in filePathColumn) {
                                    val curColIndex = cursor.getColumnIndex(column)
                                    if (curColIndex != -1) {
                                        picturePath = cursor.getString(curColIndex)
                                        if (!TextUtils.isEmpty(picturePath)) {
                                            break
                                        }
                                    }
                                }
                            }
                        } finally {
                            cursor?.close()
                        }

                        // Try to grab via file extraction method
                        intent.action = Intent.ACTION_VIEW
                        picturePath = picturePath ?: GsFileUtils.getPath(extractFileFromIntent(intent, context))

                        // Retrieve image from file descriptor / Cloud
                        if (picturePath == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            try {
                                val parcelFileDescriptor = context.contentResolver.openFileDescriptor(selectedImage, "r")
                                if (parcelFileDescriptor != null) {
                                    val fileDescriptor = parcelFileDescriptor.fileDescriptor
                                    val input = FileInputStream(fileDescriptor)

                                    val temp = File.createTempFile("image", "tmp", context.cacheDir)
                                    temp.deleteOnExit()
                                    picturePath = temp.absolutePath

                                    GsFileUtils.writeFile(File(picturePath), GsFileUtils.readCloseBinaryStream(input), null)
                                }
                            } catch (ignored: IOException) {
                            }
                        }
                    }

                    sendPathCallback(picturePath)
                }
            }

            REQUEST_RECORD_AUDIO -> {
                if (resultCode == Activity.RESULT_OK && intent?.data != null) {
                    val uri = intent.data!!
                    val uriPath = uri.path
                    val ext = if (uriPath == null || !uriPath.contains(".")) "" else uriPath.substring(uriPath.lastIndexOf("."))
                    val datestr = SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss", Locale.ENGLISH).format(Date())
                    val temp = File(context.cacheDir, "$datestr$ext")
                    GsFileUtils.copyUriToFile(context, uri, temp)
                    sendPathCallback(temp.absolutePath)
                }
            }

            REQUEST_SAF -> {
                if (resultCode == Activity.RESULT_OK && intent?.data != null) {
                    val treeUri = intent.data!!
                    PreferenceManager.getDefaultSharedPreferences(context)
                        .edit()
                        .putString(PREF_KEY__SAF_TREE_URI, treeUri.toString())
                        .commit()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        val resolver = context.contentResolver
                        try {
                            resolver.takePersistableUriPermission(
                                treeUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            )
                        } catch (se: SecurityException) {
                            resolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    }
                }
            }

            REQUEST_STORAGE_PERMISSION_M,
            REQUEST_STORAGE_PERMISSION_R -> {
                checkExternalStoragePermission(context)
            }
        }
    }

    //########################
    //## Request File Edit
    //########################

    /**
     * Request edit of file in external app.
     *
     * @param context Context to use
     * @param file File that should be edited
     */
    @JvmStatic
    fun requestFileEdit(context: Context, file: File?) {
        if (file == null || !file.exists()) {
            return
        }

        try {
            val canonicalFile = file.canonicalFile
            val uri = FileProvider.getUriForFile(context, getFileProvider(context), canonicalFile)
            val intent = Intent(Intent.ACTION_EDIT)
            intent.setDataAndType(uri, GsFileUtils.getMimeType(canonicalFile))
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            intent.putExtra(GsResourceUtils.EXTRA_FILEPATH, canonicalFile.path)
            startActivity(context, intent)
        } catch (e: IOException) {
            android.util.Log.e("GsStorageUtils", "ERROR: Failed to get canonical file path")
        }
    }

    //########################
    //## Media URI
    //########################

    /**
     * Get content://media/ Uri for given file, or null if not indexed.
     *
     * @param context Android context
     * @param file Target file
     * @param mode 1 for picture, 2 for video, anything else for other
     * @return Media URI or null
     */
    @JvmStatic
    fun getMediaUri(context: Context, file: File, mode: Int): Uri? {
        var uri = MediaStore.Files.getContentUri("external")
        uri = when (mode) {
            1 -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            2 -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else -> uri
        }

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Images.Media._ID),
                "${MediaStore.Images.Media.DATA} = ?",
                arrayOf(file.absolutePath),
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                @SuppressLint("Range")
                val mediaid = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media._ID))
                return Uri.withAppendedPath(uri, mediaid.toString())
            }
        } catch (ignored: Exception) {
        } finally {
            cursor?.close()
        }
        return null
    }

    //########################
    //## Storage Access Framework (SAF)
    //########################

    /**
     * Request storage access. The user needs to press "Select storage" at the correct storage.
     *
     * @param context The Activity which will receive the result from startActivityForResult
     */
    @JvmStatic
    fun requestStorageAccessFramework(context: Activity?) {
        if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            )
            context.startActivityForResult(intent, REQUEST_SAF)
        }
    }

    /**
     * Get storage access framework tree uri.
     * The user must have granted access via [requestStorageAccessFramework].
     *
     * @param context Android context
     * @return Uri or null if not granted yet
     */
    @JvmStatic
    fun getStorageAccessFrameworkTreeUri(context: Context): Uri? {
        val treeStr = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(PREF_KEY__SAF_TREE_URI, null)
        if (!TextUtils.isEmpty(treeStr)) {
            try {
                return Uri.parse(treeStr)
            } catch (ignored: Exception) {
            }
        }
        return null
    }

    /**
     * Get mounted storage folder root (by tree uri).
     * The user must have granted access via [requestStorageAccessFramework].
     *
     * @param context Android context
     * @return File or null if SD not mounted
     */
    @JvmStatic
    fun getStorageAccessFolder(context: Context): File? {
        val safUri = getStorageAccessFrameworkTreeUri(context) ?: return null
        val safUriStr = safUri.toString()

        for (storage in getStorages(context, false, true)) {
            val storageFolderName = storage.first.name
            if (safUriStr.contains(storageFolderName)) {
                return storage.first
            }
        }
        return null
    }

    /**
     * Check whether or not a file is under a storage access folder (external storage / SD).
     *
     * @param context Android context
     * @param file The file object (file/folder)
     * @param isDir Whether the file is a directory
     * @return Whether or not the file is under storage access folder
     */
    @JvmStatic
    fun isUnderStorageAccessFolder(context: Context, file: File?, isDir: Boolean): Boolean {
        if (file == null) return false

        val actuallyIsDir = isDir || (file.exists() && file.isDirectory)

        // When file writable as is, it's the fastest way to learn SAF isn't required
        if (canWriteFile(context, file, actuallyIsDir, false)) {
            return false
        }

        for (storage in getStorages(context, false, true)) {
            if (file.absolutePath.startsWith(storage.first.absolutePath)) {
                return true
            }
        }
        return false
    }

    /**
     * Check if file is a content resolver proxy file.
     *
     * @param file File to check
     * @return True if content resolver proxy file
     */
    @JvmStatic
    fun isContentResolverProxyFile(file: File?): Boolean {
        return file?.parentFile?.name == CONTENT_RESOLVER_FILE_PROXY_SEGMENT
    }

    //########################
    //## File Write Permissions
    //########################

    /**
     * Check whether or not a file can be written.
     * Requires storage access framework permission for external storage (SD).
     *
     * @param context Android context
     * @param file The file object (file/folder)
     * @param isDir Whether or not the given file parameter is a directory
     * @param trySaf Whether to try SAF if direct write fails
     * @return Whether or not the file can be written
     */
    @JvmStatic
    fun canWriteFile(context: Context, file: File?, isDir: Boolean, trySaf: Boolean): Boolean {
        if (file == null) return false

        // Try direct file access
        if (GsFileUtils.canCreate(file)) {
            return true
        }

        // Own AppData directories do not require any special permission or handling
        if (getCacheDirs(context).any { GsFileUtils.isChild(it, file) }) {
            return true
        }

        if (trySaf) {
            val dof = getDocumentFile(context, file, isDir)
            return dof != null && dof.canWrite()
        }

        return false
    }

    /**
     * Get a [DocumentFile] object out of a normal java [File].
     * When used on external storage (SD), use [requestStorageAccessFramework]
     * first to get access. Otherwise this will fail.
     *
     * @param context Android context
     * @param file The file/folder to convert
     * @param isDir Whether or not file is a directory
     * @return A [DocumentFile] object or null if file cannot be converted
     */
    @JvmStatic
    fun getDocumentFile(context: Context, file: File, isDir: Boolean): DocumentFile? {
        // On older versions use fromFile
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            return DocumentFile.fromFile(file)
        }

        // Find storage root folder
        val baseFolderFile = getStorageRootFolder(context, file)
        val baseFolder = baseFolderFile?.absolutePath ?: return null

        var originalDirectory = false
        val relPath = try {
            val fullPath = file.canonicalPath
            if (baseFolder != fullPath) {
                fullPath.substring(baseFolder.length + 1)
            } else {
                originalDirectory = true
                null
            }
        } catch (e: IOException) {
            return null
        } catch (ignored: Exception) {
            originalDirectory = true
            null
        }

        val treeUri = getStorageAccessFrameworkTreeUri(context) ?: return null
        var dof = DocumentFile.fromTreeUri(context, treeUri) ?: return null

        if (originalDirectory) {
            return dof
        }

        val parts = relPath?.split("/") ?: return dof
        for (i in parts.indices) {
            var nextDof = dof.findFile(parts[i])
            if (nextDof == null) {
                try {
                    nextDof = if (i < parts.size - 1 || isDir) {
                        dof.createDirectory(parts[i])
                    } else {
                        dof.createFile("image", parts[i])
                    }
                } catch (ignored: Exception) {
                }
            }
            dof = nextDof ?: return null
        }
        return dof
    }

    /**
     * Write to a file with SAF support.
     *
     * @param context Activity context
     * @param file File to write to
     * @param isDirectory Whether the file is a directory
     * @param writeFileCallback Callback to perform the actual write operation
     */
    @JvmStatic
    fun writeFile(
        context: Activity?,
        file: File,
        isDirectory: Boolean,
        writeFileCallback: GsCallback.a2<Boolean, OutputStream?>?
    ) {
        try {
            var fileOutputStream: OutputStream? = null
            var pfd: ParcelFileDescriptor? = null

            val existingEmptyFile = file.canWrite() && file.length() < TEXTFILE_OVERWRITE_MIN_TEXT_LENGTH
            val nonExistingCreatableFile = !file.exists() && file.parentFile?.canWrite() == true

            when {
                isContentResolverProxyFile(file) && context != null -> {
                    try {
                        val intent = context.intent
                        val uri = ShareCompat.IntentReader(context, intent).stream ?: intent.data
                        fileOutputStream = uri?.let { context.contentResolver.openOutputStream(it, "rwt") }
                    } catch (ignored: Exception) {
                    }
                }
                existingEmptyFile || nonExistingCreatableFile -> {
                    if (isDirectory) {
                        file.mkdirs()
                    } else {
                        fileOutputStream = FileOutputStream(file)
                    }
                }
                context != null -> {
                    val dof = getDocumentFile(context, file, isDirectory)
                    if (dof != null && dof.canWrite()) {
                        if (!isDirectory) {
                            pfd = context.contentResolver.openFileDescriptor(dof.uri, "rwt")
                            fileOutputStream = pfd?.fileDescriptor?.let { FileOutputStream(it) }
                        }
                    }
                }
            }

            writeFileCallback?.callback(
                fileOutputStream != null || (isDirectory && file.exists()),
                fileOutputStream
            )

            fileOutputStream?.apply {
                try {
                    flush()
                    close()
                } catch (ignored: Exception) {
                }
            }

            pfd?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //########################
    //## Storage Permissions
    //########################

    /**
     * Request external storage permission.
     *
     * @param activity Activity to request from
     */
    @JvmStatic
    fun requestExternalStoragePermission(activity: Activity) {
        val v = Build.VERSION.SDK_INT

        if (v >= Build.VERSION_CODES.R) {
            try {
                val uri = Uri.parse("package:${GsResourceUtils.getAppIdFlavorSpecific(activity)}")
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
                activity.startActivityForResult(intent, REQUEST_STORAGE_PERMISSION_R)
            } catch (ex: Exception) {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                activity.startActivityForResult(intent, REQUEST_STORAGE_PERMISSION_R)
            }
        }

        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            REQUEST_STORAGE_PERMISSION_M
        )
    }

    /**
     * Request external storage permission with description from resource.
     *
     * @param activity Activity to request from
     * @param description String resource id for description
     */
    @JvmStatic
    fun requestExternalStoragePermission(activity: Activity, @StringRes description: Int) {
        requestExternalStoragePermission(activity, activity.getString(description))
    }

    /**
     * Request external storage permission with description.
     *
     * @param activity Activity to request from
     * @param description Description text
     */
    @JvmStatic
    fun requestExternalStoragePermission(activity: Activity, description: String) {
        val d = AlertDialog.Builder(activity)
            .setMessage(description)
            .setCancelable(false)
            .setPositiveButton(android.R.string.yes) { _, _ ->
                requestExternalStoragePermission(activity)
            }
            .setNegativeButton(android.R.string.no, null)
            .show()
        d.setCanceledOnTouchOutside(false)
    }

    /**
     * Check if external storage permission is granted.
     *
     * @param context Android context
     * @return True if permission granted
     */
    @JvmStatic
    fun checkExternalStoragePermission(context: Context): Boolean {
        val v = Build.VERSION.SDK_INT

        // Android R Manage-All-Files permission
        if (v >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager()
        }

        // Android M permissions
        if (v >= Build.VERSION_CODES.M && v < Build.VERSION_CODES.R) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

        // In case unsure, check if anything is writable at external storage
        val externalDir = Environment.getExternalStorageDirectory()
        val files = externalDir?.listFiles() ?: emptyArray()
        for (f in files) {
            if (f.canWrite()) {
                return true
            }
        }

        return false
    }

    //########################
    //## File Metadata Extraction
    //########################

    /**
     * Extract file metadata (multimedia files).
     *
     * @param context Android context
     * @param file File to extract metadata from
     * @param withHtml Include HTML formatted data (images)
     * @return List of (Key, Value) pairs
     */
    @JvmStatic
    fun extractFileMetadata(context: Context, file: File, withHtml: Boolean): List<Pair<String, String>> {
        if (m_cacheLastExtractFileMetadata?.first == file) {
            return m_cacheLastExtractFileMetadata!!.second
        }

        val fileUri = Uri.fromFile(file.absoluteFile)
        val extracted = ArrayList<Pair<String, String>>()

        val append: GsCallback.a2<String, String> = GsCallback.a2 { key, value ->
            val resId = GsResourceUtils.getResId(context, GsResourceUtils.ResType.STRING, key)
            extracted.add(Pair(if (resId != 0) context.getString(resId) else key, value))
        }

        // java.io.File metadata
        append.callback("File", file.absolutePath)
        append.callback("Size", GsFileUtils.getReadableFileSize(file.length(), true))
        append.callback(
            "Last modified",
            DateUtils.formatDateTime(
                context, file.lastModified(),
                DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE or
                DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_NUMERIC_DATE
            )
        )

        // Detect metadata keys from MediaMetadataRetriever
        val mmrfields = mutableListOf<Pair<Int, String>>()
        for (field in MediaMetadataRetriever::class.java.declaredFields) {
            val prefix = "METADATA_KEY_"
            val name = field.name
            if (name.startsWith(prefix)) {
                val displayName = GsTextUtils.toTitleCase(
                    name.replace(prefix, "")
                        .replace("_", " ")
                        .replace(Regex("\\s*(?i)num(ber)?\\s*"), " No. ")
                )
                try {
                    mmrfields.add(Pair(field.getInt(null), displayName))
                } catch (ignored: Exception) {
                }
            }
        }
        mmrfields.sortBy { it.first }

        // Extractor for multimedia file metadata
        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(context, fileUri)
        } catch (ignored: Exception) {
        }

        // Extract Cover & preview if available
        if (withHtml) {
            try {
                val data = mmr.embeddedPicture
                if (data != null) {
                    val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                    append.callback(
                        "Cover",
                        "<img src='data:image/jpeg;base64,${imageToBase64(bitmap, Bitmap.CompressFormat.JPEG, 50)}' style='max-height: 85vh; max-width: 100%;' />"
                    )
                }

                val frameBitmap = mmr.frameAtTime
                if (frameBitmap != null) {
                    append.callback(
                        "Preview",
                        "<img src='data:image/jpeg;base64,${imageToBase64(frameBitmap, Bitmap.CompressFormat.JPEG, 50)}' style='max-height: 85vh; max-width: 100%;' />"
                    )
                }
            } catch (ignored: Exception) {
            }
        }

        // Extract all other detected fields
        for (mmrfield in mmrfields) {
            var v: String? = null
            try {
                v = mmr.extractMetadata(mmrfield.first)
            } catch (ignored: Exception) {
            }

            if (!TextUtils.isEmpty(v)) {
                when (mmrfield.first) {
                    MediaMetadataRetriever.METADATA_KEY_BITRATE -> {
                        v = "${GsFileUtils.getHumanReadableByteCountSI(v!!.toLong())}ps"
                        if (v.startsWith("-1 ")) continue
                    }
                    MediaMetadataRetriever.METADATA_KEY_DURATION -> {
                        val hms = GsFileUtils.getTimeDiffHMS(v!!.toLong(), 0)
                        v = String.format("%sh %sm %ss", hms[0], hms[1], hms[2])
                        if (v == "0h 0m 0s") continue
                    }
                }
                append.callback(mmrfield.second, v!!)
            }
        }

        // Free resources
        try {
            mmr.release()
        } catch (ignored: Exception) {
        }

        m_cacheLastExtractFileMetadata = Pair(file, extracted)
        return extracted
    }

    private fun imageToBase64(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int): String {
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(format, quality, outputStream)
        return android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.DEFAULT)
    }
}
