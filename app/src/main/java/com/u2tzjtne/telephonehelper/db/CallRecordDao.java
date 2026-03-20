package com.u2tzjtne.telephonehelper.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.u2tzjtne.telephonehelper.util.PhoneNumberUtils;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Maybe;

import static androidx.room.OnConflictStrategy.IGNORE;

/**
 * @author u2tzjtne@gmail.com
 */
@Dao
public interface CallRecordDao {
    /**
     * 分页加载 默认30条
     *
     * @param pos 末尾下标
     */
    @Query("select * from CallRecord order by id desc limit :pos,:pos+30")
    List<CallRecord> getAllForPage(int pos);

    /**
     * 获取全部数据
     */
    @Query("select * from CallRecord order by id desc")
    Maybe<List<CallRecord>> getAll();

    /**
     * 按组获取全部数据
     */
    @Query("select * from CallRecord where endTime in(select max(endTime) from CallRecord group by phoneNumber) order by startTime desc")
    Maybe<List<CallRecord>> getAllByGroup();

    /**
     * 根据ID获取
     */
    @Query("select * from CallRecord where id = :id")
    CallRecord getById(int id);

    /**
     * 根据号码查询
     */
    @Query("select * from CallRecord where REPLACE(phoneNumber, ' ', '') = :number order by startTime desc limit 1")
    Maybe<CallRecord> getByNumberInternal(String number);

    default Maybe<CallRecord> getByNumber(String number) {
        return getByNumberInternal(PhoneNumberUtils.normalizePhoneNumber(number));
    }

    @Query("select * from CallRecord where REPLACE(phoneNumber, ' ', '') = :number order by startTime desc")
    Maybe<List<CallRecord>> getByNumberMutiInternal(String number);

    default Maybe<List<CallRecord>> getByNumberMuti(String number) {
        return getByNumberMutiInternal(PhoneNumberUtils.normalizePhoneNumber(number));
    }

    /**
     * 插入数据
     */
    @Insert(onConflict = IGNORE)
    Completable insertInternal(CallRecord record);

    default Completable insert(CallRecord record) {
        if (record != null) {
            record.phoneNumber = PhoneNumberUtils.normalizePhoneNumber(record.phoneNumber);
        }
        return insertInternal(record);
    }

    /**
     * 更新数据
     */
    @Update
    Completable updateInternal(CallRecord record);

    default Completable update(CallRecord record) {
        if (record != null) {
            record.phoneNumber = PhoneNumberUtils.normalizePhoneNumber(record.phoneNumber);
        }
        return updateInternal(record);
    }

    /**
     * 更新录音路径
     * @param id 通话记录ID
     * @param recordingPath 录音文件路径
     * @param recordingStartTime 录音开始时间
     * @param recordingEndTime 录音结束时间
     */
    @Query("UPDATE CallRecord SET recordingPath = :recordingPath, recordingStartTime = :recordingStartTime, recordingEndTime = :recordingEndTime WHERE id = :id")
    Completable updateRecordingPath(int id, String recordingPath, long recordingStartTime, long recordingEndTime);

    /**
     * 删除指定数据
     */
    @Delete
    Completable delete(CallRecord record);

    /**
     * 根据Id删除数据
     */
    @Query("delete from CallRecord where id like :id")
    void deleteById(int id);

    /**
     * 根据number删除数据
     */
    @Query("delete from CallRecord where REPLACE(phoneNumber, ' ', '') = :number")
    Completable deleteByNumberInternal(String number);

    default Completable deleteByNumber(String number) {
        return deleteByNumberInternal(PhoneNumberUtils.normalizePhoneNumber(number));
    }

    /**
     * 删除所有数据
     */
    @Query("delete from CallRecord")
    void deleteAll();

    /**
     * 根据号码前缀筛选（用于拨号时实时匹配）
     * 注意：数据库中号码格式为 "010 2233 0122"，需要去掉空格后再匹配
     * @param prefix 号码前缀（纯数字）
     */
    @Query("select * from CallRecord where REPLACE(phoneNumber, ' ', '') like :prefix || '%' order by startTime desc")
    Maybe<List<CallRecord>> getByPrefixInternal(String prefix);

    default Maybe<List<CallRecord>> getByPrefix(String prefix) {
        return getByPrefixInternal(PhoneNumberUtils.normalizePhoneNumber(prefix));
    }

    /**
     * 根据号码前缀筛选并按号码分组（获取每个号码的最新记录）
     * 注意：数据库中号码格式为 "010 2233 0122"，需要去掉空格后再匹配
     * @param prefix 号码前缀（纯数字）
     */
    @Query("select * from CallRecord where REPLACE(phoneNumber, ' ', '') like :prefix || '%' and endTime in(select max(endTime) from CallRecord where REPLACE(phoneNumber, ' ', '') like :prefix || '%' group by phoneNumber) order by startTime desc")
    Maybe<List<CallRecord>> getByPrefixGroupInternal(String prefix);

    default Maybe<List<CallRecord>> getByPrefixGroup(String prefix) {
        return getByPrefixGroupInternal(PhoneNumberUtils.normalizePhoneNumber(prefix));
    }

    /**
     * 获取有录音的通话记录
     */
    @Query("select * from CallRecord where recordingPath IS NOT NULL AND recordingPath != '' order by startTime desc")
    Maybe<List<CallRecord>> getWithRecording();
}
