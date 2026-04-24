package com.u2tzjtne.telephonehelper.db;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Room;

/**
 * 通话记录数据提供者
 * <p>
 * 允许其他应用访问通话记录数据
 * <p>
 * 使用方式：
 * content://com.u2tzjtne.telephonehelper.provider/callrecords - 查询所有通话记录
 * content://com.u2tzjtne.telephonehelper.provider/callrecords/# - 查询特定ID的通话记录
 *
 * @author u2tzjtne
 */
public class CallRecordProvider extends ContentProvider {

    /**
     * Provider 授权标识
     */
    public static final String AUTHORITY = "com.u2tzjtne.telephonehelper.provider";

    /**
     * 通话记录表URI
     */
    public static final Uri CALL_RECORD_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/callrecords");

    /**
     * 自定义归属地表URI
     */
    public static final Uri CUSTOM_LOCATION_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/locations");

    /**
     * 通话记录表名
     */
    private static final String CALL_RECORD_TABLE = "CallRecord";

    /**
     * 归属地表名
     */
    private static final String LOCATION_TABLE = "custom_phone_location";

    /**
     * URI匹配码 - 所有通话记录
     */
    private static final int CALL_RECORDS = 1;

    /**
     * URI匹配码 - 单条通话记录
     */
    private static final int CALL_RECORD_ID = 2;

    /**
     * URI匹配码 - 所有归属地
     */
    private static final int LOCATIONS = 3;

    /**
     * URI匹配码 - 单条归属地
     */
    private static final int LOCATION_ID = 4;

    /**
     * URI匹配器
     */
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(AUTHORITY, "callrecords", CALL_RECORDS);
        uriMatcher.addURI(AUTHORITY, "callrecords/#", CALL_RECORD_ID);
        uriMatcher.addURI(AUTHORITY, "locations", LOCATIONS);
        uriMatcher.addURI(AUTHORITY, "locations/#", LOCATION_ID);
    }

    private AppDatabase database;

    @Override
    public boolean onCreate() {
        Context context = getContext();
        if (context == null) {
            return false;
        }
        // 直接创建数据库，避免触发 Application 的重初始化
        database = Room.databaseBuilder(
                context.getApplicationContext(),
                AppDatabase.class,
                "app.db"
        ).build();
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        androidx.sqlite.db.SupportSQLiteQueryBuilder queryBuilder;
        String tableName;

        int match = uriMatcher.match(uri);
        switch (match) {
            case CALL_RECORDS:
                tableName = CALL_RECORD_TABLE;
                queryBuilder = androidx.sqlite.db.SupportSQLiteQueryBuilder.builder(tableName);
                break;
            case CALL_RECORD_ID:
                tableName = CALL_RECORD_TABLE;
                queryBuilder = androidx.sqlite.db.SupportSQLiteQueryBuilder.builder(tableName);
                queryBuilder.selection("id = ?", new String[]{String.valueOf(ContentUris.parseId(uri))});
                break;
            case LOCATIONS:
                tableName = LOCATION_TABLE;
                queryBuilder = androidx.sqlite.db.SupportSQLiteQueryBuilder.builder(tableName);
                break;
            case LOCATION_ID:
                tableName = LOCATION_TABLE;
                queryBuilder = androidx.sqlite.db.SupportSQLiteQueryBuilder.builder(tableName);
                queryBuilder.selection("id = ?", new String[]{String.valueOf(ContentUris.parseId(uri))});
                break;
            default:
                throw new IllegalArgumentException("未知的URI: " + uri);
        }

        if (projection != null) {
            queryBuilder.columns(projection);
        }
        if (selection != null) {
            queryBuilder.selection(selection, selectionArgs);
        }
        if (sortOrder != null) {
            queryBuilder.orderBy(sortOrder);
        }

        androidx.sqlite.db.SupportSQLiteDatabase db = database.getOpenHelper().getReadableDatabase();
        Cursor cursor = db.query(queryBuilder.create());
        
        Context context = getContext();
        if (context != null) {
            cursor.setNotificationUri(context.getContentResolver(), uri);
        }
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (uriMatcher.match(uri)) {
            case CALL_RECORDS:
                return "vnd.android.cursor.dir/vnd.telephonehelper.callrecord";
            case CALL_RECORD_ID:
                return "vnd.android.cursor.item/vnd.telephonehelper.callrecord";
            case LOCATIONS:
                return "vnd.android.cursor.dir/vnd.telephonehelper.location";
            case LOCATION_ID:
                return "vnd.android.cursor.item/vnd.telephonehelper.location";
            default:
                throw new IllegalArgumentException("未知的URI: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        if (values == null) {
            throw new IllegalArgumentException("ContentValues 不能为空");
        }

        String tableName;
        Uri contentUri;

        int match = uriMatcher.match(uri);
        switch (match) {
            case CALL_RECORDS:
                tableName = CALL_RECORD_TABLE;
                contentUri = CALL_RECORD_CONTENT_URI;
                break;
            case LOCATIONS:
                tableName = LOCATION_TABLE;
                contentUri = CUSTOM_LOCATION_CONTENT_URI;
                break;
            default:
                throw new IllegalArgumentException("不支持的插入URI: " + uri);
        }

        androidx.sqlite.db.SupportSQLiteDatabase db = database.getOpenHelper().getWritableDatabase();
        long id = db.insert(tableName, SQLiteDatabase.CONFLICT_REPLACE, values);

        if (id == -1) {
            throw new SQLiteException("插入数据失败: " + uri);
        }

        Context context = getContext();
        if (context != null) {
            context.getContentResolver().notifyChange(uri, null);
        }

        return ContentUris.withAppendedId(contentUri, id);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        String tableName;
        String finalSelection = selection;
        String[] finalSelectionArgs = selectionArgs;

        int match = uriMatcher.match(uri);
        switch (match) {
            case CALL_RECORDS:
                tableName = CALL_RECORD_TABLE;
                break;
            case CALL_RECORD_ID:
                tableName = CALL_RECORD_TABLE;
                finalSelection = "id = ?";
                finalSelectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                break;
            case LOCATIONS:
                tableName = LOCATION_TABLE;
                break;
            case LOCATION_ID:
                tableName = LOCATION_TABLE;
                finalSelection = "id = ?";
                finalSelectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                break;
            default:
                throw new IllegalArgumentException("未知的URI: " + uri);
        }

        androidx.sqlite.db.SupportSQLiteDatabase db = database.getOpenHelper().getWritableDatabase();
        int rowsDeleted = db.delete(tableName, finalSelection, finalSelectionArgs);

        if (rowsDeleted > 0) {
            Context context = getContext();
            if (context != null) {
                context.getContentResolver().notifyChange(uri, null);
            }
        }

        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        if (values == null) {
            throw new IllegalArgumentException("ContentValues 不能为空");
        }

        String tableName;
        String finalSelection = selection;
        String[] finalSelectionArgs = selectionArgs;

        int match = uriMatcher.match(uri);
        switch (match) {
            case CALL_RECORDS:
                tableName = CALL_RECORD_TABLE;
                break;
            case CALL_RECORD_ID:
                tableName = CALL_RECORD_TABLE;
                finalSelection = "id = ?";
                finalSelectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                break;
            case LOCATIONS:
                tableName = LOCATION_TABLE;
                break;
            case LOCATION_ID:
                tableName = LOCATION_TABLE;
                finalSelection = "id = ?";
                finalSelectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                break;
            default:
                throw new IllegalArgumentException("未知的URI: " + uri);
        }

        androidx.sqlite.db.SupportSQLiteDatabase db = database.getOpenHelper().getWritableDatabase();
        int rowsUpdated = db.update(tableName, SQLiteDatabase.CONFLICT_REPLACE, values, finalSelection, finalSelectionArgs);

        if (rowsUpdated > 0) {
            Context context = getContext();
            if (context != null) {
                context.getContentResolver().notifyChange(uri, null);
            }
        }

        return rowsUpdated;
    }
}
