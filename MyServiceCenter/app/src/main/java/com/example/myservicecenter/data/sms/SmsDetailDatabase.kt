package com.example.myservicecenter.data.sms

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SmsDetailRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SmsDetailDatabase : RoomDatabase() {

    abstract fun smsDetailDao(): SmsDetailDao

    companion object {
        @Volatile
        private var instance: SmsDetailDatabase? = null

        fun getInstance(context: Context): SmsDetailDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SmsDetailDatabase::class.java,
                    "sms_detail_records.db"
                ).build().also { database ->
                    instance = database
                }
            }
        }
    }
}
