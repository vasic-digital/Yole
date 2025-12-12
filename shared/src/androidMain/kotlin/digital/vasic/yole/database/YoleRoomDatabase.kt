/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Room database class for Android implementation
 *
 *########################################################*/

package digital.vasic.yole.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database class for Yole Android implementation
 */
@Database(
    entities = [
        StorageEntity::class,
        DocumentEntity::class,
        CacheEntryEntity::class,
        OperationEntity::class,
        SyncStatusEntity::class,
        SettingEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class YoleRoomDatabase : RoomDatabase() {
    
    abstract fun storageDao(): StorageDao
    abstract fun documentDao(): DocumentDao
    abstract fun cacheEntryDao(): CacheEntryDao
    abstract fun operationDao(): OperationDao
    abstract fun syncStatusDao(): SyncStatusDao
    abstract fun settingDao(): SettingDao
}