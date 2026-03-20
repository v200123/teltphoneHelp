package com.u2tzjtne.telephonehelper.db;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.u2tzjtne.telephonehelper.base.App;

/**
 * 彩铃视频数据库
 */
@Database(
    entities = {RingVideo.class, RingtonePhoneBinding.class}, 
    version = 2, 
    exportSchema = false
)
public abstract class RingVideoDatabase extends RoomDatabase {

    private static volatile RingVideoDatabase INSTANCE;
    private static final String DB_NAME = "ring_video.db";

    private static final Callback NORMALIZE_PHONE_NUMBER_CALLBACK = new Callback() {
        @Override
        public void onOpen(androidx.sqlite.db.SupportSQLiteDatabase database) {
            super.onOpen(database);
            database.beginTransaction();
            try {
                database.execSQL("DELETE FROM RingtonePhoneBinding WHERE id NOT IN (SELECT MAX(id) FROM RingtonePhoneBinding GROUP BY COALESCE(REPLACE(phoneNumber, ' ', ''), ''))");
                database.execSQL("UPDATE RingtonePhoneBinding SET phoneNumber = REPLACE(phoneNumber, ' ', '') WHERE phoneNumber IS NOT NULL AND phoneNumber LIKE '% %'");
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }
        }
    };


    public abstract RingVideoDao ringVideoDao();
    
    public abstract RingtonePhoneBindingDao ringtonePhoneBindingDao();

    public static RingVideoDatabase getInstance() {
        if (INSTANCE == null) {
            synchronized (RingVideoDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            App.getContext(),
                            RingVideoDatabase.class,
                            DB_NAME)
                            .fallbackToDestructiveMigration() // 版本变更时重建数据库
                            .addCallback(NORMALIZE_PHONE_NUMBER_CALLBACK)
                            .build();

                }
            }
        }
        return INSTANCE;
    }
}