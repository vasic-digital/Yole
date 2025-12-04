package digital.vasic.yole.network.platform

import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.*
import java.security.spec.KeySpec
import javax.crypto.*
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Desktop implementation of secure storage using AES encryption with file-based storage.
 * Provides secure storage of sensitive data like passwords, tokens, and keys.
 */
actual class DesktopSecureStorage actual constructor(
    private val storageDir: File
) : SecureStorage {
    
    private val keyFile: File = File(storageDir, ".storage_key")
    private val dataFile: File = File(storageDir, ".secure_storage")
    
    override suspend fun store(key: String, value: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            ensureStorageDirectory()
            val secretKey = getOrCreateSecretKey()
            val encryptedData = encryptData(value, secretKey)
            val existingData = readEncryptedData().toMutableMap()
            existingData[key] = encryptedData
            writeEncryptedData(existingData)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun retrieve(key: String): Result<String?> = withContext(Dispatchers.IO) {
        try {
            val secretKey = getOrCreateSecretKey()
            val allData = readEncryptedData()
            val encryptedValue = allData[key] ?: return@withContext Result.success(null)
            val decryptedValue = decryptData(encryptedValue, secretKey)
            Result.success(decryptedValue)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun delete(key: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existingData = readEncryptedData().toMutableMap()
            existingData.remove(key)
            writeEncryptedData(existingData)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun contains(key: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val allData = readEncryptedData()
            Result.success(allData.containsKey(key))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun listKeys(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val allData = readEncryptedData()
            Result.success(allData.keys.toList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun clear(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (dataFile.exists()) {
                dataFile.delete()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun isSecure(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            ensureStorageDirectory()
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
    
    private fun ensureStorageDirectory() {
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
    }
    
    private fun getOrCreateSecretKey(): SecretKey {
        return if (keyFile.exists()) {
            readSecretKey()
        } else {
            val newKey = generateSecretKey()
            writeSecretKey(newKey)
            newKey
        }
    }
    
    private fun generateSecretKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        return keyGen.generateKey()
    }
    
    private fun readSecretKey(): SecretKey {
        val keyBytes = keyFile.readBytes()
        return SecretKeySpec(keyBytes, "AES")
    }
    
    private fun writeSecretKey(key: SecretKey) {
        keyFile.writeBytes(key.encoded)
        keyFile.setReadable(false, false) // Restrict file permissions
    }
    
    private fun readEncryptedData(): Map<String, String> {
        if (!dataFile.exists()) {
            return emptyMap()
        }
        
        try {
            val content = dataFile.readText(StandardCharsets.UTF_8)
            return if (content.isEmpty()) {
                emptyMap()
            } else {
                content.lines()
                    .filter { it.isNotEmpty() }
                    .associate { line ->
                        val parts = line.split("=", limit = 2)
                        parts[0] to parts.getOrNull(1) ?: ""
                    }
            }
        } catch (e: IOException) {
            return emptyMap()
        }
    }
    
    private fun writeEncryptedData(data: Map<String, String>) {
        val content = data.entries
            .joinToString("\n") { "${it.key}=${it.value}" }
        
        dataFile.writeText(content, StandardCharsets.UTF_8)
        dataFile.setReadable(false, false) // Restrict file permissions
    }
    
    private fun encryptData(data: String, key: SecretKey): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        
        // Combine IV and encrypted data for storage
        return (iv + encryptedData).joinToString(",") { it.toInt().toString() }
    }
    
    private fun decryptData(encryptedData: String, key: SecretKey): String {
        val bytes = encryptedData.split(",").map { it.toByte().toByte() }.toByteArray()
        val iv = bytes.sliceArray(0 until 12) // GCM uses 12-byte IV
        val cipherText = bytes.sliceArray(12 until bytes.size)
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, javax.crypto.spec.GCMParameterSpec(128, iv))
        val decryptedData = cipher.doFinal(cipherText)
        
        return String(decryptedData, StandardCharsets.UTF_8)
    }
}