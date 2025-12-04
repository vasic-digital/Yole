/*#######################################################
 *
 * SPDX-FileCopyrightText: 2022-2025 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2022-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 * Migrated to Kotlin 2025 by Milos Vasic
 *
 * To the extent possible under law, the author(s) have dedicated all copyright
 * and related and neighboring rights to this software to the public domain worldwide.
 * This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 #########################################################*/
package digital.vasic.opoc.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import digital.vasic.opoc.model.GsSharedPreferencesPropertyBackend
import digital.vasic.opoc.wrapper.GsHashMap
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.util.Date
import java.util.regex.Pattern

/**
 * Utility class for backup and restore of Android SharedPreferences.
 *
 * Provides functionality for:
 * - Backing up SharedPreferences to JSON files
 * - Restoring SharedPreferences from JSON files
 * - Filtering sensitive data (passwords)
 * - Including metadata in backups
 *
 * Migrated from Java to Kotlin as part of commons module modernization.
 * Depends on: GsResourceUtils, GsFileUtils, GsSharedPreferencesPropertyBackend, GsHashMap
 */
@Suppress("unused")
object GsBackupUtils {

    private const val LOG_PREFIX = "GsBackupUtils"
    private const val FIELD_BACKUP_METADATA = "__BACKUP_METADATA__"

    private val PREF_EXCLUDE_PATTERNS = arrayOf(
        Pattern.compile("^(?!PREF_PREFIX_).*password.*$", Pattern.MULTILINE or Pattern.CASE_INSENSITIVE),
        Pattern.compile(FIELD_BACKUP_METADATA, Pattern.MULTILINE)
    )

    /**
     * Get a list of preference (file)names to be included in backup.
     *
     * @return String list of preference names to be included in backup
     */
    @JvmStatic
    fun getPrefNamesToBackup(): List<String> {
        val prefNames = mutableListOf<String>()
        prefNames.add("") // Default pref (empty string, will be resolved to package name + "_preferences")
        prefNames.add(GsSharedPreferencesPropertyBackend.SHARED_PREF_APP)
        return prefNames
    }

    /**
     * Generate a timestamped filepath for the backup file.
     *
     * @param context Android Context
     * @param targetFolder Folder in which the File should be placed
     * @return The File that should be created
     */
    @JvmStatic
    fun generateBackupFilepath(context: Context, targetFolder: File): File {
        val appName = GsResourceUtils.rstr(context, "app_name_real", true)
            ?.lowercase()
            ?.replace(Regex("\\s"), "") ?: "app"
        return File(targetFolder, GsFileUtils.getFilenameWithTimestamp("BACKUP_$appName", "", ".json"))
    }

    /**
     * Get the actual preference name (resolves null/empty to default).
     *
     * @param context Android Context
     * @param raw Raw preference name (can be null or empty)
     * @return Resolved preference name
     */
    @JvmStatic
    fun getPrefName(context: Context, raw: String?): String {
        return if (!TextUtils.isEmpty(raw)) raw!! else "${context.packageName}_preferences"
    }

    /**
     * Determine if the key is allowed to be included in the backup.
     * Excludes patterns like passwords and metadata.
     *
     * @param key Check if this key is allowed
     * @return True if allowed (should backup)
     */
    @JvmStatic
    fun isPrefKeyAllowBackup(key: String): Boolean {
        for (ep in PREF_EXCLUDE_PATTERNS) {
            if (ep.matcher(key).matches()) {
                return false
            }
        }
        return true
    }

    /**
     * Make backup of Android SharedPreferences, combined into a single .json file.
     *
     * @param context Android context
     * @param prefNamesToBackup Names of the SharedPreferences to backup
     * @param targetJsonFile Target json file to write to, overwritten if already exists
     */
    @JvmStatic
    @Suppress("DEPRECATION")
    fun makeBackup(context: Context, prefNamesToBackup: List<String>, targetJsonFile: File) {
        try {
            val jsonRoot = JSONObject()

            // Collect metadata for backup file
            val now = Date()
            val jsonMetadata = JSONObject(
                GsHashMap<String, String>().load(
                    "BACKUP_DATE", "${now} ::: ${now.time}",
                    "APPLICATION_ID_MANIFEST", GsResourceUtils.getAppIdUsedAtManifest(context),
                    "EXPORT_ANDROID_DEVICE_VERSION", GsResourceUtils.getAndroidVersion(),
                    "ISOURCE", GsResourceUtils.getAppInstallationSource(context),
                    "BACKUP_REVISION", "1"
                ).data()
            )

            // Add BuildConfig fields to metadata
            for (field in GsResourceUtils.getBuildConfigFields(context)) {
                val v = GsResourceUtils.getBuildConfigValue(context, field)
                if (v != null && !v.javaClass.isArray) {
                    jsonMetadata.put(field, v)
                }
            }

            // Iterate preferences and their values
            for (rawPrefName in prefNamesToBackup) {
                val prefName = getPrefName(context, rawPrefName)
                val pref = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                val prefKeyValues = pref.all
                val jsonFromPref = JSONObject()

                for ((prefKey, prefValue) in prefKeyValues) {
                    if (isPrefKeyAllowBackup(prefKey)) {
                        when (prefValue) {
                            is Int, is Long, is Float, is String, is Boolean -> {
                                jsonFromPref.put(prefKey, prefValue)
                            }
                            is Set<*> -> {
                                val jsonArray = JSONArray()
                                @Suppress("UNCHECKED_CAST")
                                for (s in pref.getStringSet(prefKey, emptySet()) ?: emptySet()) {
                                    jsonArray.put(s)
                                }
                                jsonFromPref.put(prefKey, jsonArray)
                            }
                            else -> {
                                Log.w(LOG_PREFIX, "Unhandled backup type: ${prefValue?.javaClass?.name}")
                            }
                        }
                    }
                }

                if (jsonFromPref.length() > 0) {
                    jsonRoot.put(prefName, jsonFromPref)
                }
            }

            jsonRoot.put(FIELD_BACKUP_METADATA, jsonMetadata)

            // Write to file
            FileWriter(targetJsonFile).use { file ->
                file.write(jsonRoot.toString(2))
                file.flush()
            }

            Toast.makeText(context, "✔️ ${targetJsonFile.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Attempt to delete file if it exists
            if (targetJsonFile.exists()) {
                targetJsonFile.delete()
            }
            Log.e(LOG_PREFIX, "Backup failed: ${e.message}", e)
            Toast.makeText(
                context,
                GsResourceUtils.rstr(context, "failed_to_create_backup", true) ?: "Failed to create backup",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Load a backup-json file that should have been exported by the same app.
     * IMPORTANT: This method calls System.exit(0) after successful restore!
     *
     * @param context Android context
     * @param backupFileContainingJson An existing & accessible json file
     */
    @JvmStatic
    @SuppressLint("ApplySharedPref")
    fun loadBackup(context: Context, backupFileContainingJson: File) {
        try {
            val json = JSONObject(GsFileUtils.readTextFileFast(backupFileContainingJson).first)
            val editors = mutableListOf<SharedPreferences.Editor>()

            val keys = json.keys()
            while (keys.hasNext()) {
                val prefName = keys.next()
                val sp = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                val edit = sp.edit()
                val prefObj = json.get(prefName)

                if (prefObj is JSONObject) {
                    val prefJson = prefObj
                    val prefKeys = prefJson.keys()

                    while (prefKeys.hasNext()) {
                        val key = prefKeys.next()
                        if (isPrefKeyAllowBackup(key)) {
                            when (val value = prefJson.get(key)) {
                                is Int -> edit.putInt(key, value)
                                is Long -> edit.putLong(key, value)
                                is Float -> edit.putFloat(key, value)
                                is String -> edit.putString(key, value)
                                is Boolean -> edit.putBoolean(key, value)
                                is JSONArray -> {
                                    val ss = mutableSetOf<String>()
                                    for (i in 0 until value.length()) {
                                        ss.add(value.getString(i))
                                    }
                                    edit.putStringSet(key, ss)
                                }
                                else -> {
                                    Log.w(LOG_PREFIX, "Unhandled restore type: ${value?.javaClass?.name}")
                                }
                            }
                        }
                    }
                    editors.add(edit)
                }
            }

            // Commit all changes
            for (edit in editors) {
                edit.commit()
            }

            // Exit app to apply changes (this is the original behavior)
            System.exit(0)
        } catch (e: Exception) {
            Log.e(LOG_PREFIX, "Restore failed: ${e.message}", e)
            Toast.makeText(
                context,
                GsResourceUtils.rstr(context, "failed_to_restore_settings_from_backup", true)
                    ?: "Failed to restore settings from backup",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
