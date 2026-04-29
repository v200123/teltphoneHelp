package com.example.myservicecenter

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CachedCallRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CallRecordCacheDatabase : RoomDatabase() {

    abstract fun callRecordCacheDao(): CallRecordCacheDao

    companion object {
        @Volatile
        private var instance: CallRecordCacheDatabase? = null

        fun getInstance(context: Context): CallRecordCacheDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CallRecordCacheDatabase::class.java,
                    "call_record_cache.db"
                ).build().also { database ->
                    instance = database
                }
            }
        }
    }
}
