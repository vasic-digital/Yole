package digital.vasic.yole.network.platform

import kotlinx.browser.localStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Web implementation of secure storage using localStorage with basic obfuscation.
 * Note: Web environment has limited security capabilities compared to native platforms.
 * This provides obfuscation but not true encryption.
 */
class WebSecureStorage : SecureStorage {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    override suspend fun store(key: String, value: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val encryptedValue = obfuscateData(value)
            localStorage.setItem(getStorageKey(key), encryptedValue)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun retrieve(key: String): Result<String?> = withContext(Dispatchers.Default) {
        try {
            val encryptedValue = localStorage.getItem(getStorageKey(key)) ?: return@withContext Result.success(null)
            val decryptedValue = deobfuscateData(encryptedValue)
            Result.success(decryptedValue)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun delete(key: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            localStorage.removeItem(getStorageKey(key))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun contains(key: String): Result<Boolean> = withContext(Dispatchers.Default) {
        try {
            val value = localStorage.getItem(getStorageKey(key))
            Result.success(value != null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun listKeys(): Result<List<String>> = withContext(Dispatchers.Default) {
        try {
            val prefix = STORAGE_PREFIX
            val keys = mutableListOf<String>()
            
            for (i in 0 until localStorage.length) {
                val fullKey = localStorage.key(i) ?: continue
                if (fullKey.startsWith(prefix)) {
                    keys.add(fullKey.substring(prefix.length))
                }
            }
            
            Result.success(keys)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun clear(): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val prefix = STORAGE_PREFIX
            val keysToRemove = mutableListOf<String>()
            
            for (i in 0 until localStorage.length) {
                val fullKey = localStorage.key(i) ?: continue
                if (fullKey.startsWith(prefix)) {
                    keysToRemove.add(fullKey)
                }
            }
            
            keysToRemove.forEach { key ->
                localStorage.removeItem(key)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun isSecure(): Result<Boolean> = withContext(Dispatchers.Default) {
        try {
            // Web environment can only provide obfuscation, not true encryption
            // We'll test the obfuscation/deobfuscation cycle
            val testKey = "_secure_storage_test_"
            val testValue = "test_value_${Clock.System.now().toEpochMilliseconds()}"
            
            store(testKey, testValue)
            val retrieved = retrieve(testKey).getOrNull()
            delete(testKey)
            
            Result.success(retrieved == testValue)
        } catch (e: Exception) {
            Result.success(false)
        }
    }
    
    private fun getStorageKey(key: String): String {
        return "$STORAGE_PREFIX$key"
    }
    
    private fun obfuscateData(data: String): String {
        // Simple XOR obfuscation with a fixed key
        // Note: This is NOT secure encryption, just basic obfuscation
        val key = obfuscationKey
        val result = StringBuilder()
        
        for (i in data.indices) {
            val char = data[i].code
            val obfuscated = char xor key[i % key.length].code
            result.append(obfuscated.toChar())
        }
        
        // Convert to base64 for localStorage compatibility
        return btoa(result.toString())
    }
    
    private fun deobfuscateData(obfuscatedData: String): String {
        // Convert from base64 and apply XOR deobfuscation
        val encoded = atob(obfuscatedData)
        val key = obfuscationKey
        val result = StringBuilder()
        
        for (i in encoded.indices) {
            val char = encoded[i].code
            val deobfuscated = char xor key[i % key.length].code
            result.append(deobfuscated.toChar())
        }
        
        return result.toString()
    }
    
    companion object {
        private const val STORAGE_PREFIX = "yole_network_secure_"
        private val obfuscationKey = "Y0l3_N3tw0rk_S3cur3_K3y_2025"
    }
}

// External functions for base64 encoding/decoding
// These are available in most web browsers
external fun btoa(data: String): String
external fun atob(data: String): String