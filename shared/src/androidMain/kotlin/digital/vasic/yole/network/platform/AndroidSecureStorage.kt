package digital.vasic.yole.network.platform

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Android implementation of secure storage using EncryptedSharedPreferences.
 * Provides secure storage of sensitive data like passwords, tokens, and keys.
 */
actual class AndroidSecureStorage actual constructor(
    private val context: Context
) : SecureStorage {
    
    private val sharedPreferences: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    override suspend fun store(key: String, value: String): Result<Unit> {
        return try {
            sharedPreferences.edit()
                .putString(key, value)
                .apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun retrieve(key: String): Result<String?> {
        return try {
            val value = sharedPreferences.getString(key, null)
            Result.success(value)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun delete(key: String): Result<Unit> {
        return try {
            sharedPreferences.edit()
                .remove(key)
                .apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun contains(key: String): Result<Boolean> {
        return try {
            val contains = sharedPreferences.contains(key)
            Result.success(contains)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun listKeys(): Result<List<String>> {
        return try {
            val keys = sharedPreferences.all.keys.toList()
            Result.success(keys)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun clear(): Result<Unit> {
        return try {
            sharedPreferences.edit()
                .clear()
                .apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun isSecure(): Result<Boolean> {
        return try {
            // For Android, we can check if the EncryptedSharedPreferences is properly initialized
            // by attempting to store and retrieve a test value
            val testKey = "_secure_storage_test_"
            val testValue = "test_value_${System.currentTimeMillis()}"
            
            store(testKey, testValue)
            val retrieved = retrieve(testKey).getOrNull()
            delete(testKey)
            
            Result.success(retrieved == testValue)
        } catch (e: Exception) {
            Result.success(false) // If anything fails, assume it's not secure
        }
    }
    
    companion object {
        private const val PREFS_NAME = "network_secure_storage"
    }
}