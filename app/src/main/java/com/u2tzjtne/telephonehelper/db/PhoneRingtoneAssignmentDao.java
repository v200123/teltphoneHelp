package com.u2tzjtne.telephonehelper.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.u2tzjtne.telephonehelper.util.PhoneNumberUtils;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;

/**
 * 号码与彩铃分配关系数据访问对象
 */
@Dao
public interface PhoneRingtoneAssignmentDao {

    /**
     * 插入分配关系
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertInternal(PhoneRingtoneAssignment assignment);

    default long insert(PhoneRingtoneAssignment assignment) {
        if (assignment != null) {
            assignment.phoneNumber = PhoneNumberUtils.normalizePhoneNumber(assignment.phoneNumber);
        }
        return insertInternal(assignment);
    }

    /**
     * 批量插入
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllInternal(List<PhoneRingtoneAssignment> assignments);

    default void insertAll(List<PhoneRingtoneAssignment> assignments) {
        if (assignments != null) {
            for (PhoneRingtoneAssignment assignment : assignments) {
                if (assignment != null) {
                    assignment.phoneNumber = PhoneNumberUtils.normalizePhoneNumber(assignment.phoneNumber);
                }
            }
        }
        insertAllInternal(assignments);
    }

    /**
     * 删除分配关系
     */
    @Delete
    Completable delete(PhoneRingtoneAssignment assignment);

    /**
     * 根据号码删除
     */
    @Query("DELETE FROM PhoneRingtoneAssignment WHERE REPLACE(phoneNumber, ' ', '') = :phoneNumber")
    void deleteByPhoneNumberInternal(String phoneNumber);

    default void deleteByPhoneNumber(String phoneNumber) {
        deleteByPhoneNumberInternal(PhoneNumberUtils.normalizePhoneNumber(phoneNumber));
    }

    /**
     * 根据彩铃ID删除所有分配
     */
    @Query("DELETE FROM PhoneRingtoneAssignment WHERE ringtoneId = :ringtoneId")
    void deleteByRingtoneId(int ringtoneId);

    /**
     * 查询所有分配关系
     */
    @Query("SELECT * FROM PhoneRingtoneAssignment ORDER BY assignTime DESC")
    Single<List<PhoneRingtoneAssignment>> getAll();

    /**
     * 根据号码查询分配的彩铃ID
     */
    @Query("SELECT ringtoneId FROM PhoneRingtoneAssignment WHERE REPLACE(phoneNumber, ' ', '') = :phoneNumber LIMIT 1")
    Integer getRingtoneIdByPhoneInternal(String phoneNumber);

    default Integer getRingtoneIdByPhone(String phoneNumber) {
        return getRingtoneIdByPhoneInternal(PhoneNumberUtils.normalizePhoneNumber(phoneNumber));
    }

    /**
     * 根据号码查询完整记录
     */
    @Query("SELECT * FROM PhoneRingtoneAssignment WHERE REPLACE(phoneNumber, ' ', '') = :phoneNumber LIMIT 1")
    PhoneRingtoneAssignment getByPhoneNumberInternal(String phoneNumber);

    default PhoneRingtoneAssignment getByPhoneNumber(String phoneNumber) {
        return getByPhoneNumberInternal(PhoneNumberUtils.normalizePhoneNumber(phoneNumber));
    }

    /**
     * 检查号码是否已分配彩铃
     */
    @Query("SELECT COUNT(*) FROM PhoneRingtoneAssignment WHERE REPLACE(phoneNumber, ' ', '') = :phoneNumber")
    int isPhoneAssignedInternal(String phoneNumber);

    default boolean isPhoneAssigned(String phoneNumber) {
        return isPhoneAssignedInternal(PhoneNumberUtils.normalizePhoneNumber(phoneNumber)) > 0;
    }

    /**
     * 根据彩铃ID查询所有分配的号码
     */
    @Query("SELECT * FROM PhoneRingtoneAssignment WHERE ringtoneId = :ringtoneId")
    List<PhoneRingtoneAssignment> getByRingtoneId(int ringtoneId);

    /**
     * 获取分配给指定彩铃的号码数量
     */
    @Query("SELECT COUNT(*) FROM PhoneRingtoneAssignment WHERE ringtoneId = :ringtoneId")
    int getAssignedPhoneCount(int ringtoneId);

    /**
     * 清空所有分配关系
     */
    @Query("DELETE FROM PhoneRingtoneAssignment")
    Completable deleteAll();

    /**
     * 获取分配关系总数
     */
    @Query("SELECT COUNT(*) FROM PhoneRingtoneAssignment")
    int getCount();
}
