package com.u2tzjtne.telephonehelper.db;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.u2tzjtne.telephonehelper.base.App;

/**
 * 彩铃视频数据库
 */
@Database(entities = {RingVideo.class}, version = 1, exportSchema = false)
public abstract class RingVideoDatabase extends RoomDatabase {

    private static volatile RingVideoDatabase INSTANCE;
    private static final String DB_NAME = "ring_video.db";

    public abstract RingVideoDao ringVideoDao();

    public static RingVideoDatabase getInstance() {
        if (INSTANCE == null) {
            synchronized (RingVideoDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            App.getContext(),
                            RingVideoDatabase.class,
                            DB_NAME)
                        .build();
                }
            }
        }
        return INSTANCE;
    }
}
