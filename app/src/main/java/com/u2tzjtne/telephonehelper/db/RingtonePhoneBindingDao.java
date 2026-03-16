package com.u2tzjtne.telephonehelper.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

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
    long insert(RingtonePhoneBinding binding);

    /**
     * 批量绑定号码到彩铃
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<RingtonePhoneBinding> bindings);

    /**
     * 更新绑定关系
     */
    @Update
    void update(RingtonePhoneBinding binding);

    /**
     * 删除绑定关系
     */
    @Delete
    void delete(RingtonePhoneBinding binding);

    /**
     * 根据号码删除绑定
     */
    @Query("DELETE FROM RingtonePhoneBinding WHERE phoneNumber = :phoneNumber")
    void deleteByPhoneNumber(String phoneNumber);

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
    @Query("SELECT * FROM RingtonePhoneBinding WHERE phoneNumber = :phoneNumber LIMIT 1")
    RingtonePhoneBinding getByPhoneNumber(String phoneNumber);

    /**
     * 根据彩铃ID查询所有绑定的号码
     */
    @Query("SELECT * FROM RingtonePhoneBinding WHERE ringtoneId = :ringtoneId")
    List<RingtonePhoneBinding> getByRingtoneId(int ringtoneId);

    /**
     * 检查号码是否已绑定彩铃
     */
    @Query("SELECT COUNT(*) FROM RingtonePhoneBinding WHERE phoneNumber = :phoneNumber")
    int isPhoneBound(String phoneNumber);

    /**
     * 获取号码绑定的彩铃ID
     * @return 彩铃ID，没有绑定返回 null
     */
    @Query("SELECT ringtoneId FROM RingtonePhoneBinding WHERE phoneNumber = :phoneNumber LIMIT 1")
    Integer getRingtoneIdByPhone(String phoneNumber);

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
        deleteByPhoneNumber(phoneNumber);
        insert(new RingtonePhoneBinding(phoneNumber, newRingtoneId));
    }
}