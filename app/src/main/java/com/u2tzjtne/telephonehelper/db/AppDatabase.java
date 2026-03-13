package com.u2tzjtne.telephonehelper.db;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.u2tzjtne.telephonehelper.base.App;

/**
 * 应用数据库
 * 
 * 版本历史：
 * - v1: 初始版本，CallRecord 表
 * - v2: 未知变更
 * - v3: 添加录音字段到 CallRecord (recordingPath, recordingStartTime, recordingEndTime)
 * - v4: 添加独立的 Recording 表，支持一次通话多条录音
 *
 * @author u2tzjtne
 */
@Database(entities = {CallRecord.class, Recording.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase INSTANCE;
    private static final String DB_NAME = "app.db";

    /**
     * 通话记录数据访问
     */
    public abstract CallRecordDao callRecordModel();

    /**
     * 录音记录数据访问
     */
    public abstract RecordingDao recordingModel();

    public static synchronized AppDatabase getInstance() {
        if (INSTANCE == null) {
            INSTANCE = create();
        }
        return INSTANCE;
    }

    /**
     * 从版本 1 迁移到版本 2
     */
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // 版本1到版本2的迁移逻辑（如果之前有）
        }
    };

    /**
     * 从版本 2 迁移到版本 3
     * 添加录音相关字段到 CallRecord 表
     */
    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // 添加录音文件路径字段
            database.execSQL("ALTER TABLE CallRecord ADD COLUMN recordingPath TEXT");
            // 添加录音开始时间字段
            database.execSQL("ALTER TABLE CallRecord ADD COLUMN recordingStartTime INTEGER NOT NULL DEFAULT 0");
            // 添加录音结束时间字段
            database.execSQL("ALTER TABLE CallRecord ADD COLUMN recordingEndTime INTEGER NOT NULL DEFAULT 0");
        }
    };

    /**
     * 从版本 3 迁移到版本 4
     * 添加独立的 Recording 表，支持一次通话多条录音
     */
    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // 创建 Recording 表
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS Recording (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "callRecordId INTEGER NOT NULL, " +
                "filePath TEXT, " +
                "startTime INTEGER NOT NULL DEFAULT 0, " +
                "endTime INTEGER NOT NULL DEFAULT 0, " +
                "duration INTEGER NOT NULL DEFAULT 0, " +
                "fileSize INTEGER NOT NULL DEFAULT 0, " +
                "format TEXT, " +
                "createdAt INTEGER NOT NULL DEFAULT 0, " +
                "FOREIGN KEY(callRecordId) REFERENCES CallRecord(id) ON DELETE CASCADE)"
            );
            // 创建索引
            database.execSQL("CREATE INDEX IF NOT EXISTS index_Recording_id ON Recording(id)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_Recording_callRecordId ON Recording(callRecordId)");
        }
    };

    private static AppDatabase create() {
        return Room.databaseBuilder(
                App.getContext(),
                AppDatabase.class,
                DB_NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build();
    }
}
