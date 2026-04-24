package com.u2tzjtne.telephonehelper.db;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * 通话记录数据契约类
 * <p>
 * 提供给其他应用使用的常量定义，方便外部应用访问本应用的数据
 * <p>
 * 使用示例：
 * <pre>
 * // 查询所有通话记录
 * Cursor cursor = getContentResolver().query(
 *     CallRecordContract.CallRecord.CONTENT_URI,
 *     null, null, null, null);
 *
 * // 查询特定号码的通话记录
 * Cursor cursor = getContentResolver().query(
 *     CallRecordContract.CallRecord.CONTENT_URI,
 *     null,
 *     CallRecordContract.CallRecord.COLUMN_PHONE_NUMBER + " = ?",
 *     new String[]{"13800138000"},
 *     CallRecordContract.CallRecord.COLUMN_START_TIME + " DESC");
 * </pre>
 *
 * @author u2tzjtne
 */
public class CallRecordContract {

    /**
     * Content Provider 授权标识
     */
    public static final String CONTENT_AUTHORITY = "com.u2tzjtne.telephonehelper.provider";

    /**
     * 基础 URI
     */
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    /**
     * 读取权限
     */
    public static final String PERMISSION_READ = 
            "com.u2tzjtne.telephonehelper.permission.READ_CALL_RECORDS";

    /**
     * 写入权限
     */
    public static final String PERMISSION_WRITE = 
            "com.u2tzjtne.telephonehelper.permission.WRITE_CALL_RECORDS";

    /**
     * 通话记录表契约
     */
    public static final class CallRecord implements BaseColumns {

        /**
         * 通话记录 URI 路径
         */
        public static final String PATH_CALL_RECORDS = "callrecords";

        /**
         * 通话记录内容 URI
         */
        public static final Uri CONTENT_URI = 
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_CALL_RECORDS).build();

        /**
         * MIME 类型 - 多条记录
         */
        public static final String CONTENT_TYPE = 
                "vnd.android.cursor.dir/vnd.telephonehelper.callrecord";

        /**
         * MIME 类型 - 单条记录
         */
        public static final String CONTENT_ITEM_TYPE = 
                "vnd.android.cursor.item/vnd.telephonehelper.callrecord";

        // ========== 列名定义 ==========

        /**
         * 记录ID (INTEGER)
         */
        public static final String COLUMN_ID = "id";

        /**
         * 电话号码 (TEXT)
         */
        public static final String COLUMN_PHONE_NUMBER = "phoneNumber";

        /**
         * 归属地 (TEXT)
         */
        public static final String COLUMN_ATTRIBUTION = "attribution";

        /**
         * 运营商 (TEXT)
         */
        public static final String COLUMN_OPERATOR = "operator";

        /**
         * 通话开始时间，毫秒时间戳 (INTEGER)
         */
        public static final String COLUMN_START_TIME = "startTime";

        /**
         * 接通时间，毫秒时间戳 (INTEGER)
         */
        public static final String COLUMN_CONNECTED_TIME = "connectedTime";

        /**
         * 通话结束时间，毫秒时间戳 (INTEGER)
         */
        public static final String COLUMN_END_TIME = "endTime";

        /**
         * 是否接通 (INTEGER: 0=false, 1=true)
         */
        public static final String COLUMN_IS_CONNECTED = "isConnected";

        /**
         * 响铃次数 (INTEGER)
         */
        public static final String COLUMN_CALL_NUMBER = "callNumber";

        /**
         * 通话类型 (INTEGER: 0=呼出, 1=呼入)
         */
        public static final String COLUMN_CALL_TYPE = "callType";

        /**
         * 录音文件路径 (TEXT)
         */
        public static final String COLUMN_RECORDING_PATH = "recordingPath";

        /**
         * 录音开始时间 (INTEGER)
         */
        public static final String COLUMN_RECORDING_START_TIME = "recordingStartTime";

        /**
         * 录音结束时间 (INTEGER)
         */
        public static final String COLUMN_RECORDING_END_TIME = "recordingEndTime";

        /**
         * 默认排序规则 - 按时间倒序
         */
        public static final String DEFAULT_SORT_ORDER = COLUMN_START_TIME + " DESC";

        /**
         * 构建单条记录 URI
         *
         * @param id 记录ID
         * @return URI
         */
        public static Uri buildCallRecordUri(long id) {
            return CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();
        }
    }

    /**
     * 自定义归属地表契约
     */
    public static final class CustomLocation implements BaseColumns {

        /**
         * 归属地 URI 路径
         */
        public static final String PATH_LOCATIONS = "locations";

        /**
         * 归属地内容 URI
         */
        public static final Uri CONTENT_URI = 
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_LOCATIONS).build();

        /**
         * MIME 类型 - 多条记录
         */
        public static final String CONTENT_TYPE = 
                "vnd.android.cursor.dir/vnd.telephonehelper.location";

        /**
         * MIME 类型 - 单条记录
         */
        public static final String CONTENT_ITEM_TYPE = 
                "vnd.android.cursor.item/vnd.telephonehelper.location";

        // ========== 列名定义 ==========

        /**
         * 记录ID (INTEGER)
         */
        public static final String COLUMN_ID = "id";

        /**
         * 电话号码/号段 (TEXT)
         */
        public static final String COLUMN_PHONE = "phone";

        /**
         * 省份 (TEXT)
         */
        public static final String COLUMN_PROVINCE = "province";

        /**
         * 城市 (TEXT)
         */
        public static final String COLUMN_CITY = "city";

        /**
         * 运营商 (TEXT)
         */
        public static final String COLUMN_CARRIER = "carrier";

        /**
         * 创建时间 (INTEGER)
         */
        public static final String COLUMN_CREATE_TIME = "create_time";

        /**
         * 默认排序规则
         */
        public static final String DEFAULT_SORT_ORDER = COLUMN_CREATE_TIME + " DESC";

        /**
         * 构建单条记录 URI
         *
         * @param id 记录ID
         * @return URI
         */
        public static Uri buildLocationUri(long id) {
            return CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();
        }
    }
}
