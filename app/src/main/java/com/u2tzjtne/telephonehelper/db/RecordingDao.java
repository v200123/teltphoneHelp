package com.u2tzjtne.telephonehelper.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Maybe;

import static androidx.room.OnConflictStrategy.IGNORE;

/**
 * 录音记录数据访问对象
 *
 * @author u2tzjtne@gmail.com
 */
@Dao
public interface RecordingDao {
    /**
     * 插入录音记录
     */
    @Insert(onConflict = IGNORE)
    Completable insert(Recording recording);

    /**
     * 插入录音记录并返回ID
     */
    @Insert(onConflict = IGNORE)
    long insertAndGetId(Recording recording);

    /**
     * 更新录音记录
     */
    @androidx.room.Update
    Completable update(Recording recording);

    /**
     * 删除录音记录
     */
    @Delete
    Completable delete(Recording recording);

    /**
     * 根据ID删除录音记录
     */
    @Query("DELETE FROM Recording WHERE id = :id")
    Completable deleteById(int id);

    /**
     * 根据ID获取录音记录
     */
    @Query("SELECT * FROM Recording WHERE id = :id")
    Maybe<Recording> getById(int id);

    /**
     * 根据通话记录ID获取所有录音
     */
    @Query("SELECT * FROM Recording WHERE callRecordId = :callRecordId ORDER BY startTime ASC")
    Maybe<List<Recording>> getByCallRecordId(int callRecordId);

    /**
     * 根据通话记录ID获取所有录音（同步方法）
     */
    @Query("SELECT * FROM Recording WHERE callRecordId = :callRecordId ORDER BY startTime ASC")
    List<Recording> getByCallRecordIdSync(int callRecordId);

    /**
     * 获取所有录音记录
     */
    @Query("SELECT * FROM Recording ORDER BY startTime DESC")
    Maybe<List<Recording>> getAll();

    /**
     * 获取所有录音记录（带通话信息）
     */
    @Query("SELECT r.* FROM Recording r INNER JOIN CallRecord c ON r.callRecordId = c.id ORDER BY r.startTime DESC")
    Maybe<List<Recording>> getAllWithCallInfo();

    /**
     * 根据通话记录ID删除所有录音
     */
    @Query("DELETE FROM Recording WHERE callRecordId = :callRecordId")
    Completable deleteByCallRecordId(int callRecordId);

    /**
     * 获取通话记录的录音数量
     */
    @Query("SELECT COUNT(*) FROM Recording WHERE callRecordId = :callRecordId")
    int getCountByCallRecordId(int callRecordId);

    /**
     * 获取通话记录的总录音时长（毫秒）
     */
    @Query("SELECT SUM(duration) FROM Recording WHERE callRecordId = :callRecordId")
    long getTotalDurationByCallRecordId(int callRecordId);

    /**
     * 删除所有录音记录
     */
    @Query("DELETE FROM Recording")
    void deleteAll();
}
