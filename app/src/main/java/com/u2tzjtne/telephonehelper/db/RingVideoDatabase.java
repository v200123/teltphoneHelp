package com.u2tzjtne.telephonehelper.db;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.u2tzjtne.telephonehelper.base.App;

/**
 * 彩铃视频数据库
 */
@Database(
    entities = {
        MusicFile.class,
        RingVideo.class, 
        RingtonePhoneBinding.class,
        NoRingtonePhone.class,
        PhoneRingtoneAssignment.class,
        CallPromptPhone.class,
        AutoHangUpRule.class
    }, 
    version = 7,
    exportSchema = false
)
public abstract class RingVideoDatabase extends RoomDatabase {

    private static volatile RingVideoDatabase INSTANCE;
    private static final String DB_NAME = "ring_video.db";
    private static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE RingVideo ADD COLUMN sourceVideoUri TEXT");
            database.execSQL("ALTER TABLE RingVideo ADD COLUMN playbackVideoUri TEXT");
            database.execSQL("ALTER TABLE RingVideo ADD COLUMN hasBakedBadge INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE RingVideo ADD COLUMN appliedBadgeRuleSnapshot TEXT");
            database.execSQL("ALTER TABLE RingVideo ADD COLUMN appliedBadgeRuleName TEXT");
            database.execSQL("UPDATE RingVideo SET sourceVideoUri = videoUri WHERE sourceVideoUri IS NULL");
            database.execSQL("UPDATE RingVideo SET playbackVideoUri = videoUri WHERE playbackVideoUri IS NULL");
        }
    };

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


    public abstract MusicFileDao musicFileDao();

    public abstract RingVideoDao ringVideoDao();
    
    public abstract RingtonePhoneBindingDao ringtonePhoneBindingDao();
    
    public abstract NoRingtonePhoneDao noRingtonePhoneDao();
    
    public abstract PhoneRingtoneAssignmentDao phoneRingtoneAssignmentDao();

    public abstract CallPromptPhoneDao callPromptPhoneDao();

    public abstract AutoHangUpRuleDao autoHangUpRuleDao();

    public static RingVideoDatabase getInstance() {
        if (INSTANCE == null) {
            synchronized (RingVideoDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            App.getContext(),
                            RingVideoDatabase.class,
                            DB_NAME)
                            .addMigrations(MIGRATION_6_7)
                            .fallbackToDestructiveMigration()
                            .addCallback(NORMALIZE_PHONE_NUMBER_CALLBACK)
                            .build();

                }
            }
        }
        return INSTANCE;
    }
}
