package com.u2tzjtne.telephonehelper.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.u2tzjtne.telephonehelper.util.PhoneNumberUtils;

import java.util.List;


/**
 * 彩铃与号码绑定关系数据访问对象
 */
@Dao
public interface RingtonePhoneBindingDao {

    /**
     * 绑定号码到彩铃
     * @param binding 绑定关系
     * @return 插入的ID，如果冲突返回 -1
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertInternal(RingtonePhoneBinding binding);

    default long insert(RingtonePhoneBinding binding) {
        if (binding != null) {
            binding.phoneNumber = PhoneNumberUtils.normalizePhoneNumber(binding.phoneNumber);
        }
        return insertInternal(binding);
    }

    /**
     * 批量绑定号码到彩铃
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllInternal(List<RingtonePhoneBinding> bindings);

    default void insertAll(List<RingtonePhoneBinding> bindings) {
        if (bindings != null) {
            for (RingtonePhoneBinding binding : bindings) {
                if (binding != null) {
                    binding.phoneNumber = PhoneNumberUtils.normalizePhoneNumber(binding.phoneNumber);
                }
            }
        }
        insertAllInternal(bindings);
    }

    /**
     * 更新绑定关系
     */
    @Update
    void updateInternal(RingtonePhoneBinding binding);

    default void update(RingtonePhoneBinding binding) {
        if (binding != null) {
            binding.phoneNumber = PhoneNumberUtils.normalizePhoneNumber(binding.phoneNumber);
        }
        updateInternal(binding);
    }


    /**
     * 删除绑定关系
     */
    @Delete
    void delete(RingtonePhoneBinding binding);

    /**
     * 根据号码删除绑定
     */
    @Query("DELETE FROM RingtonePhoneBinding WHERE REPLACE(phoneNumber, ' ', '') = :phoneNumber")
    void deleteByPhoneNumberInternal(String phoneNumber);

    default void deleteByPhoneNumber(String phoneNumber) {
        deleteByPhoneNumberInternal(PhoneNumberUtils.normalizePhoneNumber(phoneNumber));
    }


    /**
     * 根据彩铃ID删除所有绑定
     */
    @Query("DELETE FROM RingtonePhoneBinding WHERE ringtoneId = :ringtoneId")
    void deleteByRingtoneId(int ringtoneId);

    /**
     * 获取所有绑定关系
     */
    @Query("SELECT * FROM RingtonePhoneBinding ORDER BY bindTime DESC")
    List<RingtonePhoneBinding> getAll();

    /**
     * 根据号码查询绑定关系
     * @param phoneNumber 手机号码
     * @return 绑定关系，没有则返回 null
     */
    @Query("SELECT * FROM RingtonePhoneBinding WHERE REPLACE(phoneNumber, ' ', '') = :phoneNumber LIMIT 1")
    RingtonePhoneBinding getByPhoneNumberInternal(String phoneNumber);

    default RingtonePhoneBinding getByPhoneNumber(String phoneNumber) {
        return getByPhoneNumberInternal(PhoneNumberUtils.normalizePhoneNumber(phoneNumber));
    }


    /**
     * 根据彩铃ID查询所有绑定的号码
     */
    @Query("SELECT * FROM RingtonePhoneBinding WHERE ringtoneId = :ringtoneId")
    List<RingtonePhoneBinding> getByRingtoneId(int ringtoneId);

    /**
     * 检查号码是否已绑定彩铃
     */
    @Query("SELECT COUNT(*) FROM RingtonePhoneBinding WHERE REPLACE(phoneNumber, ' ', '') = :phoneNumber")
    int isPhoneBoundInternal(String phoneNumber);

    default int isPhoneBound(String phoneNumber) {
        return isPhoneBoundInternal(PhoneNumberUtils.normalizePhoneNumber(phoneNumber));
    }


    /**
     * 获取号码绑定的彩铃ID
     * @return 彩铃ID，没有绑定返回 null
     */
    @Query("SELECT ringtoneId FROM RingtonePhoneBinding WHERE REPLACE(phoneNumber, ' ', '') = :phoneNumber LIMIT 1")
    Integer getRingtoneIdByPhoneInternal(String phoneNumber);

    default Integer getRingtoneIdByPhone(String phoneNumber) {
        return getRingtoneIdByPhoneInternal(PhoneNumberUtils.normalizePhoneNumber(phoneNumber));
    }


    /**
     * 获取绑定到指定彩铃的号码数量
     */
    @Query("SELECT COUNT(*) FROM RingtonePhoneBinding WHERE ringtoneId = :ringtoneId")
    int getBoundPhoneCount(int ringtoneId);

    /**
     * 清空所有绑定
     */
    @Query("DELETE FROM RingtonePhoneBinding")
    void clearAll();

    /**
     * 获取绑定数量
     */
    @Query("SELECT COUNT(*) FROM RingtonePhoneBinding")
    int getCount();

    /**
     * 事务：先解绑号码原有的彩铃，再绑定新彩铃
     */
    @Transaction
    default void rebindPhoneNumber(String phoneNumber, int newRingtoneId) {
        String normalizedPhoneNumber = PhoneNumberUtils.normalizePhoneNumber(phoneNumber);
        deleteByPhoneNumberInternal(normalizedPhoneNumber);
        insertInternal(new RingtonePhoneBinding(normalizedPhoneNumber, newRingtoneId));
    }

}