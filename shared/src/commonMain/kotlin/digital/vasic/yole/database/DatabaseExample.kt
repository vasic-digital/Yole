/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Example usage of the unified database system
 *
 *########################################################*/

package digital.vasic.yole.database

import kotlinx.coroutines.*
import digital.vasic.yole.network.common.*
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.hours

/**
 * Example usage of the unified database system
 */
object DatabaseExample {
    
    /**
     * Example of basic database usage
     */
    suspend fun basicUsageExample() {
        // Create database instance (platform-specific)
        val database = DatabaseFactory.createDatabase("yole_example")
        
        try {
            // Initialize database
            database.initialize().getOrThrow()
            println("Database initialized successfully")
            
            // Create a storage configuration
            val storage = NetworkStorage(
                id = "webdav_storage_1",
                name = "My WebDAV Storage",
                type = StorageType.WEBDAV,
                location = "https://example.com/webdav",
                totalSpace = 10_000_000_000L, // 10GB
                usedSpace = 2_000_000_000L,  // 2GB
                isOnline = true,
                lastSync = Clock.System.now(),
                metadata = mapOf("username" to "user123"),
                isEnabled = true,
                priority = 100,
                supportsFolders = true,
                supportsMetadata = true,
                maxFileSize = 100_000_000L, // 100MB
                supportedExtensions = listOf("txt", "md", "pdf", "jpg", "png")
            )
            
            // Insert storage
            database.insertStorage(storage).getOrThrow()
            println("Storage inserted: ${storage.name}")
            
            // Create a document
            val document = NetworkDocument(
                id = "doc_123",
                name = "example.txt",
                path = "/documents/example.txt",
                isFolder = false,
                size = 1024L,
                lastModified = Clock.System.now(),
                syncStatus = SyncStatus.SYNCED,
                contentType = "text/plain",
                tags = listOf("important", "work"),
                metadata = mapOf("author" to "John Doe")
            )
            
            // Insert document
            database.insertDocument(document).getOrThrow()
            println("Document inserted: ${document.name}")
            
            // Create a cache entry
            val cacheEntry = CacheEntry.create(
                remoteDocumentId = document.id,
                localPath = "/cache/doc_123.txt",
                remotePath = document.path,
                size = 1024L,
                contentType = "text/plain",
                ttl = 24.hours.inWholeMilliseconds,
                isPinned = false
            )
            
            // Insert cache entry
            database.insertCacheEntry(cacheEntry).getOrThrow()
            println("Cache entry created: ${cacheEntry.id}")
            
            // Create an operation
            val operation = NetworkOperation(
                id = 1L,
                type = OperationType.UPLOAD,
                remotePath = document.path,
                localPath = "/local/example.txt",
                status = OperationStatus.RUNNING,
                progress = 0.5,
                totalSize = 1024L,
                bytesTransferred = 512L,
                createdAt = Clock.System.now(),
                priority = 100
            )
            
            // Insert operation
            database.insertOperation(operation).getOrThrow()
            println("Operation created: ${operation.type} - ${operation.status}")
            
            // Query data
            val allStorage = database.getAllStorage().getOrThrow()
            println("Total storage configurations: ${allStorage.size}")
            
            val allDocuments = database.getDocumentsByStorage(storage.id).getOrThrow()
            println("Documents in storage: ${allDocuments.size}")
            
            val cacheUsage = database.getCacheUsage().getOrThrow()
            println("Cache usage: $cacheUsage bytes")
            
            val activeOperations = database.getActiveOperations().getOrThrow()
            println("Active operations: ${activeOperations.size}")
            
            // Search documents
            val searchResults = database.searchDocuments("example").getOrThrow()
            println("Search results for 'example': ${searchResults.size}")
            
            // Get database statistics
            val stats = database.getDatabaseStats().getOrThrow()
            println("Database stats: $stats")
            
        } catch (e: Exception) {
            println("Error: ${e.message}")
        } finally {
            // Close database
            database.close().getOrThrow()
            println("Database closed")
        }
    }
    
    /**
     * Example of using DatabaseService for high-level operations
     */
    suspend fun databaseServiceExample() {
        val database = DatabaseFactory.createDatabase("yole_service_example")
        val databaseService = DatabaseService(database)
        
        try {
            // Initialize
            databaseService.initialize().getOrThrow()
            println("Database service initialized")
            
            // Create storage with service
            val storage = NetworkStorage.mock("service_storage")
            databaseService.saveStorage(storage).getOrThrow()
            println("Storage saved via service")
            
            // Create document with service
            val document = NetworkDocument.mock("service_doc")
            databaseService.saveDocument(document).getOrThrow()
            println("Document saved via service")
            
            // Use high-level convenience methods
            val storageWithStats = databaseService.getStorageWithStats(storage.id).getOrThrow()
            println("Storage stats: ${storageWithStats.documentCount} documents, ${storageWithStats.totalSize} bytes")
            
            val documentWithCache = databaseService.getDocumentWithCacheInfo(document.id).getOrThrow()
            println("Document cache info: ${documentWithCache.cacheSize} bytes cached")
            
            val cacheStats = databaseService.getCacheStatistics().getOrThrow()
            println("Cache statistics: $cacheStats")
            
            // Global search
            val searchResults = databaseService.globalSearch("test").getOrThrow()
            println("Global search found ${searchResults.totalResults} results")
            
            // Cleanup database
            val cleanupResults = databaseService.cleanupDatabase().getOrThrow()
            println("Database cleanup: $cleanupResults")
            
        } catch (e: Exception) {
            println("Service error: ${e.message}")
        } finally {
            databaseService.close().getOrThrow()
            println("Database service closed")
        }
    }
    
    /**
     * Example of transaction usage
     */
    suspend fun transactionExample() {
        val database = DatabaseFactory.createDatabase("yole_transaction_example")
        
        try {
            database.initialize().getOrThrow()
            
            // Perform operations in a transaction
            val result = database.transaction {
                val storage = NetworkStorage.mock("transaction_storage")
                database.insertStorage(storage).getOrThrow()
                
                val document = NetworkDocument.mock("transaction_doc")
                database.insertDocument(document).getOrThrow()
                
                val cacheEntry = CacheEntry.create(
                    remoteDocumentId = document.id,
                    localPath = "/cache/transaction.txt",
                    remotePath = document.path,
                    size = 512L
                )
                database.insertCacheEntry(cacheEntry).getOrThrow()
                
                "Transaction completed successfully"
            }
            
            when (result) {
                is Result.Success -> {
                    println("Transaction success: ${result.value}")
                    
                    // Verify data was inserted
                    val storageCount = database.getTableRowCount("storage").getOrThrow()
                    val documentCount = database.getTableRowCount("documents").getOrThrow()
                    val cacheCount = database.getTableRowCount("cache_entries").getOrThrow()
                    
                    println("After transaction - Storage: $storageCount, Documents: $documentCount, Cache: $cacheCount")
                }
                is Result.Failure -> {
                    println("Transaction failed: ${result.exception}")
                }
            }
            
        } catch (e: Exception) {
            println("Transaction error: ${e.message}")
        } finally {
            database.close().getOrThrow()
        }
    }
    
    /**
     * Example of backup and restore
     */
    suspend fun backupRestoreExample() {
        val database = DatabaseFactory.createDatabase("yole_backup_example")
        
        try {
            database.initialize().getOrThrow()
            
            // Insert some test data
            val storage = NetworkStorage.mock("backup_storage")
            val document = NetworkDocument.mock("backup_doc")
            val cacheEntry = CacheEntry.create(
                remoteDocumentId = document.id,
                localPath = "/cache/backup.txt",
                remotePath = document.path,
                size = 2048L
            )
            val operation = NetworkOperation(
                id = 1L,
                type = OperationType.DOWNLOAD,
                remotePath = document.path,
                status = OperationStatus.COMPLETED,
                progress = 1.0,
                totalSize = 2048L,
                bytesTransferred = 2048L,
                createdAt = Clock.System.now()
            )
            
            database.insertStorage(storage).getOrThrow()
            database.insertDocument(document).getOrThrow()
            database.insertCacheEntry(cacheEntry).getOrThrow()
            database.insertOperation(operation).getOrThrow()
            database.setSetting("backup_setting", "backup_value").getOrThrow()
            database.updateSyncStatus(document.path, SyncStatus.SYNCED).getOrThrow()
            
            println("Test data inserted")
            
            // Export backup
            val backup = database.exportData().getOrThrow()
            println("Backup exported: ${backup.storage.size} storage, ${backup.documents.size} documents")
            
            // Clear database
            database.clearAll().getOrThrow()
            println("Database cleared")
            
            // Verify database is empty
            val emptyStats = database.getDatabaseStats().getOrThrow()
            println("Empty database stats: $emptyStats")
            
            // Restore from backup
            database.importData(backup).getOrThrow()
            println("Data restored from backup")
            
            // Verify data was restored
            val restoredStats = database.getDatabaseStats().getOrThrow()
            println("Restored database stats: $restoredStats")
            
            // Validate data integrity
            val validationErrors = database.validateData().getOrThrow()
            if (validationErrors.isEmpty()) {
                println("Data validation passed")
            } else {
                println("Data validation failed: $validationErrors")
            }
            
        } catch (e: Exception) {
            println("Backup/restore error: ${e.message}")
        } finally {
            database.close().getOrThrow()
        }
    }
    
    /**
     * Example of platform-specific configuration
     */
    fun platformConfigExample() {
        val config = DatabaseFactory.getPlatformConfig()
        
        println("Platform database configuration:")
        println("Platform: ${config.platform}")
        println("Supports transactions: ${config.supportsTransactions}")
        println("Supports foreign keys: ${config.supportsForeignKeys}")
        println("Max database size: ${config.maxDatabaseSize?.let { "${it / (1024 * 1024 * 1024)}GB" } ?: "Unlimited"}")
        println("Supports encryption: ${config.supportsEncryption}")
        println("Supports WAL: ${config.supportsWAL}")
        println("Recommended cache size: ${config.recommendedCacheSize} pages")
        println("Supports async: ${config.supportsAsync}")
    }
    
    /**
     * Example of error handling
     */
    suspend fun errorHandlingExample() {
        val database = DatabaseFactory.createDatabase("yole_error_example")
        
        try {
            database.initialize().getOrThrow()
            
            // Try to insert invalid data
            val invalidStorage = NetworkStorage(
                id = "", // Invalid: blank ID
                name = "Invalid Storage",
                type = StorageType.WEBDAV,
                location = "http://example.com"
            )
            
            val result = database.insertStorage(invalidStorage)
            
            when (result) {
                is Result.Success -> {
                    println("Unexpected success")
                }
                is Result.Failure -> {
                    println("Expected error: ${result.exception.message}")
                    
                    // Try to get more details about the error
                    when (result.exception) {
                        is IllegalArgumentException -> {
                            println("Validation error: ${result.exception.message}")
                        }
                        is IllegalStateException -> {
                            println("State error: ${result.exception.message}")
                        }
                        else -> {
                            println("Unknown error: ${result.exception.message}")
                        }
                    }
                }
            }
            
        } catch (e: Exception) {
            println("Unexpected exception: ${e.message}")
        } finally {
            database.close().getOrThrow()
        }
    }
    
    /**
     * Run all examples
     */
    suspend fun runAllExamples() {
        println("=== Database System Examples ===")
        
        println("\n1. Basic Usage Example:")
        basicUsageExample()
        
        println("\n2. Database Service Example:")
        databaseServiceExample()
        
        println("\n3. Transaction Example:")
        transactionExample()
        
        println("\n4. Backup and Restore Example:")
        backupRestoreExample()
        
        println("\n5. Platform Configuration Example:")
        platformConfigExample()
        
        println("\n6. Error Handling Example:")
        errorHandlingExample()
        
        println("\n=== All examples completed ===")
    }
}

/**
 * Main function to run examples (for testing)
 */
suspend fun main() {
    DatabaseExample.runAllExamples()
}