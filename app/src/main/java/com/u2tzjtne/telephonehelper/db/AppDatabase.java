package com.u2tzjtne.telephonehelper.db;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.u2tzjtne.telephonehelper.base.App;

/**
 * @author u2tzjtne
 */
@Database(entities = {CallRecord.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase INSTANCE;
    private static final String DB_NAME = "app.db";

    /**
     * 通话记录实现
     */
    public abstract CallRecordDao callRecordModel();

    public static synchronized AppDatabase getInstance() {
        if (INSTANCE == null) {
            INSTANCE = create();
        }
        return INSTANCE;
    }

    private static AppDatabase create() {
        return Room.databaseBuilder(
                App.getContext(),
                AppDatabase.class,
                DB_NAME).build();
    }
}